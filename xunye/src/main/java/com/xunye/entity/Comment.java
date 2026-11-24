package com.xunye.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 评论表实体（chapterId 为 NULL 时表示书评）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("comment")
public class Comment extends BaseEntity {

    /** 评论人 ID */
    private Long userId;

    /** 作品 ID */
    private Long bookId;

    /** 章节 ID，NULL 表示书评 */
    private Long chapterId;

    /** 评论内容 */
    private String content;

    /** 点赞数 */
    private Integer likeCount;

    /** 0 正常 / 1 已隐藏 */
    private Integer status;

    /** 逻辑删除：0 否 1 是 */
    @TableLogic
    private Integer isDeleted;
}
