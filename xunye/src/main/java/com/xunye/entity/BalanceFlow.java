package com.xunye.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 余额流水表实体（所有资金变动的唯一记账入口）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("balance_flow")
public class BalanceFlow extends BaseEntity {

    /** 流水号 */
    private String flowNo;

    /** 用户 ID（作者收益记在作者账户下） */
    private Long userId;

    /** 1 充值 / 2 购章支出 / 3 打赏支出 / 4 打赏收益 / 5 购章分成收益 / 6 管理员调整 */
    private Integer type;

    /** 变动金额（恒为正数，方向由 type 决定） */
    private Long amount;

    /** 变动后余额（分），对账用 */
    private Long balanceAfter;

    /** 关联对象类型：1 档位 / 2 章节 / 3 作品 / 4 套餐 */
    private Integer refType;

    /** 关联对象 ID */
    private Long refId;

    /** 备注 */
    private String remark;
}
