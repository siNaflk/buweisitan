package com.xunye.dto.resp;
/** 充值订单快照：0待支付，1已入账，2已关闭。 */
public record RechargeOrderRespDto(Long id, String orderNo, Long planId, Long amount, Long bonusAmount, Integer status, java.time.LocalDateTime expiresAt) {}
