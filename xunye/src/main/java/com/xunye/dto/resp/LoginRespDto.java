package com.xunye.dto.resp;
/** 登录响应：token是后续请求的凭证，user仅包含可向本人展示的脱敏资料。 */
public record LoginRespDto(String token, UserRespDto user) {}
