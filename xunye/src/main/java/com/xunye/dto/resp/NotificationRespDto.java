package com.xunye.dto.resp;
/** 本人站内通知；不公开其他用户的消息。 */
public record NotificationRespDto(Long id,String title,String content,boolean read,java.time.LocalDateTime createTime) {}
