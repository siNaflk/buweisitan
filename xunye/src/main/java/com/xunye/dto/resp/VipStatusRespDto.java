package com.xunye.dto.resp;
/** 余额和权益直读数据库，过期立即停止权益；discount保留兼容，VIP为0（畅读），非VIP为100。 */
public record VipStatusRespDto(boolean active,java.time.LocalDateTime expireTime,int discount,Long balance) {}
