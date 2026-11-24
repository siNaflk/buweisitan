package com.xunye.dto.resp;
/** 上架充值档位，金额单位为分。 */
public record RechargePlanRespDto(Long id, String title, Long amount, Long bonusAmount) {}
