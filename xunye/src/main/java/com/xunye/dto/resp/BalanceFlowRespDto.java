package com.xunye.dto.resp;
/** 个人余额流水；仅返回当前用户的记录。 */
public record BalanceFlowRespDto(Long id, String flowNo, Integer type, Long amount, Long balanceAfter, Integer refType, Long refId, String remark, java.time.LocalDateTime createTime) {}
