package com.xunye.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 章节购买记录表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chapter_purchase")
public class ChapterPurchase extends BaseEntity {

    /** 购买人 ID */
    private Long userId;

    /** 章节 ID */
    private Long chapterId;

    /** 作品 ID（冗余，便于按书查询） */
    private Long bookId;

    /** 实付金额（分，已含 VIP 折扣） */
    private Long amount;

    /** 作者分成（分） */
    private Long authorIncome;
}
