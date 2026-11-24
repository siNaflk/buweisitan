package com.xunye.service;

import java.util.Map;

/**
 * 验证码与限流服务接口：定义验证码生成、一次性校验及请求频率限制。
 */
public interface CaptchaService {

    /**
     * 生成4位验证码：答案只存Redis，前端只得到随机captchaId和PNG图片的data URL。
     * captchaId是一次验证请求的标识，不是用户ID；有效期为5分钟。
     */
    Map<String, String> create();

    /**
     * 验证时通过Lua原子地GET后DEL：同一验证码最多被消费一次。
     * 即使答案错误也删除；Redis不参与数据库事务回滚，重试必须获取新图。
     */
    void verify(String id, String answer);

    /**
     * Redis固定窗口限流。key代表业务与用户/IP，limit为次数，seconds为窗口秒数。
     * Lua保证INCR与首次设置过期时间一起完成；窗口内后续请求不会延长过期时间。
     */
    void rateLimit(String key, int limit, int seconds);
}
