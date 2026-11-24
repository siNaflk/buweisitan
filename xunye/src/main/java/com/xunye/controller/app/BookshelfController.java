package com.xunye.controller.app;

import com.xunye.common.Result;
import com.xunye.common.UserContext;
import com.xunye.dto.resp.BookshelfRespDto;
import com.xunye.service.BookshelfService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 书架接口（App 端，需登录，由 JwtAuthInterceptor 保护）
 */
// @RestController：注册为Spring控制器，方法返回值由框架序列化成JSON响应。
@RestController
// @RequestMapping：设置这个控制器的公共路径前缀；完整接口路径还要拼接方法上的映射。
@RequestMapping("/api/app/user")
// @RequiredArgsConstructor：Lombok为未初始化的final字段生成构造方法，Spring通过构造方法注入依赖。
@RequiredArgsConstructor
public class BookshelfController {

    private final BookshelfService bookshelfService;

    /** 我的书架 */
    // 接口：GET /api/app/user/bookshelves；由Spring根据HTTP方法和路径匹配到下方方法。
    @GetMapping("/bookshelves")
    public Result<List<BookshelfRespDto>> bookshelves() {
        return Result.ok(bookshelfService.list(UserContext.getUserId()));
    }

    /** 加入书架 */
    // 接口：POST /api/app/user/bookshelf/{bookId}；由Spring根据HTTP方法和路径匹配到下方方法。
    // @PathVariable：读取URL路径中的资源ID，例如{id}；它不是当前登录用户ID。
    @PostMapping("/bookshelf/{bookId}")
    public Result<Void> add(@PathVariable Long bookId) {
        bookshelfService.add(UserContext.getUserId(), bookId);
        return Result.ok();
    }

    /** 移出书架 */
    // 接口：DELETE /api/app/user/bookshelf/{bookId}；由Spring根据HTTP方法和路径匹配到下方方法。
    // @PathVariable：读取URL路径中的资源ID，例如{id}；它不是当前登录用户ID。
    @DeleteMapping("/bookshelf/{bookId}")
    public Result<Void> remove(@PathVariable Long bookId) {
        bookshelfService.remove(UserContext.getUserId(), bookId);
        return Result.ok();
    }
}
