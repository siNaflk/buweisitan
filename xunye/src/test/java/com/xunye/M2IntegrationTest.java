package com.xunye;
import com.fasterxml.jackson.databind.*;
import com.xunye.service.CaptchaService;
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
import org.springframework.test.web.servlet.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
@Sql("/m2-schema.sql")
class M2IntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate db;
    @Autowired JwtUtils jwt;
    @MockBean CaptchaService captcha;
    @MockBean StringRedisTemplate redis;
    Map<String,Boolean> black;
    String token; Long userId;
    @BeforeEach void setup() throws Exception {
        black=new HashMap<>();
        ValueOperations<String,String> values=mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(redis.hasKey(anyString())).thenAnswer(i->black.containsKey(i.getArgument(0)));
        doAnswer(i->{black.put(i.getArgument(0),true);return null;}).when(values).set(anyString(),anyString(),any(java.time.Duration.class));
        JsonNode registered=call("POST","/api/app/user/register",null,
            Map.of("phone","13900000001","password","test12345","nickname","测试读者","captchaId","test","captcha","1234"));
        assertThat(registered.path("code").asInt()).isEqualTo(200);
        token=registered.path("data").path("token").asText();userId=registered.path("data").path("user").path("id").asLong();
    }
    JsonNode call(String method,String path,String auth,Object body) throws Exception {
        var req=request(org.springframework.http.HttpMethod.valueOf(method),path);
        if(auth!=null)req.header("Authorization",auth);
        if(body!=null)req.contentType("application/json").content(json.writeValueAsString(body));
        return json.readTree(mvc.perform(req).andReturn().getResponse().getContentAsString());
    }
    String registerOther() throws Exception {
        return call("POST","/api/app/user/register",null,Map.of("phone","13900000002","password","test12345","captchaId","test","captcha","1234")).path("data").path("token").asText();
    }
    @Test void registrationLoginAndPrivacy() throws Exception {
        String stored=db.queryForObject("select password from `user` where id=?",String.class,userId);
        assertThat(stored).startsWith("$2").isNotEqualTo("test12345");
        JsonNode me=call("GET","/api/app/user/me","Bearer "+token,null);
        assertThat(me.path("data").has("password")).isFalse();
        assertThat(me.path("data").path("maskedPhone").asText()).isEqualTo("139****0001");
        assertThat(call("POST","/api/app/user/register",null,Map.of("phone","13900000001","password","test12345","captchaId","x","captcha","1234")).path("code").asInt()).isEqualTo(400);
        assertThat(call("POST","/api/app/user/login",null,Map.of("phone","13900000001","password","wrongpass","captchaId","x","captcha","1234")).path("code").asInt()).isEqualTo(400);
        assertThat(call("POST","/api/app/user/login",null,Map.of("phone","13900000001","password","test12345","captchaId","x","captcha","1234")).path("code").asInt()).isEqualTo(200);
    }
    @Test void authBoundariesBanAndLogout() throws Exception {
        mvc.perform(get("/api/app/user/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/app/user/me").header("Authorization",jwt.createAdminToken(userId))).andExpect(status().isUnauthorized());
        db.update("update `user` set status=1 where id=?",userId);
        assertThat(call("GET","/api/app/user/me",token,null).path("code").asInt()).isEqualTo(5005);
        db.update("update `user` set status=0 where id=?",userId);
        assertThat(call("POST","/api/app/user/logout",token,null).path("code").asInt()).isEqualTo(200);
        mvc.perform(get("/api/app/user/me").header("Authorization",token)).andExpect(status().isUnauthorized());
    }
    @Test void commentsLikeIdempotencyOwnershipAndChapterValidation() throws Exception {
        assertThat(call("POST","/api/app/interaction/comments",token,Map.of("bookId",1,"chapterId",2,"content","错误关联")).path("code").asInt()).isEqualTo(5007);
        long id=call("POST","/api/app/interaction/comments",token,Map.of("bookId",1,"content","好书")).path("data").asLong();
        assertThat(id).isPositive();
        call("POST","/api/app/interaction/comments/"+id+"/like",token,null);
        call("POST","/api/app/interaction/comments/"+id+"/like",token,null);
        assertThat(db.queryForObject("select like_count from comment where id=?",Integer.class,id)).isEqualTo(1);
        assertThat(db.queryForObject("select count(*) from comment_like",Integer.class)).isEqualTo(1);
        JsonNode list=call("GET","/api/app/comments?bookId=1",token,null);
        assertThat(list.path("data").path("records").get(0).path("liked").asBoolean()).isTrue();
        assertThat(call("DELETE","/api/app/interaction/comments/"+id,registerOther(),null).path("code").asInt()).isEqualTo(403);
        call("DELETE","/api/app/interaction/comments/"+id+"/like",token,null);
        call("DELETE","/api/app/interaction/comments/"+id+"/like",token,null);
        assertThat(db.queryForObject("select like_count from comment where id=?",Integer.class,id)).isZero();
        call("DELETE","/api/app/interaction/comments/"+id,token,null);
        assertThat(db.queryForObject("select comment_count from book where id=1",Long.class)).isZero();
        assertThat(call("GET","/api/app/comments?bookId=1",null,null).path("data").path("total").asInt()).isZero();
    }
    @Test void moderationPrivatePendingAndReplyVisibility() throws Exception {
        long id=call("POST","/api/app/interaction/forum/posts",token,Map.of("title","找一本书","content","记得主角在海边")).path("data").asLong();
        assertThat(call("GET","/api/app/forum/posts",null,null).path("data").path("total").asInt()).isZero();
        assertThat(call("GET","/api/app/forum/posts/"+id,null,null).path("code").asInt()).isEqualTo(5004);
        assertThat(call("GET","/api/app/interaction/forum/mine",token,null).path("data").path("total").asInt()).isEqualTo(1);
        assertThat(call("POST","/api/app/interaction/forum/posts/"+id+"/replies",token,Map.of("content","线索")).path("code").asInt()).isEqualTo(5004);
        db.update("update forum_post set audit_status=1 where id=?",id);
        long reply=call("POST","/api/app/interaction/forum/posts/"+id+"/replies",token,Map.of("content","线索")).path("data").asLong();
        assertThat(call("GET","/api/app/forum/posts/"+id+"/replies",null,null).path("data").path("total").asInt()).isZero();
        assertThat(call("GET","/api/app/forum/posts/"+id+"/replies",token,null).path("data").path("total").asInt()).isEqualTo(1);
        db.update("update forum_reply set audit_status=1 where id=?",reply);
        assertThat(call("GET","/api/app/forum/posts/"+id,null,null).path("data").path("replyCount").asInt()).isEqualTo(1);
        db.update("update forum_post set status=1 where id=?",id);
        assertThat(call("POST","/api/app/interaction/forum/posts/"+id+"/replies",token,Map.of("content","不能回复")).path("code").asInt()).isEqualTo(5004);
        assertThat(call("DELETE","/api/app/interaction/forum/posts/"+id,registerOther(),null).path("code").asInt()).isEqualTo(403);
    }
    @Test void validatesInputAndPreservesM1Shelf() throws Exception {
        assertThat(call("POST","/api/app/interaction/comments",token,Map.of("bookId",1,"content","   ")).path("code").asInt()).isEqualTo(400);
        assertThat(call("POST","/api/app/interaction/forum/posts",token,Map.of("title","x","content","a".repeat(5001))).path("code").asInt()).isEqualTo(400);
        assertThat(call("POST","/api/app/user/bookshelf/1",token,null).path("code").asInt()).isEqualTo(200);
        assertThat(call("GET","/api/app/user/bookshelves",token,null).path("data").size()).isEqualTo(1);
        assertThat(call("GET","/api/app/book/detail/1",token,null).path("data").path("isCollected").asBoolean()).isTrue();
    }

    /** 游客/空书架也能访问，且随机结果只包含已上线、未删除作品。 */
    @Test void recommendationsSupportGuestsAndFilterOfflineBooks() throws Exception {
        seedRecommendationBooks();
        db.update("insert into book(id,title,author_id,author_name,category_id,status,audit_status,word_count,is_deleted) values(20,'下架',1,'作者',3,2,1,10,0),(21,'待审',1,'作者',3,0,0,10,0),(22,'删除',1,'作者',3,0,1,10,1)");
        JsonNode guest=call("GET","/api/app/book/recommendation",null,null);
        assertThat(guest.path("code").asInt()).isEqualTo(200);
        Set<Long> ids=recommendationIds(guest);
        assertThat(guest.path("data").size()).isEqualTo(6);
        assertThat(ids).hasSize(6).doesNotContain(20L,21L,22L);
        assertThat(call("GET","/api/app/book/recommendation",token,null).path("data").size()).isEqualTo(6);
    }

    /** 通过真实HTTP请求验证可选身份解析、多分类召回、已收藏排除与跨分类补齐。 */
    @Test void recommendationsUseAllShelfCategoriesAndFillWithoutDuplicates() throws Exception {
        seedRecommendationBooks();
        db.update("insert into bookshelf(user_id,book_id) values(?,1),(?,3)",userId,userId);
        JsonNode response=call("GET","/api/app/book/recommendation",token,null);
        // 分类1剩书2，分类2剩书4，两个分类都必须参与，而不是遇到首个非空分类就返回。
        Set<Long> ids=recommendationIds(response);
        assertThat(ids).contains(2L,4L).doesNotContain(1L,3L).hasSize(6);
        assertThat(response.path("data").size()).isEqualTo(6);
        assertThat(response.path("data").get(0).path("categoryId").asInt()).isIn(1,2);
        assertThat(response.path("data").get(1).path("categoryId").asInt()).isIn(1,2);
    }

    /** 同类候选足够时不补其他分类，且返回数量始终由服务端限制为6。 */
    @Test void recommendationsCapResultsWhenPreferredCategoryHasManyBooks() throws Exception {
        seedRecommendationBooks();
        db.update("update book set category_id=1 where id between 3 and 10");
        db.update("insert into bookshelf(user_id,book_id) values(?,1)",userId);
        JsonNode response=call("GET","/api/app/book/recommendation",token,null);
        assertThat(recommendationIds(response)).hasSize(6).doesNotContain(1L);
        for(JsonNode book:response.path("data")) assertThat(book.path("categoryId").asInt()).isEqualTo(1);
    }

    /** 只剩一本或全部收藏时允许不足6本，不能用已收藏书强行补齐。 */
    @Test void recommendationsReturnAvailableCountOrEmpty() throws Exception {
        db.update("insert into bookshelf(user_id,book_id) values(?,1)",userId);
        assertThat(recommendationIds(call("GET","/api/app/book/recommendation",token,null))).containsExactly(2L);
        db.update("insert into bookshelf(user_id,book_id) values(?,2)",userId);
        assertThat(call("GET","/api/app/book/recommendation",token,null).path("data").size()).isZero();
    }

    private void seedRecommendationBooks() {
        for(long id=3;id<=10;id++) {
            int category=id<=4?2:3;
            db.update("insert into book(id,title,author_id,author_name,category_id,status,audit_status,word_count) values(?,?,1,'作者',?,0,1,100)",id,"推荐测试书"+id,category);
        }
    }

    private Set<Long> recommendationIds(JsonNode response) {
        assertThat(response.path("code").asInt()).isEqualTo(200);
        Set<Long> ids=new HashSet<>();
        for(JsonNode book:response.path("data")) ids.add(book.path("id").asLong());
        return ids;
    }

}
