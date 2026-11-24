package com.xunye.util;
import com.xunye.service.AuthSessionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
/** 游客浏览接口的身份辅助器：登录有效则获取userId，否则按游客处理。 */
@Component @RequiredArgsConstructor
public class AuthHelper {

    private final AuthSessionService sessions;
    /** 供详情收藏状态、评论点赞状态等读取场景使用，不用于写接口的强制鉴权。 */
    public Long resolveOptionalUserId(HttpServletRequest request) {
        return sessions.optional(request.getHeader("Authorization"));
    }
}
