package com.xunye.dto.resp;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 书籍详情 DTO（含章节数与当前用户收藏状态）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BookDetailRespDto extends BookBriefRespDto implements Serializable {

    /** 已上线章节数 */
    private Long chapterCount;

    /** 当前用户是否已收藏（未登录恒为 false） */
    private Boolean isCollected;
}
