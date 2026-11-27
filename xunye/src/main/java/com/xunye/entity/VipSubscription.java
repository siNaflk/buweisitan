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

    /** 本次新增时段的开始时间：未到期续费取旧到期时间，首次/过期开通取当前时间。 */
    private LocalDateTime startTime;

    /** 本次新增时段的结束时间；开通成功时同时更新User.vipExpireTime作为总有效期。 */
    private LocalDateTime expireTime;

    /** 逻辑删除：0 否 1 是 */
    @TableLogic
    private Integer isDeleted;
}
