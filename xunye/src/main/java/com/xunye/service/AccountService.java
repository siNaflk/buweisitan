package com.xunye.service;

import com.xunye.dto.req.AuthReqDto;
import com.xunye.dto.resp.LoginRespDto;

/**
 * 账号服务接口：定义注册、登录能力，密码处理与JWT签发由实现类完成。
 */
public interface AccountService {

    /**
     * 注册成功即登录：插入用户后返回JWT和脱敏资料。
     * 注册不会充值或授予作者/VIP身份，相关权限由后续业务管理。
     */
    LoginRespDto register(AuthReqDto req);

    /** 登录：先消费验证码，再验证密码与账号状态，最后签发一枚新token。 */
    LoginRespDto login(AuthReqDto req);
}
