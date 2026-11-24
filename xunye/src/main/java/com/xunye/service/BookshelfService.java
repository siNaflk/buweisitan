package com.xunye.service;

import com.xunye.dto.resp.BookshelfRespDto;

import java.util.List;

/**
 * 书架服务：列表、收藏、取消收藏
 */
public interface BookshelfService {

    /** 书架列表（连书籍与阅读进度） */
    List<BookshelfRespDto> list(Long userId);

    /** 加入书架（重复收藏幂等返回 true） */
    void add(Long userId, Long bookId);

    /** 移出书架（未收藏幂等返回） */
    void remove(Long userId, Long bookId);
}
