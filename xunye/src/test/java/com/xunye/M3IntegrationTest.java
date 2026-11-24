package com.xunye;
import com.fasterxml.jackson.databind.*;
import com.xunye.dto.req.*;
import com.xunye.service.WalletService;
import com.xunye.service.impl.WalletServiceImpl;
import com.xunye.exception.BizException;
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
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 资金链路使用真实事务与H2行锁；仅Redis被替换，所有余额/流水断言直查数据库。 */
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
@Sql({"/m2-schema.sql","/m3-schema.sql"})
class M3IntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate db;
    @Autowired JwtUtils jwt;
    @Autowired WalletService wallet;
    @MockBean StringRedisTemplate redis;
    @MockBean com.xunye.service.CaptchaService captcha;
    ValueOperations<String,String> values;
    String token,authorToken;
    @BeforeEach void setup() {
        values=mock(ValueOperations.class);when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(),anyString(),any(Duration.class))).thenReturn(true);
        when(redis.hasKey(anyString())).thenReturn(false);
        ReflectionTestUtils.setField(wallet,"simulatedEnabled",true);
        db.update("insert into `user`(id,username,phone,password,nickname,balance,is_author) values(1,'author','13900000001','test','作者',0,1),(2,'reader','13900000002','test','读者',0,0)");
        db.update("update book_chapter set is_free=0,price=101 where id=1");
        db.update("insert into recharge_plan(id,title,amount,bonus_amount,status) values(1,'10元档',1000,100,0),(2,'下架档',1000,0,1)");
        token=jwt.createUserToken(2L);authorToken=jwt.createUserToken(1L);
    }
    JsonNode call(String method,String path,String auth,Object body) throws Exception {
        var req=request(org.springframework.http.HttpMethod.valueOf(method),path);
        if(auth!=null)req.header("Authorization",auth);
        if(body!=null)req.contentType("application/json").content(json.writeValueAsString(body));
        return json.readTree(mvc.perform(req).andReturn().getResponse().getContentAsString());
    }
    JsonNode ok(String method,String path,String auth,Object body) throws Exception {
        JsonNode r=call(method,path,auth,body);assertThat(r.path("code").asInt()).as(r.toString()).isEqualTo(200);return r.path("data");
    }
    long balance(long id){return db.queryForObject("select balance from `user` where id=?",Long.class,id);}
    long count(String table){return db.queryForObject("select count(*) from "+table,Long.class);}
    @Test void rechargeSnapshotsAndRepeatedPaymentCreditExactlyOnce() throws Exception {
        Map<String,Object> req=Map.of("planId",1,"requestId","recharge-request-0001","amount",999999);
        long id=ok("POST","/api/app/wallet/recharge",token,req).path("id").asLong();
        assertThat(ok("POST","/api/app/wallet/recharge",token,req).path("id").asLong()).isEqualTo(id);
        db.update("update recharge_plan set amount=2000,bonus_amount=0,status=1 where id=1");
        ok("POST","/api/app/wallet/recharge/"+id+"/simulate-payment",token,null);
        ok("POST","/api/app/wallet/recharge/"+id+"/simulate-payment",token,null);
        assertThat(balance(2)).isEqualTo(1100);assertThat(count("balance_flow")).isEqualTo(1);
        assertThat(ok("GET","/api/app/wallet/recharge/"+id,token,null).path("status").asInt()).isEqualTo(1);
        assertThat(call("GET","/api/app/wallet/recharge/"+id,authorToken,null).path("code").asInt()).isEqualTo(403);
        assertThat(call("POST","/api/app/wallet/recharge",token,Map.of("planId",2,"requestId","recharge-request-0001")).path("code").asInt()).isEqualTo(409);
    }
    @Test void expiredOrdersAndDisabledSimulationNeverCredit() throws Exception {
        long id=ok("POST","/api/app/wallet/recharge",token,Map.of("planId",1,"requestId","recharge-request-0002")).path("id").asLong();
        db.update("update recharge_order set expires_at=DATEADD('MINUTE',-1,CURRENT_TIMESTAMP) where id=?",id);
        assertThat(ok("POST","/api/app/wallet/recharge/"+id+"/simulate-payment",token,null).path("status").asInt()).isEqualTo(2);
        assertThat(balance(2)).isZero();assertThat(count("balance_flow")).isZero();
        ReflectionTestUtils.setField(wallet,"simulatedEnabled",false);
        assertThat(call("POST","/api/app/wallet/recharge/"+id+"/simulate-payment",token,null).path("code").asInt()).isEqualTo(403);
    }
    @Test void purchaseUnlocksContentAndWritesBalancedLedgerOnlyOnce() throws Exception {
        db.update("update `user` set balance=500 where id=2");
        assertThat(ok("GET","/api/app/chapter/content/1",token,null).path("content").isNull()).isTrue();
        JsonNode receipt=ok("POST","/api/app/chapter/buy/1",token,Map.of("expectedPrice",101));
        assertThat(receipt.path("alreadyProcessed").asBoolean()).isFalse();
        assertThat(balance(2)).isEqualTo(399);assertThat(balance(1)).isEqualTo(50);
        assertThat(count("chapter_purchase")).isEqualTo(1);assertThat(count("balance_flow")).isEqualTo(2);
        assertThat(ok("POST","/api/app/chapter/buy/1",token,Map.of("expectedPrice",101)).path("alreadyProcessed").asBoolean()).isTrue();
        assertThat(balance(2)).isEqualTo(399);assertThat(count("balance_flow")).isEqualTo(2);
        assertThat(ok("GET","/api/app/chapter/content/1",token,null).path("locked").asBoolean()).isFalse();
        assertThat(ok("GET","/api/app/chapter/content/1",null,null).path("content").isNull()).isTrue();
        assertThat(ok("GET","/api/app/chapter/content/1",authorToken,null).path("locked").asBoolean()).isFalse();
    }
    @Test void purchaseRejectsInsufficientFundsWrongPriceAndHiddenContent() throws Exception {
        assertThat(call("POST","/api/app/chapter/buy/1",token,Map.of("expectedPrice",101)).path("code").asInt()).isEqualTo(5001);
        db.update("update `user` set balance=500 where id=2");
        assertThat(call("POST","/api/app/chapter/buy/1",token,Map.of("expectedPrice",1)).path("code").asInt()).isEqualTo(409);
        db.update("update book set status=2 where id=1");
        assertThat(call("POST","/api/app/chapter/buy/1",token,Map.of("expectedPrice",101)).path("code").asInt()).isEqualTo(5006);
        assertThat(balance(2)).isEqualTo(500);assertThat(count("balance_flow")).isZero();assertThat(count("chapter_purchase")).isZero();
    }
    @Test void tipIsIdempotentAndCannotReuseRequestForDifferentAmount() throws Exception {
        db.update("update `user` set balance=500 where id=2");
        Map<String,Object> req=Map.of("amount",125,"message","加油","requestId","tip-request-00001");
        ok("POST","/api/app/book/tip/1",token,req);ok("POST","/api/app/book/tip/1",token,req);
        assertThat(balance(2)).isEqualTo(375);assertThat(balance(1)).isEqualTo(125);
        assertThat(count("tip_record")).isEqualTo(1);assertThat(count("balance_flow")).isEqualTo(2);
        assertThat(call("POST","/api/app/book/tip/1",token,Map.of("amount",126,"requestId","tip-request-00001")).path("code").asInt()).isEqualTo(409);
        assertThat(call("POST","/api/app/book/tip/1",authorToken,req).path("code").asInt()).isEqualTo(400);
        assertThat(call("POST","/api/app/book/tip/1",token,Map.of("amount",0,"requestId","tip-request-00002")).path("code").asInt()).isEqualTo(400);
    }
    @Test void ledgerFailureRollsBackBothAccountsAndRecords() {
        db.update("update `user` set balance=500 where id=2");
        // 强制作者流水写失败，验证先前读者扣款/流水也会回滚。
        db.execute("alter table balance_flow add constraint test_reject_income check(type <> 4)");
        assertThatThrownBy(()->wallet.tip(2L,1L,new TipReqDto(100L,"测试","rollback-request-001"))).isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThat(balance(2)).isEqualTo(500);assertThat(balance(1)).isZero();
        assertThat(count("balance_flow")).isZero();assertThat(count("tip_record")).isZero();
    }
    @Test void concurrentPurchaseUsesDatabaseIdempotencyEvenWithoutRedisLock() throws Exception {
        db.update("update `user` set balance=500 where id=2");
        try(var executor=Executors.newFixedThreadPool(2)) {
            var tasks=List.<Callable<Object>>of(()->wallet.buy(2L,1L,new BuyChapterReqDto(101L)),()->wallet.buy(2L,1L,new BuyChapterReqDto(101L)));
            for(var future:executor.invokeAll(tasks))future.get(10,TimeUnit.SECONDS);
        }
        assertThat(balance(2)).isEqualTo(399);assertThat(balance(1)).isEqualTo(50);
        assertThat(count("chapter_purchase")).isEqualTo(1);assertThat(count("balance_flow")).isEqualTo(2);
    }
    @Test void reverseTipsLockAccountsInStableOrder() throws Exception {
        db.update("update `user` set balance=500");db.update("update book set author_id=2 where id=2");
        try(var executor=Executors.newFixedThreadPool(2)) {
            var tasks=List.<Callable<Object>>of(()->wallet.tip(2L,1L,new TipReqDto(100L,"A","reverse-tip-request1")),()->wallet.tip(1L,2L,new TipReqDto(50L,"B","reverse-tip-request2")));
            for(var future:executor.invokeAll(tasks))future.get(10,TimeUnit.SECONDS);
        }
        assertThat(balance(1)).isEqualTo(550);assertThat(balance(2)).isEqualTo(450);assertThat(count("balance_flow")).isEqualTo(4);
    }
    @Test void walletAuthPrivacyAndBusyLock() throws Exception {
        mvc.perform(get("/api/app/wallet")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/app/chapter/buy/1").contentType("application/json").content("{\"expectedPrice\":101}")).andExpect(status().isUnauthorized());
        db.update("update `user` set balance=500 where id=2");
        wallet.tip(2L,1L,new TipReqDto(100L,"测试","privacy-tip-request1"));
        JsonNode reader=ok("GET","/api/app/wallet/flows",token,null),author=ok("GET","/api/app/wallet/flows",authorToken,null);
        assertThat(reader.path("total").asInt()).isEqualTo(1);assertThat(author.path("total").asInt()).isEqualTo(1);
        assertThat(reader.path("records").get(0).path("type").asInt()).isEqualTo(3);
        assertThat(author.path("records").get(0).path("type").asInt()).isEqualTo(4);
        when(values.setIfAbsent(anyString(),anyString(),any(Duration.class))).thenReturn(false);
        assertThat(call("POST","/api/app/chapter/buy/1",token,Map.of("expectedPrice",101)).path("code").asInt()).isEqualTo(429);
        assertThat(count("chapter_purchase")).isZero();
    }
    @Test void forumPostAndReplyLikesPersistAndRespectModeration() throws Exception {
        long post=ok("POST","/api/app/interaction/forum/posts",token,Map.of("title","找书","content","找书内容")).asLong();
        String path="/api/app/interaction/forum/posts/"+post+"/like";
        assertThat(call("POST",path,token,null).path("code").asInt()).isEqualTo(5004);
        db.update("update forum_post set audit_status=1 where id=?",post);
        assertThat(ok("POST",path,token,null).asLong()).isEqualTo(1);assertThat(ok("POST",path,token,null).asLong()).isEqualTo(1);
        assertThat(ok("GET","/api/app/forum/posts/"+post,token,null).path("liked").asBoolean()).isTrue();
        assertThat(ok("GET","/api/app/forum/posts",null,null).path("records").get(0).path("likeCount").asInt()).isEqualTo(1);
        assertThat(ok("GET","/api/app/forum/posts/"+post,null,null).path("liked").asBoolean()).isFalse();
        long reply=ok("POST","/api/app/interaction/forum/posts/"+post+"/replies",token,Map.of("content","回复")).asLong();
        String rp="/api/app/interaction/forum/replies/"+reply+"/like";
        assertThat(call("POST",rp,token,null).path("code").asInt()).isEqualTo(5004);
        db.update("update forum_reply set audit_status=1 where id=?",reply);
        ok("POST",rp,token,null);ok("POST",rp,token,null);
        assertThat(ok("GET","/api/app/forum/posts/"+post+"/replies",token,null).path("records").get(0).path("liked").asBoolean()).isTrue();
        assertThat(ok("DELETE",rp,token,null).asInt()).isZero();assertThat(ok("DELETE",rp,token,null).asInt()).isZero();
        ok("DELETE",path,token,null);assertThat(count("forum_like")).isZero();
        db.update("update forum_post set status=1 where id=?",post);
        assertThat(call("POST",rp,token,null).path("code").asInt()).isEqualTo(5004);
        mvc.perform(post(path)).andExpect(status().isUnauthorized());
    }
}
