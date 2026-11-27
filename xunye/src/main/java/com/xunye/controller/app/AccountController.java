package com.xunye.controller.app;
import com.xunye.constant.AuthConstant;
import com.xunye.common.Result;
import com.xunye.dto.req.AuthReqDto;
import com.xunye.dto.resp.*;
import com.xunye.service.*;
import com.xunye.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/**
 * App账号接口。captcha/register/login允许游客访问，me/logout由登录拦截器保护。
 * @Valid在方法执行前检查DTO中的手机号、长度和非空约束。
 */
// @RestController：注册为Spring控制器，方法返回值由框架序列化成JSON响应。
@RestController
// @RequestMapping：设置这个控制器的公共路径前缀；完整接口路径还要拼接方法上的映射。
@RequestMapping("/api/app/user")
// @RequiredArgsConstructor：Lombok为未初始化的final字段生成构造方法，Spring通过构造方法注入依赖。
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accounts;
    private final AuthSessionService sessions;
    private final CaptchaService captcha;

    /** 获取图形验证码；按直接连接IP限制为5分钟30次，不直接信任客户端伪造的转发头。 */
    // 接口：GET /api/app/user/captcha；由Spring根据HTTP方法和路径匹配到下方方法。
    @GetMapping("/captcha")
    public Result<Map<String,String>> captcha(HttpServletRequest request) {
        captcha.rateLimit("captcha:"+request.getRemoteAddr(),AuthConstant.CAPTCHA_IP_LIMIT,AuthConstant.CAPTCHA_WINDOW_SECONDS);
        return Result.ok(captcha.create());
    }

    /** 注册并自动登录；同一IP十分钟最多请求10次注册。 */
    // 接口：POST /api/app/user/register；由Spring根据HTTP方法和路径匹配到下方方法。
    // @RequestBody：将请求体JSON转换为DTO；@Valid触发DTO上的非空、长度、金额范围等校验。
    @PostMapping("/register")
    public Result<LoginRespDto> register(@Valid @RequestBody AuthReqDto req, HttpServletRequest request) {
        captcha.rateLimit("register:"+request.getRemoteAddr(),AuthConstant.REGISTER_LIMIT,AuthConstant.AUTH_WINDOW_SECONDS);
        return Result.ok(accounts.register(req));
    }

    /** 登录；以IP+手机号为维度限制十分钟10次尝试。 */
    // 接口：POST /api/app/user/login；由Spring根据HTTP方法和路径匹配到下方方法。
    // @RequestBody：将请求体JSON转换为DTO；@Valid触发DTO上的非空、长度、金额范围等校验。
    @PostMapping("/login")
    public Result<LoginRespDto> login(@Valid @RequestBody AuthReqDto req, HttpServletRequest request) {
        captcha.rateLimit("login:"+request.getRemoteAddr()+":"+req.getPhone(),AuthConstant.LOGIN_LIMIT,AuthConstant.AUTH_WINDOW_SECONDS);
        return Result.ok(accounts.login(req));
    }

    /** 获取当前用户资料，使用DTO脱敏；不能把数据库User实体直接返回。 */
    // 接口：GET /api/app/user/me；由Spring根据HTTP方法和路径匹配到下方方法。
    @GetMapping("/me")
    public Result<UserRespDto> me(HttpServletRequest request) {
        return Result.ok(UserRespDto.from(sessions.require(request.getHeader("Authorization"))));
    }


    /** 服务端撤销当前token；成功后前端再清理本地token及资料缓存。 */
    // 接口：POST /api/app/user/logout；由Spring根据HTTP方法和路径匹配到下方方法。
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        sessions.logout(request.getHeader("Authorization"));
        return Result.ok();
    }
}
