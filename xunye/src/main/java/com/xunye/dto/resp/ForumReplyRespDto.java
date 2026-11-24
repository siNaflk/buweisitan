package com.xunye.dto.resp;
import java.time.LocalDateTime;
/**
 * 回复响应：auditStatus为0待审/1通过/2驳回，mine表示是否当前用户的回复。
 * 非公开回复只可能出现在其发布者本人的响应里。
 */
public record ForumReplyRespDto(Long id, Long userId, String nickname, String content,
    Integer auditStatus, long likeCount, boolean liked, boolean mine, LocalDateTime createTime) {}
