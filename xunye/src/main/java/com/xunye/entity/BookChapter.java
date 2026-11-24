package com.xunye.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 章节表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("book_chapter")
public class BookChapter extends BaseEntity {

    /** 作品 ID */
    private Long bookId;

    /** 章节名 */
    private String title;

    /** 正文 */
    private String content;

    /** 字数 */
    private Integer wordCount;

    /** 1 免费 / 0 付费 */
    private Integer isFree;

    /** 付费价格（分） */
    private Long price;

    /** 章节序号 */
    private Integer sort;

    /** 0 待审 / 1 通过 / 2 驳回 */
    private Integer auditStatus;

    /** 逻辑删除：0 否 1 是 */
    @TableLogic
    private Integer isDeleted;
}
