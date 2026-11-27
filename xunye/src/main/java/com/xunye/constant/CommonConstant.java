package com.xunye.constant;
/** 统一业务常量。金额单位为分；运营可变的价格/时长/折扣仍从数据库读取，不能写死在常量中。 */
public final class CommonConstant {

    public static final int NORMAL=0, AUDIT_APPROVED=1, BOOK_OFFLINE=2, YES=1;
    public static final int DEFAULT_PAGE_SIZE=20, MAX_PAGE_SIZE=50, PERCENT_BASE=100;
    public static final int HTTP_CONFLICT=409, HTTP_TOO_MANY_REQUESTS=429;
    public static final long MIN_PAID_PRICE_FEN=1L;
    public static final String SHA_256="SHA-256";
    private CommonConstant() {throw new UnsupportedOperationException("常量类不允许实例化");}
}
