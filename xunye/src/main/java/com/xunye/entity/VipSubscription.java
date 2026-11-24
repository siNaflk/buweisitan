package com.xunye.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * VIP 订阅记录表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("vip_subscription")
public class VipSubscription extends BaseEntity {

    /** 订单号 */
    private String orderNo;

    /** 用户 ID */
    private Long userId;

    /** 套餐 ID */
    private Long planId;

    /** 实付金额（分） */
    private Long amount;

    /** 生效时间 */
    private LocalDateTime startTime;

    /** 到期时间 */
    private LocalDateTime expireTime;

    /** 逻辑删除：0 否 1 是 */
    @TableLogic
    private Integer isDeleted;
}
