package com.xunye.dto.req;
import jakarta.validation.constraints.*;
/** 发帖参数：标题最多100字符，正文最多5000字符；审核状态由服务端决定。 */
public record ForumPostReqDto(@NotBlank @Size(max=100) String title,
    @NotBlank @Size(max=5000, message="帖子最多5000字") String content) {}
