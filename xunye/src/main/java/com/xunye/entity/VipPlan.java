package com.xunye.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * VIP 套餐表实体（Web 端配置）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("vip_plan")
public class VipPlan extends BaseEntity {

    /** 套餐名：月卡/季卡/年卡 */
    private String name;

    /** 有效天数 */
    private Integer durationDays;

    /** 售价（分） */
    private Long price;

    /** 购书折扣，90 表示 9 折 */
    private Integer discount;

    /** 权益文案，分号分隔 */
    private String privileges;

    /** 排序 */
    private Integer sort;

    /** 0 上架 / 1 下架 */
    private Integer status;

    /** 逻辑删除：0 否 1 是 */
    @TableLogic
    private Integer isDeleted;
}
