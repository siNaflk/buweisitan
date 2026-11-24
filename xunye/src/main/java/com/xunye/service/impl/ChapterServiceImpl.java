package com.xunye.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xunye.dao.BookChapterMapper;
import com.xunye.dao.BookMapper;
import com.xunye.dao.ChapterPurchaseMapper;
import com.xunye.dao.UserMapper;
import com.xunye.dto.resp.ChapterContentRespDto;
import com.xunye.entity.Book;
import com.xunye.entity.BookChapter;
import com.xunye.entity.ChapterPurchase;
import com.xunye.entity.User;
import com.xunye.enums.ErrorCode;
import com.xunye.exception.BizException;
import com.xunye.service.ChapterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 章节服务实现：读取内容 + 阅读权限判定
 * <p>权限判定顺序：免费章节 → VIP 且会员书库 → 已购买 → 锁定
 */
// @Service：把该实现类交给Spring管理，Controller注入Service接口时可获得这个Bean。
@Service
// @RequiredArgsConstructor：Lombok为未初始化的final字段生成构造方法，Spring通过构造方法注入依赖。
@RequiredArgsConstructor
public class ChapterServiceImpl implements ChapterService {

    private final com.xunye.service.BookVisitService visits;
    private final BookChapterMapper chapterMapper;
    private final BookMapper bookMapper;
    private final UserMapper userMapper;
    private final ChapterPurchaseMapper purchaseMapper;

    // @Override：标记这是Service接口方法的实现，编译器会检查签名；它本身不负责开启事务。
    @Override
    public ChapterContentRespDto content(Long chapterId, Long userId) {
        BookChapter chapter = chapterMapper.selectById(chapterId);
        if (chapter == null || chapter.getAuditStatus() != 1) {
            throw new BizException(ErrorCode.CHAPTER_NOT_ONLINE);
        }
        Book book = bookMapper.selectById(chapter.getBookId());
        if (book == null || book.getAuditStatus() != 1 || book.getStatus() == 2) {
            throw new BizException(ErrorCode.BOOK_NOT_EXIST);
        }

        // 浏览量先聚合，定时写回；不将余额或购买记录放进缓存。
        visits.record(book.getId());

        boolean isVip = resolveVip(userId);
        boolean locked;
        if (chapter.getIsFree() == 1 || java.util.Objects.equals(userId,book.getAuthorId())) {
            // 1. 免费章节
            locked = false;
        } else if (isVip && book.getIsVipLibrary() == 1) {
            // 2. VIP 且作品在会员书库
            locked = false;
        } else {
            // 3. 已购买（游客无购买记录，直接锁定）
            long purchased = userId == null ? 0 : purchaseMapper.selectCount(
                    new LambdaQueryWrapper<ChapterPurchase>()
                            .eq(ChapterPurchase::getUserId, userId)
                            .eq(ChapterPurchase::getChapterId, chapterId));
            locked = purchased == 0;
        }

        ChapterContentRespDto resp = new ChapterContentRespDto();
        resp.setBookId(book.getId());
        resp.setBookTitle(book.getTitle());
        resp.setChapterId(chapter.getId());
        resp.setChapterTitle(chapter.getTitle());
        resp.setContent(locked ? null : chapter.getContent());
        resp.setWordCount(chapter.getWordCount());
        resp.setIsFree(chapter.getIsFree());
        resp.setPrice(chapter.getPrice()==null?book.getChapterPrice():chapter.getPrice());
        resp.setLocked(locked);
        resp.setIsVip(isVip);
        resp.setSort(chapter.getSort());
        return resp;
    }

    /** VIP 判定：vip_expire_time > 当前时间即有效（全项目统一规则，见设计文档 5.3） */
    private boolean resolveVip(Long userId) {
        if (userId == null) {
            return false;
        }
        User user = userMapper.selectById(userId);
        return user != null
                && user.getVipExpireTime() != null
                && user.getVipExpireTime().isAfter(LocalDateTime.now());
    }
}
