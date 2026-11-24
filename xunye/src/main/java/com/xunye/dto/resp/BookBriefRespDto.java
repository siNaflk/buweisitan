package com.xunye.dto.resp;

import lombok.Data;

import java.io.Serializable;

/**
 * 书籍简要信息 DTO（列表 / 首页推荐用）
 */
@Data
public class BookBriefRespDto implements Serializable {

    private Long id;

    /** 书名 */
    private String title;

    /** 作者名快照 */
    private String authorName;

    /** 分类 ID */
    private Long categoryId;

    /** 封面 URL */
    private String coverUrl;

    /** 简介 */
    private String intro;

    /** 总字数 */
    private Long wordCount;

    /** 点击量 */
    private Long visitCount;

    /** 收藏数 */
    private Long collectCount;

    /** 默认单章价格（分） */
    private Long chapterPrice;

    /** 是否会员书库作品：0 否 1 是 */
    private Integer isVipLibrary;

    /** 0 连载 / 1 完本 */
    private Integer status;
}
