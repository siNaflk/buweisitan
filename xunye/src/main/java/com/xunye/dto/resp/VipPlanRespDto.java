package com.xunye.dto.resp;
/** 套餐价格单位为分；discount是历史兼容字段，当前权益以畅读规则为准。 */
public record VipPlanRespDto(Long id,String name,Integer durationDays,Long price,Integer discount,String privileges) {}
