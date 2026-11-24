package com.xunye.enums;

import org.springframework.util.StringUtils;

/**
 * 书籍排序方式
 * <p>对应前端传入的 sort 参数：visit=最热（浏览量）、newest=最新（创建时间）、update=最近更新
 */
public enum BookSortType {

    /** 最热：按浏览量降序 */
    VISIT,

    /** 最新：按创建时间降序 */
    CREATE,

    /** 最近更新：按更新时间降序（默认） */
    UPDATE;

    /**
     * 将前端排序参数解析为枚举，无法识别时回退默认值 UPDATE
     */
    public static BookSortType of(String value) {
        if (!StringUtils.hasText(value)) {
            return UPDATE;
        }
        return switch (value.toLowerCase()) {
            case "visit" -> VISIT;
            case "newest" -> CREATE;
            default -> UPDATE;
        };
    }
}
