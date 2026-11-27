package com.xunye.service.impl;
import com.xunye.constant.RedisKeyConstant;
import com.xunye.dao.UserMapper;
import com.xunye.entity.User;
import com.xunye.enums.ErrorCode;
import com.xunye.exception.BizException;
import com.xunye.util.JwtUtils;
import com.xunye.service.AuthSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * App会话校验统一入口：解析JWT → 查登出黑名单 → 查用户是否存在/封禁。
 * JWT验签通过只证明令牌有效，还必须检查服务端实时状态。
 */
// @Service：把该实现类交给Spring管理，Controller注入Service接口时可获得这个Bean。
@Service
// @RequiredArgsConstructor：Lombok为未初始化的final字段生成构造方法，Spring通过构造方法注入依赖。
@RequiredArgsConstructor
public class AuthSessionServiceImpl implements AuthSessionService {
    private final JwtUtils jwt;
    private final UserMapper users;
    private final StringRedisTemplate redis;
    /** 兼容现有App的裸token与通用Bearer前缀，最终只保留JWT字符串。 */
    // @Override：标记这是Service接口方法的实现，编译器会检查签名；它本身不负责开启事务。
    @Override
    public String normalize(String header) {
        if (header == null) return "";
        return header.startsWith("Bearer ") ? header.substring(7).trim() : header.trim();
    }
    /** 强制登录：成功返回当前用户实体供服务内部使用，失败抛业务异常。 */
    @Override
    public User require(String header) {
        String token = normalize(header);
        Long id;
        try {
            if (!StringUtils.hasText(token)) throw new IllegalArgumentException();
            // 明确读取userId；管理员令牌只有adminId，因此不能冒充App用户。
            id = jwt.parseId(token, JwtUtils.CLAIM_USER_ID);
            if (id == null) throw new IllegalArgumentException();
        } catch (Exception e) { throw new BizException(ErrorCode.UNAUTHORIZED); }
        // JWT无法主动撤销，黑名单用于让退出登录的令牌在到期前立即失效。
        if (redis.hasKey(blackKey(token))) throw new BizException(ErrorCode.UNAUTHORIZED);
        // 每次检查数据库状态，使账号被封禁或逻辑删除后无需等待JWT过期。
        User u = users.selectById(id);
        if (u == null) throw new BizException(ErrorCode.UNAUTHORIZED);
        if (!Integer.valueOf(0).equals(u.getStatus())) throw new BizException(ErrorCode.USER_BANNED);
        return u;
    }
    /**
     * 可选登录：供书城、评论等游客可浏览的接口使用。
     * 没有令牌或身份不可用时按游客处理；不能用这个方法保护写接口。
     */
    @Override
    public Long optional(String header) {
        if (!StringUtils.hasText(header)) return null;
        try {
            return require(header).getId();
        }
        catch (BizException e) { return null; }
    }
    /**
     * 控制器进入前已经过强制登录检查；黑名单TTL只设为token剩余有效期。
     * token自然过期后黑名单自动删除，避免永久占用Redis。
     */
    @Override
    public void logout(String header) {
        String token = normalize(header);
        long ttl = jwt.remainingMillis(token);
        if (ttl > 0) redis.opsForValue().set(blackKey(token),"1",Duration.ofMillis(ttl));
    }
    /** Redis键保存token的SHA-256摘要，而非完整登录凭证，减少原始令牌暴露。 */
    private String blackKey(String token) {
        try {
            return RedisKeyConstant.TOKEN_BLACKLIST + HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
