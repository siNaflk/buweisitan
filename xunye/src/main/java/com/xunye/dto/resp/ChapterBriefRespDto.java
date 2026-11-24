package com.xunye.dto.resp;

import lombok.Data;

import java.io.Serializable;

/**
 * 章节目录项 DTO
 */
@Data
public class ChapterBriefRespDto implements Serializable {

    private Long id;

    /** 章节名 */
    private String title;

    /** 字数 */
    private Integer wordCount;

    /** 1 免费 / 0 付费 */
    private Integer isFree;

    /** 付费价格（分） */
    private Long price;

    /** 章节序号 */
    private Integer sort;
}
