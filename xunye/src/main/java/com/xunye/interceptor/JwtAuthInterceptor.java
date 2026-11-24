package com.xunye.interceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xunye.common.*;
import com.xunye.exception.BizException;
import com.xunye.service.AuthSessionService;
import jakarta.annotation.Nonnull;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 强制登录拦截器：请求进入时校验会话并保存userId，请求结束时清理线程上下文。
 * 只有WebConfig注册的受保护路径会经过这里，游客接口使用AuthHelper可选解析身份。
 */
@Component @RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {
    private final AuthSessionService sessions;
    private final ObjectMapper objectMapper;
    @Override
    public boolean preHandle(HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull Object handler) throws Exception {
        // Web服务器线程会复用，先清理残留身份；认证失败也不能沿用上一请求的用户。
        UserContext.clear();
        // 浏览器跨域预检不携带登录凭证，只检查请求是否允许，放行后再校验实际业务请求。
        if ("OPTIONS".equals(request.getMethod())) return true;
        // 会话校验包括JWT签名/有效期、登出黑名单以及账号封禁状态。
        try {
            UserContext.setUserId(sessions.require(request.getHeader("Authorization")).getId());
            return true;
        }
        catch (BizException e) {
            // 拦截器中直接写响应：未登录用HTTP 401，其他身份拒绝用403，body保留具体业务码。
            response.setStatus(e.getCode()==401 ? 401 : 403);
            response.setContentType("application/json;charset=UTF-8");
            objectMapper.writeValue(response.getOutputStream(),Result.fail(e.getCode(),e.getMessage()));
            return false;
        }
    }
    /** 正常或异常结束都移除ThreadLocal，防止线程池复用导致用户身份串线。 */
    @Override
    public void afterCompletion(@Nonnull HttpServletRequest req, @Nonnull HttpServletResponse resp, @Nonnull Object handler, Exception ex) {
        UserContext.clear();
    }
}
