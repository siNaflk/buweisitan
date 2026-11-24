package com.xunye.util;

import com.xunye.config.XunyeProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类
 * <p>App 用户与管理员共用一套工具，claim 中分别放 userId / adminId
 */
@Component
@RequiredArgsConstructor
public class JwtUtils {

    /** claim 键：用户 ID */
    public static final String CLAIM_USER_ID = "userId";
    /** claim 键：管理员 ID */
    public static final String CLAIM_ADMIN_ID = "adminId";

    private final XunyeProperties xunyeProperties;

    /**
     * 为 App 用户签发 token
     */
    public String createUserToken(Long userId) {
        return createToken(CLAIM_USER_ID, userId);
    }

    /**
     * 为管理员签发 token
     */
    public String createAdminToken(Long adminId) {
        return createToken(CLAIM_ADMIN_ID, adminId);
    }

    /**
     * 解析 token，返回指定 claim 的值；token 无效或已过期抛出 JwtException
     */
    public Long parseId(String token, String claimKey) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        Object value = claims.get(claimKey);
        return value == null ? null : Long.valueOf(value.toString());
    }

    /** JWT只保存身份与有效期，不保存密码、余额或VIP状态等敏感/实时业务数据。 */
    private String createToken(String claimKey, Long id) {
        Date now = new Date();
        Date expire = new Date(now.getTime() + xunyeProperties.getJwt().getExpireDays() * 24L * 3600 * 1000);
        // jti使用随机UUID，使同一用户同一时间登录也得到不同令牌，可独立登出其中一枚。
        return Jwts.builder()
                .setId(java.util.UUID.randomUUID().toString())
                .setIssuedAt(now)
                .setExpiration(expire)
                .claim(claimKey, id)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /** 校验令牌后计算剩余毫秒数，供登出黑名单设置TTL使用；已过期令牌会在解析时抛异常。 */
    public long remainingMillis(String token) {
        return Jwts.parserBuilder().setSigningKey(getSigningKey()).build()
                .parseClaimsJws(token).getBody().getExpiration().getTime() - System.currentTimeMillis();
    }

    private javax.crypto.SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(xunyeProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
