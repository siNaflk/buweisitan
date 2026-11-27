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

import com.xunye.service.VipService;
import com.xunye.constant.CommonConstant;

/**
 * 章节服务实现：读取内容 + 阅读权限判定
 * <p>先区分会员专属：专属作品只认VIP；普通作品按VIP → 免费/作者本人 → 已购买判定。
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
    private final VipService vip;
    private final ChapterPurchaseMapper purchaseMapper;

    // @Override：标记这是Service接口方法的实现，编译器会检查签名；它本身不负责开启事务。
    @Override
    public ChapterContentRespDto content(Long chapterId, Long userId) {
        BookChapter chapter = chapterMapper.selectById(chapterId);
        if (chapter == null || chapter.getAuditStatus() != CommonConstant.AUDIT_APPROVED) {
            throw new BizException(ErrorCode.CHAPTER_NOT_ONLINE);
        }
        Book book = bookMapper.selectById(chapter.getBookId());
        if (book == null || book.getAuditStatus() != CommonConstant.AUDIT_APPROVED || book.getStatus() == CommonConstant.BOOK_OFFLINE) {
            throw new BizException(ErrorCode.BOOK_NOT_EXIST);
        }

        // 浏览量先聚合，定时写回；不将余额或购买记录放进缓存。
        visits.record(book.getId());

        User reader=userId==null?null:userMapper.selectById(userId);
        boolean isVip = vip.isVip(reader);
        boolean exclusive = Integer.valueOf(CommonConstant.YES).equals(book.getIsVipLibrary());
        boolean locked;
        if (exclusive) {
            // 专属作品的所有章节都要求有效VIP，免费标记和历史购买记录不能绕过此门槛。
            locked = !isVip;
        } else if (isVip || chapter.getIsFree() == CommonConstant.YES
                || java.util.Objects.equals(userId, book.getAuthorId())) {
            // 普通作品：VIP全章畅读；非会员仍可读免费章，作者可阅读自己的普通作品。
            locked = false;
        } else {
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
        long originalPrice=chapter.getPrice()==null?book.getChapterPrice():chapter.getPrice();
        resp.setOriginalPrice(originalPrice);
        // 专属作品无单章售价；普通作品仅在确实需要购买时展示价格。
        resp.setPrice(exclusive || !locked ? 0L : originalPrice);
        resp.setVipRequired(exclusive && !isVip);
        resp.setCanPurchase(!exclusive && locked);
        resp.setDiscount(isVip ? 0 : CommonConstant.PERCENT_BASE);
        resp.setVipLibrary(exclusive);
        resp.setLocked(locked);
        resp.setIsVip(isVip);
        resp.setSort(chapter.getSort());
        return resp;
    }

}
