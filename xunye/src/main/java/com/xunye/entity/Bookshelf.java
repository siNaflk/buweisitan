package com.xunye.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 书架表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bookshelf")
public class Bookshelf extends BaseEntity {

    /** 用户 ID */
    private Long userId;

    /** 作品 ID */
    private Long bookId;

    /** 逻辑删除：0 否 1 是 */
    @TableLogic
    private Integer isDeleted;
}
