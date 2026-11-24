package com.xunye.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 自定义配置项（对应 application.yml 中 xunye. 前缀）
 */
@Data
@Component
@ConfigurationProperties(prefix = "xunye")
public class XunyeProperties {

    /** 跨域配置 */
    private Cors cors = new Cors();

    /** JWT 配置 */
    private Jwt jwt = new Jwt();

    @Data
    public static class Cors {
        /** 允许跨域的来源 */
        private List<String> allowOrigins;
    }

    @Data
    public static class Jwt {
        /** 签名密钥（生产环境必须通过 JWT_SECRET 环境变量注入） */
        private String secret;
        /** token 有效期（天） */
        private int expireDays = 7;
    }
}
