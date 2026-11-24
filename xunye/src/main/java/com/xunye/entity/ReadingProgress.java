package com.xunye.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 阅读进度表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("reading_progress")
public class ReadingProgress extends BaseEntity {

    /** 用户 ID */
    private Long userId;

    /** 作品 ID */
    private Long bookId;

    /** 最近阅读章节 ID */
    private Long chapterId;

    /** 章内进度百分比 0~100 */
    private Integer progress;
}
