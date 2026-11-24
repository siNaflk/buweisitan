package com.xunye.controller.app;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xunye.common.Result;
import com.xunye.dto.req.BookQueryReqDto;
import com.xunye.dto.resp.BookBriefRespDto;
import com.xunye.dto.resp.BookDetailRespDto;
import com.xunye.dto.resp.ChapterBriefRespDto;
import com.xunye.service.BookService;
import com.xunye.util.AuthHelper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 书城接口（App 端）
 */
// @RestController：注册为Spring控制器，方法返回值由框架序列化成JSON响应。
@RestController
// @RequestMapping：设置这个控制器的公共路径前缀；完整接口路径还要拼接方法上的映射。
@RequestMapping("/api/app")
// @RequiredArgsConstructor：Lombok为未初始化的final字段生成构造方法，Spring通过构造方法注入依赖。
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final AuthHelper authHelper;

    /** 书城首页推荐（热门 / 最新 / 会员书库） */
    // 接口：GET /api/app/home/books；由Spring根据HTTP方法和路径匹配到下方方法。
    @GetMapping("/home/books")
    public Result<Map<String, List<BookBriefRespDto>>> home() {
        return Result.ok(bookService.homeBooks());
    }

    /** 分类书籍分页列表 */
    // 接口：GET /api/app/book/list；由Spring根据HTTP方法和路径匹配到下方方法。
    @GetMapping("/book/list")
    public Result<Page<BookBriefRespDto>> list(BookQueryReqDto req) {
        return Result.ok(bookService.listBooks(req));
    }

    /** 书籍详情 */
    // 接口：GET /api/app/book/detail/{id}；由Spring根据HTTP方法和路径匹配到下方方法。
    // @PathVariable：读取URL路径中的资源ID，例如{id}；它不是当前登录用户ID。
    @GetMapping("/book/detail/{id}")
    public Result<BookDetailRespDto> detail(@PathVariable Long id, HttpServletRequest request) {
        return Result.ok(bookService.detail(id, authHelper.resolveOptionalUserId(request)));
    }

    /** 章节目录 */
    // 接口：GET /api/app/book/chapters/{id}；由Spring根据HTTP方法和路径匹配到下方方法。
    // @PathVariable：读取URL路径中的资源ID，例如{id}；它不是当前登录用户ID。
    @GetMapping("/book/chapters/{id}")
    public Result<List<ChapterBriefRespDto>> chapters(@PathVariable Long id) {
        return Result.ok(bookService.chapters(id));
    }

    /** 相关推荐（同分类随机，limit 可选，默认 4） */
    // 接口：GET /api/app/book/related/{id}；由Spring根据HTTP方法和路径匹配到下方方法。
    // @PathVariable：读取URL路径中的资源ID，例如{id}；它不是当前登录用户ID。
    // @RequestParam：读取URL查询参数；required=false允许省略，defaultValue提供缺省值。
    @GetMapping("/book/related/{id}")
    public Result<List<BookBriefRespDto>> related(@PathVariable Long id,
                                                  @RequestParam(required = false) Integer limit) {
        return Result.ok(bookService.related(id, limit));
    }

    /** 搜索（书名 / 作者名模糊匹配） */
    // 接口：GET /api/app/book/search；由Spring根据HTTP方法和路径匹配到下方方法。
    // @RequestParam：读取URL查询参数；required=false允许省略，defaultValue提供缺省值。
    @GetMapping("/book/search")
    public Result<Page<BookBriefRespDto>> search(@RequestParam String keyword,
                                                 @RequestParam(required = false) Integer pageNum,
                                                 @RequestParam(required = false) Integer pageSize) {
        return Result.ok(bookService.search(keyword, pageNum, pageSize));
    }

    /** 为你推荐书籍 */
    // 接口：GET /api/app/book/recommendation；由Spring根据HTTP方法和路径匹配到下方方法。
    @GetMapping("/book/recommendation")
    public Result<List<BookBriefRespDto>> recommendation(HttpServletRequest request) {
        // 此接口允许游客访问，不经过强制登录拦截器，须主动解析可选身份。
        return Result.ok(bookService.recommendation(authHelper.resolveOptionalUserId(request)));
    }
}
