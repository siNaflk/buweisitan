package com.xunye.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
/** 充值下单时锁定金额与赠送额；支付重试不能重复入账。 */
@Data @EqualsAndHashCode(callSuper=true) @TableName("recharge_order")
public class RechargeOrder extends BaseEntity {
    private String orderNo;
    private Long userId;
    private Long planId;
    private String requestId;
    private Long amount;
    private Long bonusAmount;
    private Integer status;
    private LocalDateTime expiresAt;
    private LocalDateTime paidAt;
}
