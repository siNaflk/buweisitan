package com.xunye.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 充值档位表实体（Web 端配置）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("recharge_plan")
public class RechargePlan extends BaseEntity {

    /** 档位名，如 100 元档 */
    private String title;

    /** 充值金额（分） */
    private Long amount;

    /** 赠送金额（分） */
    private Long bonusAmount;

    /** 排序 */
    private Integer sort;

    /** 0 上架 / 1 下架 */
    private Integer status;

    /** 逻辑删除：0 否 1 是 */
    @TableLogic
    private Integer isDeleted;
}
