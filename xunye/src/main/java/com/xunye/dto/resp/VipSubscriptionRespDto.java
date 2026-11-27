package com.xunye.dto.resp;
/** 订阅交易结果及记录，保存购买时的名称和折扣快照。 */
public record VipSubscriptionRespDto(Long id,String orderNo,Long planId,String planName,Long amount,int discount,java.time.LocalDateTime startTime,java.time.LocalDateTime expireTime,boolean alreadyProcessed) {}
