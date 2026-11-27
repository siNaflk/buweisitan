package com.xunye.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * 订阅详情快照，与VipSubscription通过subscriptionId关联。
 * 主订阅记录保存金额和时段；此表补充“当时买的是什么套餐”与“是哪次前端提交”。
 * 查询历史时使用快照，不受运营后来修改VipPlan影响；当前阅读权限统一看用户的VIP到期时间。
 */
@Data @EqualsAndHashCode(callSuper=true) @TableName("vip_subscription_detail")
public class VipSubscriptionDetail extends BaseEntity {
    /** 关联vip_subscription.id，指向这次购买的主记录。 */
    private Long subscriptionId;
    /** 购买用户ID；与requestId组成唯一约束，识别同一个用户的重复提交。 */
    private Long userId;
    /** 前端提交标识。它不是订阅订单号，而是避免网络重试导致再次扣款的依据。 */
    private String requestId;
    /** 购买时的套餐名称快照，而非当前套餐名称。 */
    private String planName;
    /** 本次购买增加的会员天数，不包含此前剩余天数。 */
    private Integer durationDays;
    /** 旧折扣字段的历史快照，仅为兼容保留；当前所有VIP均为畅读，不按折扣购章。 */
    private Integer discount;
}
