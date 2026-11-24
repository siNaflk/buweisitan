package com.xunye;
import com.xunye.dao.BookMapper;
import com.xunye.service.impl.BookVisitServiceImpl;
import org.junit.jupiter.api.*;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class BookVisitServiceTest {
    StringRedisTemplate redis;BookMapper books;HashOperations<String,Object,Object> hash;BookVisitServiceImpl service;
    @BeforeEach void setup(){redis=mock(StringRedisTemplate.class);books=mock(BookMapper.class);hash=mock(HashOperations.class);doReturn(hash).when(redis).opsForHash();service=new BookVisitServiceImpl(redis,books);ReflectionTestUtils.setField(service,"enabled",true);}
    @Test void recordAggregatesInRedisWithoutWritingDatabase(){service.record(7L);verify(hash).increment(anyString(),eq("7"),eq(1L));verifyNoInteractions(books);}
    @Test void redisFailureFallsBackToDatabase(){when(hash.increment(anyString(),any(),anyLong())).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("offline"));service.record(7L);verify(books).update(isNull(),any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));}
    @Test void flushReturnsCountsWhenDatabaseWriteFails(){when(hash.keys(anyString())).thenReturn(Set.of("7"));when(redis.execute(any(RedisScript.class),anyList(),any(Object[].class))).thenReturn(3L);when(books.update(isNull(),any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("offline"));service.flush();verify(hash).increment(anyString(),eq("7"),eq(3L));}
}
