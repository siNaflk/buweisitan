package com.xunye.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user")
public class User extends BaseEntity {

    /** 用户名（登录凭证之一） */
    private String username;

    /** 手机号 */
    private String phone;

    /** 密码（BCrypt 加密） */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 头像 URL */
    private String avatar;

    /** 余额（分），仅钱包用途 */
    private Long balance;

    /** 0 读者 / 1 作者 */
    private Integer isAuthor;

    /** VIP 到期时间，NULL 表示非会员 */
    private LocalDateTime vipExpireTime;

    /** 0 正常 / 1 封禁 */
    private Integer status;

    /** 逻辑删除：0 否 1 是 */
    @TableLogic
    private Integer isDeleted;
}
