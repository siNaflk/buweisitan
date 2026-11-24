package com.xunye.service;

import com.xunye.entity.User;

/**
 * App会话服务接口：统一令牌格式、强制/可选身份校验与退出登录。
 */
public interface AuthSessionService {

    /** 兼容现有App的裸token与通用Bearer前缀，最终只保留JWT字符串。 */
    String normalize(String header);

    /** 强制登录：成功返回当前用户实体供服务内部使用，失败抛业务异常。 */
    User require(String header);

    /**
     * 可选登录：供书城、评论等游客可浏览的接口使用。
     * 没有令牌或身份不可用时按游客处理；不能用这个方法保护写接口。
     */
    Long optional(String header);

    /**
     * 控制器进入前已经过强制登录检查；黑名单TTL只设为token剩余有效期。
     * token自然过期后黑名单自动删除，避免永久占用Redis。
     */
    void logout(String header);
}
