package com.xunye.dto.req;
import jakarta.validation.constraints.*;
/**
 * 发布评论参数。bookId必须为正数；chapterId允许null（书评），非空为章评且必须属于bookId。
 * @NotBlank拒绝空白正文，@Size限制最多500字符；userId由服务端获取。
 */
public record CommentReqDto(
        @NotNull @Positive Long bookId,
        @Positive Long chapterId,
    @NotBlank @Size(max=500, message="评论最多500字") String content) {}
