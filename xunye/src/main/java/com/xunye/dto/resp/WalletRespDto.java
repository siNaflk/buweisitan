package com.xunye.dto.resp;
/** 余额直读数据库，不缓存。 */
public record WalletRespDto(Long balance, boolean simulatedPaymentEnabled) {}
