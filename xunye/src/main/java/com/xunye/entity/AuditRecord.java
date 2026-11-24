package com.xunye.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 审核记录表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("audit_record")
public class AuditRecord extends BaseEntity {

    /** 审核人 ID */
    private Long adminId;

    /** 1 章节 / 2 帖子 / 3 回复 / 4 评论 */
    private Integer targetType;

    /** 审核对象 ID */
    private Long targetId;

    /** 1 通过 / 2 驳回 */
    private Integer result;

    /** 驳回理由 */
    private String reason;
}
