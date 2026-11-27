package com.xunye.constant;
/** 统一业务常量。金额单位为分；运营可变的价格/时长/折扣仍从数据库读取，不能写死在常量中。 */
public final class MqConstant {
    public static final String ORDER_TIMEOUT="ORDER_TIMEOUT", VIP_OPENED="VIP_OPENED", VIP_EXPIRING="VIP_EXPIRING";
    public static final String ORDER_TOPIC="xunye-order-timeout-topic", VIP_TOPIC="xunye-vip-notify-topic";
    public static final String PRODUCER_GROUP="xunye-m4-producer", CONSUMER_GROUP="xunye-m4-consumer";
    public static final int PENDING=0, SENT=1, CONSUMED=2, BATCH_SIZE=20;
    public static final long RETRY_MAX_SECONDS=300, RECOVERY_LEASE_SECONDS=60;
    private MqConstant() {throw new UnsupportedOperationException("常量类不允许实例化");}
}
