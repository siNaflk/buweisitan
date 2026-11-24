package com.xunye.dto.resp;
/** 交易凭证；重复购章返回原记录，不重复扣款。 */
public record TransactionRespDto(String type, Long referenceId, Long amount, Long balance, boolean alreadyProcessed) {}
