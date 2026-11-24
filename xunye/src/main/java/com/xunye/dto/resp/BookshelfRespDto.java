package com.xunye.dto.resp;

import lombok.Data;

import java.io.Serializable;

/**
 * 书架项 DTO（bookshelf 连 book，左连 reading_progress）
 */
@Data
public class BookshelfRespDto implements Serializable {

    /** 书架记录 ID */
    private Long shelfId;

    /** 作品 ID */
    private Long bookId;

    /** 书名 */
    private String title;

    /** 作者名 */
    private String authorName;

    /** 封面 URL */
    private String coverUrl;

    /** 分类 ID */
    private Long categoryId;

    /** 总字数 */
    private Long wordCount;

    /** 最近阅读章节 ID（无进度为 null） */
    private Long lastChapterId;

    /** 章内进度百分比（无进度为 null） */
    private Integer progress;
}
