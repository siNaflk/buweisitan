package com.xunye.dto.req;

import lombok.Data;

import java.io.Serializable;

/**
 * 书籍列表查询 DTO
 */
@Data
public class BookQueryReqDto implements Serializable {

    /** 分类 ID（可选） */
    private Long categoryId;

    /** 排序：visit 点击 / newest 最新 / update 更新（默认 update） */
    private String sort;

    /** 页码（默认 1） */
    private Integer pageNum = 1;

    /** 每页数量（默认 10，最大 50） */
    private Integer pageSize = 10;
}
