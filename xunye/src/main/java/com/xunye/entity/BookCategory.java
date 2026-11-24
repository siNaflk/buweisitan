package com.xunye.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 作品分类表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("book_category")
public class BookCategory extends BaseEntity {

    /** 分类名 */
    private String name;

    /** 排序权重 */
    private Integer sort;

    /** 0 启用 / 1 停用 */
    private Integer status;

    /** 逻辑删除：0 否 1 是 */
    @TableLogic
    private Integer isDeleted;
}
