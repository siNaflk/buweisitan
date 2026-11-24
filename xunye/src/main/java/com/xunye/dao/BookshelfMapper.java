package com.xunye.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xunye.dto.resp.BookshelfRespDto;
import com.xunye.entity.Bookshelf;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 书架表 Mapper
 */
public interface BookshelfMapper extends BaseMapper<Bookshelf> {

    /**
     * 书架列表：连 book 取书籍信息，左连 reading_progress 取最近阅读章节与进度
     */
    @Select("""
            SELECT bs.id          AS shelf_id,
                   b.id           AS book_id,
                   b.title,
                   b.author_name  AS author_name,
                   b.cover_url    AS cover_url,
                   b.category_id  AS category_id,
                   b.word_count   AS word_count,
                   rp.chapter_id  AS last_chapter_id,
                   rp.progress
            FROM bookshelf bs
            JOIN book b ON b.id = bs.book_id AND b.is_deleted = 0 AND b.audit_status = 1
            LEFT JOIN reading_progress rp ON rp.user_id = bs.user_id AND rp.book_id = bs.book_id
            WHERE bs.user_id = #{userId} AND bs.is_deleted = 0
            ORDER BY bs.update_time DESC
            """)
    List<BookshelfRespDto> selectBookshelfList(Long userId);

    /**
     * 复活逻辑删除的书架记录（重新收藏场景），返回影响行数
     */
    @Update("UPDATE bookshelf SET is_deleted = 0 WHERE user_id = #{userId} AND book_id = #{bookId} AND is_deleted = 1")
    int revive(Long userId, Long bookId);
}
