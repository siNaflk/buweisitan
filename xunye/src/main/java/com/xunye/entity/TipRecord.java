package com.xunye.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 打赏记录表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tip_record")
public class TipRecord extends BaseEntity {

    /** 打赏单号 */
    private String tipNo;

    /** 打赏人 ID */
    private Long userId;

    /** 被打赏作品 ID */
    private Long bookId;

    /** 金额（分） */
    private Long amount;

    /** 附言 */
    private String message;
}
