package com.xunye.controller.app;

import com.xunye.common.Result;
import com.xunye.dto.resp.ChapterContentRespDto;
import com.xunye.service.ChapterService;
import com.xunye.util.AuthHelper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 章节阅读接口（App 端）
 */
// @RestController：注册为Spring控制器，方法返回值由框架序列化成JSON响应。
@RestController
// @RequestMapping：设置这个控制器的公共路径前缀；完整接口路径还要拼接方法上的映射。
@RequestMapping("/api/app/chapter")
// @RequiredArgsConstructor：Lombok为未初始化的final字段生成构造方法，Spring通过构造方法注入依赖。
@RequiredArgsConstructor
public class ChapterController {

    private final ChapterService chapterService;
    private final AuthHelper authHelper;

    /**
     * 章节内容（游客可读免费章节；登录后按 VIP / 已购判定权限，locked 为 true 时无正文）
     */
    // 接口：GET /api/app/chapter/content/{id}；由Spring根据HTTP方法和路径匹配到下方方法。
    // @PathVariable：读取URL路径中的资源ID，例如{id}；它不是当前登录用户ID。
    @GetMapping("/content/{id}")
    public Result<ChapterContentRespDto> content(@PathVariable Long id, HttpServletRequest request) {
        return Result.ok(chapterService.content(id, authHelper.resolveOptionalUserId(request)));
    }
}
