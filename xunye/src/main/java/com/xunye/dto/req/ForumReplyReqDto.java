package com.xunye.dto.req;
import jakarta.validation.constraints.*;
/** 回复正文，最多500字符；帖子ID来自路径，回复用户来自登录上下文。 */
public record ForumReplyReqDto(@NotBlank @Size(max=500) String content) {}
