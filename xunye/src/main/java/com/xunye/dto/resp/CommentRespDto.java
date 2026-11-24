package com.xunye.dto.resp;
import java.time.LocalDateTime;
/**
 * 评论响应：likeCount为总点赞数，liked为查看者是否已赞，mine为是否本人评论。
 * 前端可据此显示操作按钮，真实权限仍由后端校验。
 */
public record CommentRespDto(Long id, Long userId, String nickname, String avatar, String content,
    Integer likeCount, boolean liked, boolean mine, LocalDateTime createTime) {}
