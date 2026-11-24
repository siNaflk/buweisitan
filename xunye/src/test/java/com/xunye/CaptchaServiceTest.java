package com.xunye;
import com.xunye.service.CaptchaService;
import com.xunye.service.impl.CaptchaServiceImpl;
import com.xunye.exception.BizException;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.script.RedisScript;
import java.util.*;
import java.time.Duration;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;
class CaptchaServiceTest {
    @Test void imageContainsNoPlainAnswerAndStoresFiveMinuteCode() {
        StringRedisTemplate redis=mock(StringRedisTemplate.class);
        ValueOperations<String,String> values=mock(ValueOperations.class);when(redis.opsForValue()).thenReturn(values);
        var image=new CaptchaServiceImpl(redis).create();
        assertThat(image.keySet()).containsExactlyInAnyOrder("captchaId","image");
        assertThat(image.get("image")).startsWith("data:image/png;base64,");
        verify(values).set(contains(image.get("captchaId")),matches("[0-9]{4}"),eq(Duration.ofMinutes(5)));
    }
    @Test void atomicConsumptionRejectsReplayAndWrongAnswer() {
        StringRedisTemplate redis=mock(StringRedisTemplate.class);
        when(redis.execute(any(RedisScript.class),anyList())).thenReturn("1234",null,"1234");
        CaptchaService service=new CaptchaServiceImpl(redis);
        service.verify("id","1234");
        assertThatThrownBy(()->service.verify("id","1234")).isInstanceOf(BizException.class);
        assertThatThrownBy(()->service.verify("other","9999")).isInstanceOf(BizException.class);
    }
}
