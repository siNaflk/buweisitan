package com.xunye.service;

import com.xunye.dto.req.BookQueryReqDto;
import com.xunye.dto.resp.BookBriefRespDto;
import com.xunye.dto.resp.BookDetailRespDto;
import com.xunye.dto.resp.ChapterBriefRespDto;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;
import java.util.Map;

/**
 * 书城服务：首页推荐、分类列表、搜索、详情、章节目录
 */
public interface BookService {

    /** 书城首页三组推荐（热门 / 最新 / 会员书库） */
    Map<String, List<BookBriefRespDto>> homeBooks();

    /** 分类书籍分页列表 */
    Page<BookBriefRespDto> listBooks(BookQueryReqDto req);

    /** 书籍详情（chapterCount、isCollected） */
    BookDetailRespDto detail(Long bookId, Long userId);

    /** 章节目录（仅已过审章节，按序号正序） */
    List<ChapterBriefRespDto> chapters(Long bookId);

    /** 按书名 / 作者名模糊搜索 */
    Page<BookBriefRespDto> search(String keyword, Integer pageNum, Integer pageSize);

    /** 相关推荐：同分类随机查询，不足时以其他分类随机补齐 */
    List<BookBriefRespDto> related(Long bookId, Integer limit);

    /** 首页随机推荐：优先收藏分类，排除已收藏；userId为空时按游客处理，最多返回6本。 */
    List<BookBriefRespDto> recommendation(Long userId);
}
