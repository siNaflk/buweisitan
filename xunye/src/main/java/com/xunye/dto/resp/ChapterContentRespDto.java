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
    /** 标准价格（分）；discount保留兼容，0表示VIP畅读、100表示原价，不再用于会员折扣。 */
    private Long originalPrice;
    private Integer discount;
    /** 作品是否会员书库；决定是否显示开通VIP后畅读入口。 */
    private Boolean vipLibrary;
    /** true表示必须开通VIP，前端不能展示单章购买入口。 */
    private Boolean vipRequired;
    /** 只有普通作品中被锁定的付费章可单独购买。 */
    private Boolean canPurchase;

    /** true=无权限阅读，结合vipRequired决定开通VIP或购章 / false=可读 */
    private Boolean locked;

    /** 当前用户是否 VIP（前端展示用，未登录为 false） */
    private Boolean isVip;

    /** 章节序号 */
    private Integer sort;
}
