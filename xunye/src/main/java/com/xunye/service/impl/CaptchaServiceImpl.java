package com.xunye.service.impl;
import com.xunye.constant.AuthConstant;
import com.xunye.constant.RedisKeyConstant;
import com.xunye.enums.ErrorCode;
import com.xunye.exception.BizException;
import com.xunye.service.CaptchaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.*;

/** 一次性验证码，Lua GET+DEL 兼容 Redis 3，错误答案也会消费验证码。 */
// @Service：把该实现类交给Spring管理，Controller注入Service接口时可获得这个Bean。
@Service
// @RequiredArgsConstructor：Lombok为未初始化的final字段生成构造方法，Spring通过构造方法注入依赖。
@RequiredArgsConstructor
public class CaptchaServiceImpl implements CaptchaService {
    private final StringRedisTemplate redis;
    // 随机数用于验证码及图像干扰，不让验证码按时间等规律轻易预测。
    private final SecureRandom random = new SecureRandom();
    /**
     * 生成4位验证码：答案只存Redis，前端只得到随机captchaId和PNG图片的data URL。
     * captchaId是一次验证请求的标识，不是用户ID；有效期为5分钟。
     */
    // @Override：标记这是Service接口方法的实现，编译器会检查签名；它本身不负责开启事务。
    @Override
    public Map<String, String> create() {
        String id = UUID.randomUUID().toString();
        // %04d补齐前导0，例如随机到7时显示0007，始终保持4位。
        String code = String.format("%04d", random.nextInt(10000));
        BufferedImage image = new BufferedImage(160, 56, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(new Color(248, 243, 235)); g.fillRect(0, 0, 160, 56);
            // 绘制背景干扰线，再绘制数字；图片不依赖外部文件。
            for (int i=0;i<10;i++) {
                g.setColor(new Color(160+random.nextInt(80),160+random.nextInt(80),160+random.nextInt(80)));
                g.drawLine(random.nextInt(160),random.nextInt(56),random.nextInt(160),random.nextInt(56));
            }
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 32));
            for (int i=0;i<4;i++) {
                g.setColor(new Color(80+random.nextInt(60),50+random.nextInt(60),40+random.nextInt(60)));
                g.drawString(code.substring(i,i+1), 15+i*34, 39+random.nextInt(5));
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image,"png",out);
            // 以captchaId为键保存正确答案；接口返回值绝不包含明文答案字段。
            redis.opsForValue().set(RedisKeyConstant.IMG_VERIFY_CODE+id, code, Duration.ofMinutes(AuthConstant.CAPTCHA_TTL_MINUTES));
            return Map.of("captchaId",id,"image","data:image/png;base64,"+Base64.getEncoder().encodeToString(out.toByteArray()));
        } catch (java.io.IOException e) {
            throw new IllegalStateException("验证码生成失败",e);
        }
        // 无论是否生成成功都释放绘图资源。
        finally {
            g.dispose();
        }
    }
    /**
     * 验证时通过Lua原子地GET后DEL：同一验证码最多被消费一次。
     * 即使答案错误也删除；Redis不参与数据库事务回滚，重试必须获取新图。
     */
    @Override
    public void verify(String id, String answer) {
        String actual = redis.execute(new DefaultRedisScript<>(
            "local v=redis.call('get',KEYS[1]); redis.call('del',KEYS[1]); return v", String.class),
            java.util.List.of(RedisKeyConstant.IMG_VERIFY_CODE+id));
        if (actual == null || !actual.equals(answer))
            throw new BizException(ErrorCode.PARAM_ERROR,"验证码错误或已过期，请点击图片刷新");
    }

    /**
     * Redis固定窗口限流。key代表业务与用户/IP，limit为次数，seconds为窗口秒数。
     * Lua保证INCR与首次设置过期时间一起完成；窗口内后续请求不会延长过期时间。
     */
    @Override
    public void rateLimit(String key, int limit, int seconds) {
        Long count = redis.execute(new DefaultRedisScript<>(
            "local n=redis.call('incr',KEYS[1]); " + "if n==1 " + "then redis.call('expire',KEYS[1],ARGV[1]); " + "end; return n", Long.class),
            java.util.List.of(RedisKeyConstant.RATE_LIMIT+key), String.valueOf(seconds));
        if (count == null || count > limit) throw new BizException(429,"操作太频繁，请稍后再试");
    }
}
