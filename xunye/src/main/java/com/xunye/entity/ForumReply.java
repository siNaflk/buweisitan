package com.xunye.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 论坛回复表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("forum_reply")
public class ForumReply extends BaseEntity {

    /** 帖子 ID */
    private Long postId;

    /** 回复人 ID */
    private Long userId;

    /** 回复内容 */
    private String content;

    /** 0 待审 / 1 通过 / 2 驳回 */
    private Integer auditStatus;

    /** 逻辑删除：0 否 1 是 */
    @TableLogic
    private Integer isDeleted;
}
