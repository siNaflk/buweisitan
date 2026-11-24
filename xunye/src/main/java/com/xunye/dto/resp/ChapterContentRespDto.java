package com.xunye.dto.resp;

import lombok.Data;

import java.io.Serializable;

/**
 * 章节内容 DTO（含阅读权限判定结果）
 */
@Data
public class ChapterContentRespDto implements Serializable {

    /** 作品 ID */
    private Long bookId;

    /** 书名 */
    private String bookTitle;

    /** 章节 ID */
    private Long chapterId;

    /** 章节名 */
    private String chapterTitle;

    /** 正文（locked 为 true 时为 null） */
    private String content;

    /** 字数 */
    private Integer wordCount;

    /** 1 免费 / 0 付费 */
    private Integer isFree;

    /** 购买价格（分），locked 时前端展示用 */
    private Long price;

    /** true=无权限阅读，需购买 / false=可读 */
    private Boolean locked;

    /** 当前用户是否 VIP（前端展示用，未登录为 false） */
    private Boolean isVip;

    /** 章节序号 */
    private Integer sort;
}
