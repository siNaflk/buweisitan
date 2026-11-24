package com.xunye.config;

import com.xunye.interceptor.JwtAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置：跨域 + 登录拦截
 * <p>App 端 H5 开发服务器与管理端前端部署地址不同，需要放行对应来源
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final XunyeProperties xunyeProperties;
    private final JwtAuthInterceptor jwtAuthInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(xunyeProperties.getCors().getAllowOrigins().toArray(new String[0]))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // M2增加/interaction保护评论写入与论坛个人操作；公开读取路径由Service控制可见范围。
        // 用户域与互动写接口需要登录；验证码、注册和登录明确排除在拦截范围外
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/api/app/user/**", "/api/app/interaction/**", "/api/app/wallet", "/api/app/wallet/**", "/api/app/chapter/buy/**", "/api/app/book/tip/**")
                .excludePathPatterns("/api/app/user/login", "/api/app/user/register", "/api/app/user/captcha");
    }
}
