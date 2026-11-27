package com.xunye.constant;

/**
 * Redis 缓存键常量
 * <p>命名风格：Cache::XunYe::业务名::业务键
 */
public final class RedisKeyConstant {

    /** 图形验证码缓存键前缀，拼接 sessionId */
    public static final String IMG_VERIFY_CODE = "Cache::XunYe::imgVerifyCodeCache::";

    /** Redis锁、访问统计和限流统一使用此前缀；更改前缀会影响历史键，不能随意修改。 */
    public static final String WALLET_LOCK="Cache::XunYe::wallet:lock::";
    public static final String VISIT_PENDING="Cache::XunYe::visit:pending";
    public static final String RATE_LIMIT="Cache::XunYe::rate::";
    public static final String TOKEN_BLACKLIST="Cache::XunYe::token:black::";
    private RedisKeyConstant() {
        throw new UnsupportedOperationException("常量类不允许实例化");
    }
}
