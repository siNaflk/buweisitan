package com.xunye.controller;

import com.xunye.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 系统探活接口（脚手架冒烟测试用，可随时删除）
 */
// @RestController：注册为Spring控制器，方法返回值由框架序列化成JSON响应。
@RestController
public class SystemController {

    // 接口：GET /api/ping；由Spring根据HTTP方法和路径匹配到下方方法。
    @GetMapping("/api/ping")
    public Result<Map<String, Object>> ping() {
        return Result.ok(Map.of("service", "xunye", "status", "ok"));
    }
}
