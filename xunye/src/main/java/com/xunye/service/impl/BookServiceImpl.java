package com.xunye.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xunye.constant.BookConstant;
import com.xunye.dao.BookChapterMapper;
import com.xunye.dao.BookMapper;
import com.xunye.dao.BookshelfMapper;
import com.xunye.dto.req.BookQueryReqDto;
import com.xunye.dto.resp.BookBriefRespDto;
import com.xunye.dto.resp.BookDetailRespDto;
import com.xunye.dto.resp.BookshelfRespDto;
import com.xunye.dto.resp.ChapterBriefRespDto;
import com.xunye.entity.Book;
import com.xunye.entity.BookChapter;
import com.xunye.entity.Bookshelf;
import com.xunye.enums.BookSortType;
import com.xunye.enums.ErrorCode;
import com.xunye.exception.BizException;
import com.xunye.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static com.xunye.constant.BookConstant.RECOMMENDATION_DEFAULT_SIZE;

/**
 * 书城服务实现
 */
// @Service：把该实现类交给Spring管理，Controller注入Service接口时可获得这个Bean。
@Service
// @RequiredArgsConstructor：Lombok为未初始化的final字段生成构造方法，Spring通过构造方法注入依赖。
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookMapper bookMapper;
    private final BookChapterMapper chapterMapper;
    private final BookshelfMapper bookshelfMapper;

    // @Override：标记这是Service接口方法的实现，编译器会检查签名；它本身不负责开启事务。
    @Override
    public Map<String, List<BookBriefRespDto>> homeBooks() {
        Map<String, List<BookBriefRespDto>> result = new HashMap<>(3);
        result.put("hot", queryHome(BookSortType.VISIT, null));
        result.put("newest", queryHome(BookSortType.CREATE, null));
        result.put("vipLibrary", queryHome(BookSortType.UPDATE, 1));
        return result;
    }
    @Override
    public Page<BookBriefRespDto> listBooks(BookQueryReqDto req) {
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<Book>()
                .eq(Book::getAuditStatus, 1)
                .ne(Book::getStatus, 2)
                .eq(req.getCategoryId() != null, Book::getCategoryId, req.getCategoryId());
        applySort(wrapper, BookSortType.of(req.getSort()));
        Page<Book> page = bookMapper.selectPage(new Page<>(req.getPageNum(), req.getPageSize()), wrapper);
        return convertPage(page);
    }

    @Override
    public BookDetailRespDto detail(Long bookId, Long userId) {
        Book book = bookMapper.selectById(bookId);
        if (book == null || book.getAuditStatus() != 1 || book.getStatus() == 2) {
            throw new BizException(ErrorCode.BOOK_NOT_EXIST);
        }
        BookDetailRespDto resp = new BookDetailRespDto();
        BeanUtils.copyProperties(book, resp);
        // 已上线章节数
        resp.setChapterCount(chapterMapper.selectCount(new LambdaQueryWrapper<BookChapter>()
                .eq(BookChapter::getBookId, bookId)
                .eq(BookChapter::getAuditStatus, 1)));
        // 当前用户收藏状态
        resp.setIsCollected(userId != null && bookshelfMapper.selectCount(
                new LambdaQueryWrapper<com.xunye.entity.Bookshelf>()
                        .eq(com.xunye.entity.Bookshelf::getUserId, userId)
                        .eq(com.xunye.entity.Bookshelf::getBookId, bookId)) > 0);
        return resp;
    }

    @Override
    public List<ChapterBriefRespDto> chapters(Long bookId) {
        checkBookOnline(bookId);
        Book book = bookMapper.selectById(bookId);
        boolean exclusive = Integer.valueOf(1).equals(book.getIsVipLibrary());
        List<BookChapter> chapters = chapterMapper.selectList(new LambdaQueryWrapper<BookChapter>()
                .eq(BookChapter::getBookId, bookId)
                .eq(BookChapter::getAuditStatus, 1)
                .orderByAsc(BookChapter::getSort));
        return chapters.stream().map(chapter -> {
            ChapterBriefRespDto dto = new ChapterBriefRespDto();
            BeanUtils.copyProperties(chapter, dto);
            dto.setVipLibrary(exclusive);
            return dto;
        }).toList();
    }

    @Override
    public Page<BookBriefRespDto> search(String keyword, Integer pageNum, Integer pageSize) {
        if (!StringUtils.hasText(keyword)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "搜索关键词不能为空");
        }
        int current = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 50);
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<Book>()
                .eq(Book::getAuditStatus, 1)
                .ne(Book::getStatus, 2)
                .and(w -> w.like(Book::getTitle, keyword).or().like(Book::getAuthorName, keyword))
                .orderByDesc(Book::getVisitCount);
        Page<Book> page = bookMapper.selectPage(new Page<>(current, size), wrapper);
        return convertPage(page);
    }

    /** 上线中的书籍校验，不存在则抛业务异常 */
    private void checkBookOnline(Long bookId) {
        Book book = bookMapper.selectById(bookId);
        if (book == null || book.getAuditStatus() != 1 || book.getStatus() == 2) {
            throw new BizException(ErrorCode.BOOK_NOT_EXIST);
        }
    }

    @Override
    public List<BookBriefRespDto> related(Long bookId, Integer limit) {
        int size = limit == null || limit < 1
                ? BookConstant.RELATED_DEFAULT_SIZE : Math.min(limit, BookConstant.RELATED_MAX_SIZE);
        Book book = bookMapper.selectById(bookId);
        if (book == null || book.getAuditStatus() != 1 || book.getStatus() == 2) {
            throw new BizException(ErrorCode.BOOK_NOT_EXIST);
        }
        // 同分类随机推荐，排除当前书籍
        List<BookBriefRespDto> result = new ArrayList<>(queryRandom(
                w -> w.eq(Book::getCategoryId, book.getCategoryId()).ne(Book::getId, bookId), size));
        // 同分类不足时，用其他分类随机补齐，保证推荐位不满屏空洞
        if (result.size() < size) {
            result.addAll(queryRandom(
                    w -> w.ne(Book::getCategoryId, book.getCategoryId()), size - result.size()));
        }
        return result;
    }

    /**
     * 首页“为你推荐”：收藏分类中随机选书，不足时从全站补齐。
     * 数量由服务端常量决定，不接受前端limit或userId参数。
     */
    @Override
    public List<BookBriefRespDto> recommendation(Long userId) {
        // 游客不查个人书架；登录用户取书架中的分类作为简单兴趣信号。
        List<BookshelfRespDto> bookshelf = userId == null
                ? List.of() : bookshelfMapper.selectBookshelfList(userId);
        List<Long> categoryIds = bookshelf.stream()
                .map(BookshelfRespDto::getCategoryId)
                .filter(Objects::nonNull)
                .distinct().toList();
        Set<Long> excludedIds = new HashSet<>();

        bookshelf.stream().map(BookshelfRespDto::getBookId)
                .filter(Objects::nonNull).forEach(excludedIds::add);

        List<BookBriefRespDto> result = new ArrayList<>();
        if (!categoryIds.isEmpty()) {
            // 一次IN查询涵盖所有偏好分类，总共最多6本
            result.addAll(queryRandom(w -> w.in(Book::getCategoryId, categoryIds)
                    .notIn(!excludedIds.isEmpty(), Book::getId, excludedIds),
                    RECOMMENDATION_DEFAULT_SIZE));
        }

        // 排除已选作品，避免补齐时重复；无书架或无分类时直接走这一段全站随机查询。
        result.stream().map(BookBriefRespDto::getId).forEach(excludedIds::add);
        int remaining = RECOMMENDATION_DEFAULT_SIZE - result.size();
        if (remaining > 0) {
            result.addAll(
                    queryRandom(w -> w.notIn(!excludedIds.isEmpty(),
                            Book::getId, excludedIds), remaining));
        }
        // 库存不足时允许少于6本；每次调用重新随机，但不保证相邻两批绝不重复。
        return result;
    }

    /** 随机查询上线中的书籍（MySQL ORDER BY RAND()，演示数据量下可接受） */
    private List<BookBriefRespDto> queryRandom(
            java.util.function.Consumer<LambdaQueryWrapper<Book>> condition, int limit) {
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<Book>()
                .eq(Book::getAuditStatus, 1)
                .ne(Book::getStatus, 2);
        condition.accept(wrapper);
        List<Book> books = bookMapper.selectList(wrapper.last("ORDER BY RAND() LIMIT " + limit));
        return books.stream().map(book -> {
            BookBriefRespDto dto = new BookBriefRespDto();
            BeanUtils.copyProperties(book, dto);
            return dto;
        }).toList();
    }

    private void applySort(LambdaQueryWrapper<Book> wrapper, BookSortType sortType) {
        switch (sortType) {
            case VISIT -> wrapper.orderByDesc(Book::getVisitCount);
            case CREATE -> wrapper.orderByDesc(Book::getCreateTime);
            default -> wrapper.orderByDesc(Book::getUpdateTime);
        }
    }

    private List<BookBriefRespDto> queryHome(BookSortType sortType, Integer vipLibrary) {
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<Book>()
                .eq(Book::getAuditStatus, 1)
                .ne(Book::getStatus, 2)
                .eq(vipLibrary != null, Book::getIsVipLibrary, vipLibrary);
        applySort(wrapper, sortType);
        List<Book> books = bookMapper.selectList(wrapper.last("LIMIT " + BookConstant.HOME_RECOMMEND_SIZE));
        return books.stream().map(book -> {
            BookBriefRespDto dto = new BookBriefRespDto();
            BeanUtils.copyProperties(book, dto);
            return dto;
        }).toList();
    }

    private Page<BookBriefRespDto> convertPage(Page<Book> page) {
        Page<BookBriefRespDto> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(book -> {
            BookBriefRespDto dto = new BookBriefRespDto();
            BeanUtils.copyProperties(book, dto);
            return dto;
        }).toList());
        return result;
    }
}
