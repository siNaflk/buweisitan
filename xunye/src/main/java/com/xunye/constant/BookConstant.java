package com.xunye.constant;

/**
 * 书城业务常量
 */
public final class BookConstant {

    /** 首页每组推荐数量 */
    public static final int HOME_RECOMMEND_SIZE = 6;

    /** 相关推荐默认数量 */
    public static final int RELATED_DEFAULT_SIZE = 4;

    /** 为你推荐默认数量 */
    public static final int RECOMMENDATION_DEFAULT_SIZE = 6;

    /** 相关推荐数量上限 */
    public static final int RELATED_MAX_SIZE = 10;

    private BookConstant() {
        throw new UnsupportedOperationException("常量类不允许实例化");
    }
}
