package com.xunye.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 论坛帖子表实体（找书）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("forum_post")
public class ForumPost extends BaseEntity {

    /** 发帖人 ID */
    private Long userId;

    /** 标题 */
    private String title;

    /** 正文 */
    private String content;

    /** 回复数 */
    private Integer replyCount;

    /** 0 待审 / 1 通过 / 2 驳回 */
    private Integer auditStatus;

    /** 0 正常 / 1 已封帖 */
    private Integer status;

    /** 逻辑删除：0 否 1 是 */
    @TableLogic
    private Integer isDeleted;
}
