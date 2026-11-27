package com.xunye;
import com.fasterxml.jackson.databind.*;
import com.xunye.constant.*;
import com.xunye.dao.*;
import com.xunye.dto.req.*;
import com.xunye.entity.*;
import com.xunye.mq.*;
import com.xunye.service.*;
import com.xunye.service.impl.*;
import com.xunye.util.JwtUtils;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** VIP支付用真实事务/行锁；MQ仅替换传输层，投递重试和消费去重仍执行生产代码。 */
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Sql({"/m2-schema.sql","/m3-schema.sql"})
class M4IntegrationTest {
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired JdbcTemplate db; @Autowired JwtUtils jwt;
    @Autowired WalletService wallet; @Autowired VipService vip; @Autowired UserMapper users;
    @Autowired BusinessEventService events; @Autowired OutboxPublisher publisher; @Autowired TransactionTemplate tx;
    @MockBean StringRedisTemplate redis; @MockBean CaptchaService captcha; @MockBean MessageTransport transport;
    String token,other;
    @BeforeEach void setup() {
        var values=mock(ValueOperations.class);when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(),anyString(),any(Duration.class))).thenReturn(true);when(redis.hasKey(anyString())).thenReturn(false);
        ReflectionTestUtils.setField(publisher,"enabled",false);
        db.update("insert into `user`(id,username,phone,password,nickname,balance,is_author) values(1,'author','13900000001','test','作者',0,1),(2,'reader','13900000002','test','读者',10000,0),(3,'other','13900000003','test','其他',0,0)");
        db.update("insert into vip_plan(id,name,duration_days,price,discount,privileges,status) values(1,'月卡',30,1500,90,'会员书库;九折',0),(2,'季卡',90,3900,85,'会员书库;八五折',0),(3,'下架',30,1000,90,'',1)");
        db.update("update book_chapter set is_free=0,price=101 where id=1");
        token=jwt.createUserToken(2L);other=jwt.createUserToken(3L);
    }
    JsonNode call(String method,String path,String auth,Object body)throws Exception {
        var req=request(org.springframework.http.HttpMethod.valueOf(method),path);
        if(auth!=null)req.header("Authorization",auth);
        if(body!=null)req.contentType("application/json").content(json.writeValueAsString(body));
        return json.readTree(mvc.perform(req).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }
    JsonNode ok(String method,String path,String auth,Object body)throws Exception {JsonNode r=call(method,path,auth,body);assertThat(r.path("code").asInt()).as(r.toString()).isEqualTo(200);return r.path("data");}
    Map<String,Object> req(String id,long price){return Map.of("requestId",id,"expectedPrice",price);}
    long balance(){return users.selectById(2L).getBalance();}
    int count(String table){return db.queryForObject("select count(*) from "+table,Integer.class);}
    String subscribe()throws Exception{return ok("POST","/api/app/vip/subscribe/1",token,req("vip-request-00001",1500)).path("id").asText();}

    @Test void subscribeIsAtomicAndIdempotentAndExpiryStartsNow()throws Exception {
        LocalDateTime before=LocalDateTime.now().minusSeconds(1);subscribe();
        User user=users.selectById(2L);assertThat(vip.isVip(user)).isTrue();assertThat(balance()).isEqualTo(8500);
        assertThat(user.getVipExpireTime()).isAfter(before.plusDays(30)).isBefore(LocalDateTime.now().plusDays(30).plusSeconds(1));
        assertThat(count("vip_subscription")).isEqualTo(1);assertThat(count("vip_subscription_detail")).isEqualTo(1);assertThat(count("balance_flow")).isEqualTo(1);assertThat(count("mq_outbox")).isEqualTo(1);
        assertThat(ok("POST","/api/app/vip/subscribe/1",token,req("vip-request-00001",1500)).path("alreadyProcessed").asBoolean()).isTrue();
        assertThat(balance()).isEqualTo(8500);assertThat(count("mq_outbox")).isEqualTo(1);
        assertThat(ok("GET","/api/app/vip/status",token,null).path("discount").asInt()).isZero();
    }
    @Test void renewalPreservesTimeAndSnapshotsDiscountForEachPeriod()throws Exception {
        subscribe();LocalDateTime first=users.selectById(2L).getVipExpireTime();
        ok("POST","/api/app/vip/subscribe/2",token,req("vip-request-00002",3900));
        assertThat(users.selectById(2L).getVipExpireTime()).isEqualTo(first.plusDays(90));
        assertThat(vip.discount(users.selectById(2L))).isEqualTo(90);
        db.update("update vip_plan set discount=50,price=2000,name='改价套餐' where id=1");
        assertThat(vip.discount(users.selectById(2L))).isEqualTo(90);
        JsonNode rows=ok("GET","/api/app/vip/subscriptions",token,null).path("records");
        assertThat(rows.get(1).path("planName").asText()).isEqualTo("月卡");
        // 把第一段模拟为过去、第二段模拟为当前，验证续费套餐折扣从对应区间生效。
        db.update("update vip_subscription set start_time=DATEADD('DAY',-40,CURRENT_TIMESTAMP),expire_time=DATEADD('DAY',-1,CURRENT_TIMESTAMP) where plan_id=1");
        db.update("update vip_subscription set start_time=DATEADD('DAY',-1,CURRENT_TIMESTAMP) where plan_id=2");
        assertThat(vip.discount(users.selectById(2L))).isEqualTo(85);
    }
    @Test void expiredVipRestartsNowAndPermissionsExpireImmediately()throws Exception {
        db.update("update `user` set vip_expire_time=DATEADD('DAY',-5,CURRENT_TIMESTAMP) where id=2");subscribe();
        assertThat(users.selectById(2L).getVipExpireTime()).isAfter(LocalDateTime.now().plusDays(29));
        db.update("update book set is_vip_library=1 where id=1");
        assertThat(ok("GET","/api/app/chapter/content/1",token,null).path("locked").asBoolean()).isFalse();
        assertThat(ok("GET","/api/app/chapter/content/1",other,null).path("content").isNull()).isTrue();
        db.update("update `user` set vip_expire_time=DATEADD('SECOND',-1,CURRENT_TIMESTAMP) where id=2");
        assertThat(ok("GET","/api/app/chapter/content/1",token,null).path("locked").asBoolean()).isTrue();
        assertThat(ok("GET","/api/app/vip/status",token,null).path("active").asBoolean()).isFalse();
    }
    @Test void ordinaryPaidChaptersAreFreeForVipWithoutPurchaseOrLedger()throws Exception {
        subscribe();JsonNode quote=ok("GET","/api/app/chapter/content/1",token,null);
        assertThat(quote.path("locked").asBoolean()).isFalse();
        assertThat(quote.path("price").asLong()).isZero();
        assertThat(quote.path("originalPrice").asLong()).isEqualTo(101);
        assertThat(quote.path("canPurchase").asBoolean()).isFalse();
        JsonNode result=ok("POST","/api/app/chapter/buy/1",token,Map.of("expectedPrice",101));
        assertThat(result.path("amount").asLong()).isZero();
        assertThat(balance()).isEqualTo(8500);assertThat(users.selectById(1L).getBalance()).isZero();
        assertThat(count("chapter_purchase")).isZero();assertThat(count("balance_flow")).isEqualTo(1);
        assertThat(vip.chapterPrice(101,users.selectById(2L))).isZero();
        // VIP畅读不等于买断，会员过期后普通付费章重新要求购买。
        db.update("update `user` set vip_expire_time=DATEADD('SECOND',-1,CURRENT_TIMESTAMP) where id=2");
        JsonNode expired=ok("GET","/api/app/chapter/content/1",token,null);
        assertThat(expired.path("locked").asBoolean()).isTrue();
        assertThat(expired.path("canPurchase").asBoolean()).isTrue();
        assertThat(expired.path("price").asLong()).isEqualTo(101);
    }
    @Test void exclusiveBooksRequireVipEvenForFreeChapterOrLegacyPurchase()throws Exception {
        db.update("update book set is_vip_library=1 where id=1");
        db.update("update book_chapter set is_free=1,price=0 where id=1");
        db.update("insert into chapter_purchase(user_id,book_id,chapter_id,amount,author_income) values(3,1,1,101,50)");
        for(String auth:new String[]{null,other,jwt.createUserToken(1L)}) {
            JsonNode locked=ok("GET","/api/app/chapter/content/1",auth,null);
            assertThat(locked.path("locked").asBoolean()).isTrue();
            assertThat(locked.path("content").isNull()).isTrue();
            assertThat(locked.path("vipRequired").asBoolean()).isTrue();
            assertThat(locked.path("canPurchase").asBoolean()).isFalse();
            assertThat(locked.path("price").asLong()).isZero();
        }
        assertThat(call("POST","/api/app/chapter/buy/1",other,Map.of("expectedPrice",0)).path("code").asInt()).isEqualTo(5003);
        assertThat(ok("GET","/api/app/book/chapters/1",null,null).get(0).path("vipLibrary").asBoolean()).isTrue();
        subscribe();
        assertThat(ok("GET","/api/app/chapter/content/1",token,null).path("locked").asBoolean()).isFalse();
        db.update("update book_chapter set is_free=0,price=101 where id=1");
        assertThat(ok("GET","/api/app/chapter/content/1",token,null).path("locked").asBoolean()).isFalse();
        ok("POST","/api/app/chapter/buy/1",token,Map.of("expectedPrice",101));
        assertThat(balance()).isEqualTo(8500);assertThat(count("chapter_purchase")).isEqualTo(1);
    }
    @Test void ordinaryBooksKeepFreeAccessAndPermanentNonMemberPurchases()throws Exception {
        assertThat(ok("GET","/api/app/chapter/content/2",null,null).path("locked").asBoolean()).isFalse();
        db.update("update `user` set balance=500 where id=3");
        JsonNode locked=ok("GET","/api/app/chapter/content/1",other,null);
        assertThat(locked.path("vipRequired").asBoolean()).isFalse();
        assertThat(locked.path("canPurchase").asBoolean()).isTrue();
        ok("POST","/api/app/chapter/buy/1",other,Map.of("expectedPrice",101));
        assertThat(users.selectById(3L).getBalance()).isEqualTo(399);
        assertThat(ok("GET","/api/app/chapter/content/1",other,null).path("locked").asBoolean()).isFalse();
        assertThat(ok("GET","/api/app/chapter/content/1",null,null).path("locked").asBoolean()).isTrue();
    }
    @Test void currentPlanBenefitsNoLongerAdvertiseChapterDiscounts()throws Exception {
        JsonNode plans=ok("GET","/api/app/vip/plans",null,null);
        assertThat(plans.get(0).path("privileges").asText()).contains("普通作品全部章节畅读","会员专属").doesNotContain("折扣");
    }
    @Test void priceChangeAndDownPlanAndNoBalanceAndRequestReuseAreRejected()throws Exception {
        assertThat(call("POST","/api/app/vip/subscribe/3",token,req("vip-request-00003",1000)).path("code").asInt()).isEqualTo(400);
        assertThat(call("POST","/api/app/vip/subscribe/1",token,req("vip-request-00003",1)).path("code").asInt()).isEqualTo(409);
        assertThat(call("POST","/api/app/vip/subscribe/1",other,req("vip-request-00003",1500)).path("code").asInt()).isEqualTo(5001);
        subscribe();
        assertThat(call("POST","/api/app/vip/subscribe/2",token,req("vip-request-00001",3900)).path("code").asInt()).isEqualTo(409);
        assertThat(balance()).isEqualTo(8500);
    }
    @Test void snapshotFailureRollsBackBalanceLedgerVipAndOutbox() {
        db.execute("alter table vip_subscription_detail add constraint reject_snapshot check(discount<>90)");
        assertThatThrownBy(()->wallet.subscribe(2L,1L,new VipSubscribeReqDto("vip-rollback-0001",1500L))).isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThat(balance()).isEqualTo(10000);assertThat(users.selectById(2L).getVipExpireTime()).isNull();
        assertThat(count("vip_subscription")).isZero();assertThat(count("balance_flow")).isZero();assertThat(count("mq_outbox")).isZero();
    }
    @Test void concurrentSameSubscriptionOnlyChargesOnce()throws Exception {
        try(var pool=Executors.newFixedThreadPool(2)) {
            var tasks=List.<Callable<Object>>of(()->wallet.subscribe(2L,1L,new VipSubscribeReqDto("vip-concurrent-001",1500L)),()->wallet.subscribe(2L,1L,new VipSubscribeReqDto("vip-concurrent-001",1500L)));
            for(var result:pool.invokeAll(tasks))result.get(10,TimeUnit.SECONDS);
        }
        assertThat(balance()).isEqualTo(8500);assertThat(count("vip_subscription")).isEqualTo(1);assertThat(count("mq_outbox")).isEqualTo(1);
    }
    @Test void mqFailureKeepsSubscriptionAndMessageCanRetryThenConsumeOnce()throws Exception {
        subscribe();ReflectionTestUtils.setField(publisher,"enabled",true);
        doThrow(new RuntimeException("broker offline")).when(transport).send(any());publisher.publish();
        assertThat(balance()).isEqualTo(8500);assertThat(db.queryForObject("select status from mq_outbox",Integer.class)).isZero();
        assertThat(db.queryForObject("select attempts from mq_outbox",Integer.class)).isEqualTo(1);
        db.update("update mq_outbox set next_attempt_at=DATEADD('SECOND',-1,CURRENT_TIMESTAMP)");
        doNothing().when(transport).send(any());publisher.publish();
        assertThat(db.queryForObject("select status from mq_outbox",Integer.class)).isEqualTo(MqConstant.SENT);
        String id=db.queryForObject("select id from mq_outbox",String.class);events.consume(id);events.consume(id);
        assertThat(count("user_notification")).isEqualTo(1);assertThat(db.queryForObject("select status from mq_outbox",Integer.class)).isEqualTo(MqConstant.CONSUMED);
        ReflectionTestUtils.setField(publisher,"enabled",false);
    }
    @Test void failedNotificationRollsBackDedupeSoRetryStillCreatesIt()throws Exception {
        subscribe();String id=db.queryForObject("select id from mq_outbox",String.class);
        db.execute("alter table user_notification add constraint reject_notice check(title<>'VIP订阅成功')");
        assertThatThrownBy(()->events.consume(id)).isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThat(db.queryForObject("select consumed_at from mq_outbox",LocalDateTime.class)).isNull();
        db.execute("alter table user_notification drop constraint reject_notice");events.consume(id);assertThat(count("user_notification")).isEqualTo(1);
    }
    @Test void timeoutDoesNotClosePaidOrUnexpiredOrderAndClosesExpiredWithoutUserQuery() {
        db.update("insert into recharge_order(id,order_no,user_id,plan_id,request_id,amount,bonus_amount,status,expires_at) values(1,'timeout1',2,1,'timeout-request1',1000,0,0,DATEADD('MINUTE',10,CURRENT_TIMESTAMP)),(2,'timeout2',2,1,'timeout-request2',1000,0,1,DATEADD('MINUTE',-1,CURRENT_TIMESTAMP)),(3,'timeout3',2,1,'timeout-request3',1000,0,0,DATEADD('MINUTE',-1,CURRENT_TIMESTAMP))");
        for(long ref=1;ref<=3;ref++){long order=ref;tx.executeWithoutResult(s->events.enqueue(MqConstant.ORDER_TIMEOUT,2L,order,LocalDateTime.now(),"test-timeout:"+order));}
        for(String id:db.queryForList("select id from mq_outbox",String.class))events.consume(id);
        assertThat(db.queryForObject("select status from recharge_order where id=1",Integer.class)).isZero();
        assertThat(db.queryForObject("select status from recharge_order where id=2",Integer.class)).isEqualTo(1);
        assertThat(db.queryForObject("select status from recharge_order where id=3",Integer.class)).isEqualTo(2);
        assertThat(count("balance_flow")).isZero();
    }
    @Test void notificationPrivacyReadIdempotencyAndGuestVipLibrary()throws Exception {
        subscribe();events.consume(db.queryForObject("select id from mq_outbox",String.class));
        long id=ok("GET","/api/app/notifications",token,null).path("records").get(0).path("id").asLong();
        assertThat(ok("GET","/api/app/notifications",other,null).path("total").asInt()).isZero();
        assertThat(call("POST","/api/app/notifications/"+id+"/read",other,null).path("code").asInt()).isEqualTo(403);
        ok("POST","/api/app/notifications/"+id+"/read",token,null);ok("POST","/api/app/notifications/"+id+"/read",token,null);
        assertThat(ok("GET","/api/app/notifications/unread-count",token,null).asLong()).isZero();
        mvc.perform(get("/api/app/vip/status")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/app/notifications")).andExpect(status().isUnauthorized());
        db.update("update book set is_vip_library=1 where id in(1,2)");db.update("update book set audit_status=0 where id=2");
        assertThat(ok("GET","/api/app/vip/library",null,null).path("total").asInt()).isEqualTo(1);
        assertThat(ok("GET","/api/app/vip/plans",null,null).size()).isEqualTo(2);
    }
    @Test void expiryReminderIsDedupedAndStaleReminderSuppressed() {
        LocalDateTime old=LocalDateTime.now().plusDays(2).withNano(0);db.update("update `user` set vip_expire_time=? where id=2",old);
        tx.executeWithoutResult(s->{events.enqueue(MqConstant.VIP_EXPIRING,2L,2L,old,"reminder:2:"+old);events.enqueue(MqConstant.VIP_EXPIRING,2L,2L,old,"reminder:2:"+old);});
        assertThat(count("mq_outbox")).isEqualTo(1);
        db.update("update `user` set vip_expire_time=DATEADD('DAY',30,CURRENT_TIMESTAMP) where id=2");
        events.consume(db.queryForObject("select id from mq_outbox",String.class));assertThat(count("user_notification")).isZero();
    }
    @Test void concurrentConsumersCommitOnlyOneNotification()throws Exception {
        subscribe();String id=db.queryForObject("select id from mq_outbox",String.class);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var tasks=List.<Callable<Object>>of(()->{events.consume(id);return null;},()->{events.consume(id);return null;});
            for(var result:pool.invokeAll(tasks))result.get(10,TimeUnit.SECONDS);
        }
        assertThat(count("user_notification")).isEqualTo(1);
        assertThat(db.queryForObject("select status from mq_outbox",Integer.class)).isEqualTo(MqConstant.CONSUMED);
    }
    @Test void oldM3OrdersAreRecoveredAcrossMultipleBatchesWithoutStarvation() {
        for(int i=1;i<=25;i++)db.update("insert into recharge_order(order_no,user_id,plan_id,request_id,amount,bonus_amount,status,expires_at) values(?,2,1,?,1000,0,0,DATEADD('MINUTE',10,CURRENT_TIMESTAMP))","recover-order-"+i,"recover-request-"+i);
        ReflectionTestUtils.setField(events,"enabled",true);
        try {
            ((BusinessEventServiceImpl)events).recoverOrders();
            ((BusinessEventServiceImpl)events).recoverOrders();
            ((BusinessEventServiceImpl)events).recoverOrders();
            assertThat(count("mq_outbox")).isEqualTo(25);
        }finally {ReflectionTestUtils.setField(events,"enabled",false);}
    }

}
