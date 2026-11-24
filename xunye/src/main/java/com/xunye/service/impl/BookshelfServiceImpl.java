package com.xunye.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.xunye.dao.BookMapper;
import com.xunye.dao.BookshelfMapper;
import com.xunye.dto.resp.BookshelfRespDto;
import com.xunye.entity.Book;
import com.xunye.entity.Bookshelf;
import com.xunye.enums.ErrorCode;
import com.xunye.exception.BizException;
import com.xunye.service.BookshelfService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 书架服务实现
 * <p>唯一约束 uk_user_book + 逻辑删除：重新收藏走"复活"更新而非新插入
 */
// @Service：把该实现类交给Spring管理，Controller注入Service接口时可获得这个Bean。
@Service
// @RequiredArgsConstructor：Lombok为未初始化的final字段生成构造方法，Spring通过构造方法注入依赖。
@RequiredArgsConstructor
public class BookshelfServiceImpl implements BookshelfService {

    private final BookshelfMapper bookshelfMapper;
    private final BookMapper bookMapper;

    // @Override：标记这是Service接口方法的实现，编译器会检查签名；它本身不负责开启事务。
    @Override
    public List<BookshelfRespDto> list(Long userId) {
        return bookshelfMapper.selectBookshelfList(userId);
    }

    @Override
    // @Transactional：通过Spring事务代理调用时启用事务；rollbackFor=Exception.class包含受检异常回滚。
    @Transactional(rollbackFor = Exception.class)
    public void add(Long userId, Long bookId) {
        Book book = bookMapper.selectById(bookId);
        if (book == null || book.getAuditStatus() != 1 || book.getStatus() == 2) {
            throw new BizException(ErrorCode.BOOK_NOT_EXIST);
        }
        long active = bookshelfMapper.selectCount(new LambdaQueryWrapper<Bookshelf>()
                .eq(Bookshelf::getUserId, userId)
                .eq(Bookshelf::getBookId, bookId));
        if (active > 0) {
            // 重复收藏幂等处理
            return;
        }
        // 先尝试复活逻辑删除的历史记录（唯一约束 uk_user_book 占位）
        int revived = bookshelfMapper.revive(userId, bookId);
        if (revived == 0) {
            Bookshelf shelf = new Bookshelf();
            shelf.setUserId(userId);
            shelf.setBookId(bookId);
            bookshelfMapper.insert(shelf);
        }
        bookMapper.update(null, new UpdateWrapper<Book>()
                .setSql("collect_count = collect_count + 1")
                .eq("id", bookId));
    }

    @Override
    // @Transactional：通过Spring事务代理调用时启用事务；rollbackFor=Exception.class包含受检异常回滚。
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long userId, Long bookId) {
        int rows = bookshelfMapper.delete(new LambdaQueryWrapper<Bookshelf>()
                .eq(Bookshelf::getUserId, userId)
                .eq(Bookshelf::getBookId, bookId));
        if (rows == 0) {
            // 未收藏，幂等返回
            return;
        }
        bookMapper.update(null, new UpdateWrapper<Book>()
                .setSql("collect_count = IF(collect_count > 0, collect_count - 1, 0)")
                .eq("id", bookId));
    }
}
