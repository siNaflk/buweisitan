package com.xunye.dto.resp;
import java.time.LocalDateTime;
/**
 * 帖子响应：列表content是摘要、详情是全文；replyCount仅统计审核通过的回复。
 * auditStatus：0待审/1通过/2驳回；status：0正常/1封帖；mine表示是否当前用户发布。
 */
public record ForumPostRespDto(Long id, Long userId, String nickname, String title, String content,
    Long replyCount, long likeCount, boolean liked, Integer auditStatus, Integer status, boolean mine, LocalDateTime createTime) {}
