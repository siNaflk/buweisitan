package com.xunye.constant;
/** 统一业务常量。金额单位为分；运营可变的价格/时长/折扣仍从数据库读取，不能写死在常量中。 */
public final class VipConstant {
    public static final int NO_DISCOUNT=100, REMIND_AHEAD_DAYS=3, MAX_PLAN_DAYS=3650;
    public static final String PRIVILEGE_SEPARATOR=";";
    public static final String VIP_LIBRARY_TAG="会员专属";
    public static final String READING_PRIVILEGES="会员专属作品畅读;普通作品全部章节畅读";
    private VipConstant() {throw new UnsupportedOperationException("常量类不允许实例化");}
}
