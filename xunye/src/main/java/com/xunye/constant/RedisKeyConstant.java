package com.xunye.constant;

/**
 * Redis 缓存键常量
 * <p>命名风格：Cache::XunYe::业务名::业务键
 */
public final class RedisKeyConstant {

    /** 图形验证码缓存键前缀，拼接 sessionId */
    public static final String IMG_VERIFY_CODE = "Cache::XunYe::imgVerifyCodeCache::";

    private RedisKeyConstant() {
        throw new UnsupportedOperationException("常量类不允许实例化");
    }
}
