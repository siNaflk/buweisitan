package com.xunye.dto.resp;

import lombok.Data;

import java.io.Serializable;

/**
 * 分类响应 DTO
 */
@Data
public class CategoryRespDto implements Serializable {

    private Long id;

    /** 分类名 */
    private String name;
}
