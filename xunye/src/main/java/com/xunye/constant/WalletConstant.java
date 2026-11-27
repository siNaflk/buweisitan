package com.xunye.constant;
/** 统一业务常量。金额单位为分；运营可变的价格/时长/折扣仍从数据库读取，不能写死在常量中。 */
public final class WalletConstant {
    public static final int ORDER_PENDING=0, ORDER_PAID=1, ORDER_CLOSED=2;
    public static final int FLOW_RECHARGE=1, FLOW_PURCHASE=2, FLOW_TIP_OUT=3, FLOW_TIP_IN=4, FLOW_AUTHOR_INCOME=5, FLOW_VIP_SUBSCRIPTION=7;
    public static final int REF_PLAN=1, REF_CHAPTER=2, REF_BOOK=3, REF_VIP_PLAN=4;
    public static final int RECHARGE_EXPIRE_MINUTES=15, LOCK_SECONDS=15, DEFAULT_AUTHOR_SHARE_PERCENT=50;
    public static final long MAX_TIP_FEN=100000L;
    public static final String TYPE_PURCHASE="purchase", TYPE_TIP="tip";
    private WalletConstant() {throw new UnsupportedOperationException("常量类不允许实例化");}
}
