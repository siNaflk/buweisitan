package com.xunye.constant;
/** 验证码及访问频率的统一业务限制，单位见字段名。 */
public final class AuthConstant {
    public static final int CAPTCHA_TTL_MINUTES=5, CAPTCHA_IP_LIMIT=30, CAPTCHA_WINDOW_SECONDS=300;
    public static final int LOGIN_LIMIT=10, REGISTER_LIMIT=10, AUTH_WINDOW_SECONDS=600;
    public static final int PUBLISH_LIMIT=10, PUBLISH_WINDOW_SECONDS=60;
    private AuthConstant(){throw new UnsupportedOperationException("常量类不允许实例化");}
}
