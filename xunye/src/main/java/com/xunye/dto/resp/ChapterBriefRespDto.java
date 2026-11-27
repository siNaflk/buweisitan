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
    /** 会员专属目录全部展示专属标识，不能仅凭isFree显示免费。 */
    private Boolean vipLibrary;

    /** 付费价格（分） */
    private Long price;

    /** 章节序号 */
    private Integer sort;
}
