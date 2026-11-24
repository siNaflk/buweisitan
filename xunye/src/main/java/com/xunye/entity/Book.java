package com.xunye.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 作品表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("book")
public class Book extends BaseEntity {

    /** 书名 */
    private String title;

    /** 作者用户 ID */
    private Long authorId;

    /** 作者名快照 */
    private String authorName;

    /** 分类 ID */
    private Long categoryId;

    /** 封面 URL */
    private String coverUrl;

    /** 简介 */
    private String intro;

    /** 0 连载 / 1 完本 / 2 下架 */
    private Integer status;

    /** 0 待审 / 1 通过 / 2 驳回 */
    private Integer auditStatus;

    /** 是否会员书库作品：0 否 1 是 */
    private Integer isVipLibrary;

    /** 默认单章价格（分） */
    private Long chapterPrice;

    /** 总字数 */
    private Long wordCount;

    /** 点击量 */
    private Long visitCount;

    /** 收藏数 */
    private Long collectCount;

    /** 评论数 */
    private Long commentCount;

    /** 逻辑删除：0 否 1 是 */
    @TableLogic
    private Integer isDeleted;
}
