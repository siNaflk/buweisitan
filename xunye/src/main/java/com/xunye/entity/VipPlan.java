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

    /** 历史折扣配置，90曾表示九折；当前仅兼容旧数据，VIP阅读不再据此计费。 */
    private Integer discount;

    /** 数据库权益文案（分号分隔）；当前套餐接口统一返回VipConstant中的畅读权益文案。 */
    private String privileges;

    /** 排序 */
    private Integer sort;

    /** 0 上架 / 1 下架 */
    private Integer status;

    /** 逻辑删除：0 否 1 是 */
    @TableLogic
    private Integer isDeleted;
}
