package com.xunye.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xunye.dao.*;
import com.xunye.dto.req.*;
import com.xunye.dto.resp.*;
import com.xunye.entity.*;
import com.xunye.enums.ErrorCode;
import com.xunye.exception.BizException;
import com.xunye.service.InteractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 用户身份只来自登录上下文；公开内容与本人待审内容严格区分。 */
// @Service：把该实现类交给Spring管理，Controller注入Service接口时可获得这个Bean。
@Service
// @RequiredArgsConstructor：Lombok为未初始化的final字段生成构造方法，Spring通过构造方法注入依赖。
@RequiredArgsConstructor
public class InteractionServiceImpl implements InteractionService {
    // Mapper负责数据库访问；Service负责组合查询、检查权限与维护事务。
    private final BookMapper books;
    private final BookChapterMapper chapters;
    private final CommentMapper comments;
    private final CommentLikeMapper likes;
    private final ForumPostMapper posts;
    private final ForumLikeMapper forumLikes;
    private final ForumReplyMapper replies;
    private final UserMapper users;

    /** 确认作品已通过审核且未下架；逻辑删除记录由MyBatis-Plus自动过滤。 */
    // @Override：标记这是Service接口方法的实现，编译器会检查签名；它本身不负责开启事务。
    @Override
    public void checkBook(Long id) {
        Book b=books.selectById(id);
        if(b==null || !Integer.valueOf(1).equals(b.getAuditStatus()) || Integer.valueOf(2).equals(b.getStatus()))
            throw new BizException(ErrorCode.BOOK_NOT_EXIST);
    }
    /** chapterId为空表示书评；非空时还必须验证章节属于这本书且已过审。 */
    private void checkChapter(Long bookId, Long chapterId) {
        if(chapterId==null)return;
        BookChapter c=chapters.selectById(chapterId);
        if(c==null || !bookId.equals(c.getBookId()) || !Integer.valueOf(1).equals(c.getAuditStatus()))
            throw new BizException(ErrorCode.CHAPTER_NOT_ONLINE);
    }

    /**
     * 分页查询书评或章评。viewer是当前登录用户ID，游客为null。
     * 返回DTO附带liked（我是否点过赞）和mine（是否本人评论），供前端展示操作按钮。
     */
    @Override
    public Page<CommentRespDto> comments(Long bookId, Long chapterId, Long viewer, int page, int size) {
        checkBook(bookId);
        checkChapter(bookId,chapterId);
        var q=new LambdaQueryWrapper<Comment>().eq(Comment::getBookId,bookId).eq(Comment::getStatus,0);
        // 书评只查chapter_id IS NULL；章评只查指定章节，避免两个列表混在一起。
        if(chapterId==null)q.isNull(Comment::getChapterId);
        else q.eq(Comment::getChapterId,chapterId);
        // 先按时间倒序，再按ID倒序，让同一时间的记录也有稳定顺序。
        Page<Comment> data=comments.selectPage(page(page,size),q.orderByDesc(Comment::getCreateTime).orderByDesc(Comment::getId));
        // 一次批量查询这一页评论的作者信息，避免每条评论都单独查一次用户表。
        Map<Long,User> authors=authors(data.getRecords().stream().map(Comment::getUserId).toList());
        // 游客没有点赞状态；登录用户用Set快速判断每条评论是否点过赞。
        Set<Long> liked=viewer==null ? Set.of() : new HashSet<>(likes.likedIds(viewer));
        // map把数据库Comment实体转为响应DTO；不将User实体或密码等敏感字段返回。
        return mapped(data,data.getRecords().stream().map(c->{
            User u=authors.get(c.getUserId());
            return new CommentRespDto(c.getId(),c.getUserId(),name(u),u==null?null:u.getAvatar(),c.getContent(),
                c.getLikeCount(),liked.contains(c.getId()),Objects.equals(viewer,c.getUserId()),c.getCreateTime());
        }).toList());
    }

    // @Transactional：通过Spring事务代理调用时启用事务；rollbackFor=Exception.class包含受检异常回滚。
    @Transactional(rollbackFor=Exception.class)
    /** 发布书评/章评并返回新ID；评论写入与作品评论数加一在同一事务中完成。 */
    @Override
    public Long addComment(Long userId, CommentReqDto req) {
        checkBook(req.bookId());
        checkChapter(req.bookId(),req.chapterId());
        Comment c=new Comment();
        c.setUserId(userId);c.setBookId(req.bookId());c.setChapterId(req.chapterId());
        // strip去掉首尾空白；新评论正常可见、初始点赞为0，发布人由后端登录上下文决定。
        c.setContent(req.content().strip());c.setStatus(0);c.setLikeCount(0);c.setIsDeleted(0);
        comments.insert(c);
        // 使用数据库表达式原子加一，避免先查再写在并发下丢失计数。
        books.update(null,new UpdateWrapper<Book>().eq("id",req.bookId()).setSql("comment_count=comment_count+1"));
        return c.getId();
    }
    // @Transactional：通过Spring事务代理调用时启用事务；rollbackFor=Exception.class包含受检异常回滚。
    @Transactional(rollbackFor=Exception.class)
    /** 删除本人评论；行锁串行化同一评论的删除/点赞，事务保证删除与计数同步。 */
    @Override
    public void deleteComment(Long userId,Long id) {
        Comment c=comments.lock(id);
        if(c==null)throw new BizException(ErrorCode.PARAM_ERROR,"评论不存在");
        owner(userId,c.getUserId());
        // 实体配置了@TableLogic，因此这里是逻辑删除；GREATEST保证评论数不会减成负数。
        comments.deleteById(id);
        books.update(null,new UpdateWrapper<Book>().eq("id",c.getBookId()).setSql("comment_count=GREATEST(comment_count-1,0)"));
    }

    // @Transactional：通过Spring事务代理调用时启用事务；rollbackFor=Exception.class包含受检异常回滚。
    @Transactional(rollbackFor=Exception.class)
    /**
     * add=true为点赞，false为取消点赞，返回最新点赞数。
     * 行锁保护计数，关系表唯一约束保护同一用户同一评论只能有一条点赞记录。
     */
    @Override
    public int like(Long userId,Long id,boolean add) {
        Comment c=comments.lock(id);
        if(c==null || !Integer.valueOf(0).equals(c.getStatus()))throw new BizException(ErrorCode.PARAM_ERROR,"评论不存在或已隐藏");
        checkBook(c.getBookId());
        // 返回影响行数：重复点赞/重复取消为0，只有关系真正变化时才修改点赞数。
        int changed=add?likes.add(userId,id):likes.remove(userId,id);
        int count=Math.max(0,c.getLikeCount()+(changed>0?(add?1:-1):0));
        if(changed>0)comments.update(null,new UpdateWrapper<Comment>().eq("id",id).set("like_count",count));
        return count;
    }
    /**
     * mine=false：社区广场只显示已审核通过、未封禁的帖子。
     * mine=true：只查当前用户本人帖子，保留待审/驳回/封帖状态供本人查看。
     */
    @Override
    public Page<ForumPostRespDto> posts(Long viewer,boolean mine,int page,int size) {
        var q=new LambdaQueryWrapper<ForumPost>();
        if(mine) {
            if(viewer==null)throw new BizException(ErrorCode.UNAUTHORIZED);
            q.eq(ForumPost::getUserId,viewer);
        }
        else q.eq(ForumPost::getAuditStatus,1).eq(ForumPost::getStatus,0);
        Page<ForumPost> data=posts.selectPage(page(page,size),q.orderByDesc(ForumPost::getCreateTime).orderByDesc(ForumPost::getId));
        var authors=authors(data.getRecords().stream().map(ForumPost::getUserId).toList());
        return mapped(data,data.getRecords().stream().map(
                p->postDto(p,viewer,authors.get(p.getUserId()),false)).toList());
    }

    /** 帖子详情先做可见性校验，再返回完整正文。 */
    @Override
    public ForumPostRespDto post(Long id,Long viewer) {
        ForumPost p=visiblePost(id,viewer);
        return postDto(p,viewer,users.selectById(p.getUserId()),true);
    }

    /**
     * 公共可见性校验：作者可查看自己的非公开帖子，其他人只能看已通过且未封帖的内容。
     * “作者能查看”不等于“帖子能回复”，写回复时还要单独检查审核和封帖状态。
     */
    private ForumPost visiblePost(Long id,Long viewer) {
        ForumPost p=posts.selectById(id);
        if(p==null || (!Objects.equals(viewer,p.getUserId()) &&
            (!Integer.valueOf(1).equals(p.getAuditStatus()) || !Integer.valueOf(0).equals(p.getStatus()))))
            throw new BizException(ErrorCode.CONTENT_NOT_ONLINE,"帖子不存在或暂未公开");
        return p;
    }

    /** 发布帖子；审核状态由服务端固定为0（待审），客户端不能自行指定“已通过”。 */
    @Override
    public Long addPost(Long userId,ForumPostReqDto req) {
        ForumPost p=new ForumPost();
        p.setUserId(userId);p.setTitle(req.title().strip());p.setContent(req.content().strip());
        p.setAuditStatus(0);p.setStatus(0);p.setReplyCount(0);p.setIsDeleted(0);
        posts.insert(p);
        return p.getId();
    }

    // @Transactional：通过Spring事务代理调用时启用事务；rollbackFor=Exception.class包含受检异常回滚。
    @Transactional(rollbackFor=Exception.class)
    @Override
    public void deletePost(Long userId,Long id) {
        ForumPost p=posts.lock(id);
        if(p==null)throw new BizException(ErrorCode.PARAM_ERROR,"帖子不存在");
        owner(userId,p.getUserId());
        posts.deleteById(id);
    }

    /** 校验帖子可见后，返回已审核回复以及查看者自己的回复，后者可带待审/驳回状态。 */
    @Override
    public Page<ForumReplyRespDto> replies(Long postId,Long viewer,int page,int size) {
        visiblePost(postId,viewer);
        var q=new LambdaQueryWrapper<ForumReply>().eq(ForumReply::getPostId,postId);
        // 括号至关重要：post_id=? AND (audit_status=1 OR user_id=当前用户)。
        // 不能在外层直接OR，否则可能把其他帖子中该用户的回复也查出来。
        q.and(w->{w.eq(ForumReply::getAuditStatus,1);
            if(viewer!=null)w.or().eq(ForumReply::getUserId,viewer);});
        Page<ForumReply> data=replies.selectPage(page(page,size),
                q.orderByAsc(ForumReply::getCreateTime).orderByAsc(ForumReply::getId));
        var authors=authors(data.getRecords().stream().map(ForumReply::getUserId).toList());
        return mapped(data,data.getRecords().stream().map(r->new ForumReplyRespDto(r.getId(),r.getUserId(),
            name(authors.get(r.getUserId())),r.getContent(),r.getAuditStatus(),forumLikes.count(2,r.getId()),viewer!=null && forumLikes.liked(viewer,2,r.getId())>0,Objects.equals(viewer,r.getUserId()),r.getCreateTime())).toList());
    }
    /** 只有已通过且未封帖的帖子可回复；新回复仍待审，不立即进入公开列表。 */
    @Override
    public Long addReply(Long userId,Long postId,ForumReplyReqDto req) {
        ForumPost p=visiblePost(postId,userId);
        if(!Integer.valueOf(1).equals(p.getAuditStatus()) || !Integer.valueOf(0).equals(p.getStatus()))
            throw new BizException(ErrorCode.CONTENT_NOT_ONLINE,"帖子待审或已封帖，暂不能回复");
        ForumReply r=new ForumReply();
        r.setPostId(postId);r.setUserId(userId);r.setContent(req.content().strip());r.setAuditStatus(0);r.setIsDeleted(0);
        replies.insert(r);
        return r.getId();
    }
    // @Transactional：通过Spring事务代理调用时启用事务；rollbackFor=Exception.class包含受检异常回滚。
    @Transactional(rollbackFor=Exception.class)
    /** 删除本人回复；查询条件再次带上userId，防止越权操作。 */
    @Override
    public void deleteReply(Long userId,Long id) {
        ForumReply r=replies.lock(id);
        if(r==null)throw new BizException(ErrorCode.PARAM_ERROR,"回复不存在");
        owner(userId,r.getUserId());
        replies.delete(new LambdaQueryWrapper<ForumReply>().eq(ForumReply::getId,id).eq(ForumReply::getUserId,userId));
    }

    /**
     * 组装帖子DTO。列表截取120字符摘要，详情返回完整正文。
     * 回复数统计已通过且未逻辑删除的记录，与公开可见范围一致。
     */
    private ForumPostRespDto postDto(ForumPost p,Long viewer,User u,boolean full) {
        // 回复数直读已审核记录，避免把待审核回复计入公开数字；M6审核时可维护冗余字段。
        Long count=replies.selectCount(new LambdaQueryWrapper<ForumReply>().eq(ForumReply::getPostId,p.getId()).eq(ForumReply::getAuditStatus,1));
        String content=p.getContent();if(!full && content.length()>120)content=content.substring(0,120)+"…";
        return new ForumPostRespDto(p.getId(),p.getUserId(),name(u),p.getTitle(),content,count,forumLikes.count(1,p.getId()),viewer!=null && forumLikes.liked(viewer,1,p.getId())>0,
            p.getAuditStatus(),p.getStatus(),Objects.equals(viewer,p.getUserId()),p.getCreateTime());
    }

    @Override
    // @Transactional：通过Spring事务代理调用时启用事务；rollbackFor=Exception.class包含受检异常回滚。
    @Transactional(rollbackFor=Exception.class)
    public long likePost(Long userId,Long id,boolean add) {
        ForumPost p=posts.lock(id);
        requirePublic(p);
        return changeForumLike(userId,1,id,add);
    }
    @Override
    // @Transactional：通过Spring事务代理调用时启用事务；rollbackFor=Exception.class包含受检异常回滚。
    // READ_COMMITTED（读已提交）：每次查询可以看到其他事务已经提交的数据，避免等待行锁后仍使用旧快照。
    @Transactional(rollbackFor=Exception.class,isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public long likeReply(Long userId,Long id,boolean add) {
        // 统一先锁父帖子，再锁回复，与删除/点赞的可见性检查保持一致。
        ForumReply preview=replies.selectById(id);
        if(preview==null)throw new BizException(ErrorCode.CONTENT_NOT_ONLINE,"回复不存在");
        requirePublic(posts.lock(preview.getPostId()));
        ForumReply r=replies.lock(id);
        if(r==null || !Integer.valueOf(1).equals(r.getAuditStatus()))
            throw new BizException(ErrorCode.CONTENT_NOT_ONLINE,"回复暂未公开");
        return changeForumLike(userId,2,id,add);
    }
    private void requirePublic(ForumPost p) {
        if(p==null || !Integer.valueOf(1).equals(p.getAuditStatus()) || !Integer.valueOf(0).equals(p.getStatus()))
            throw new BizException(ErrorCode.CONTENT_NOT_ONLINE,"帖子暂未公开，不能点赞");
    }
    private long changeForumLike(Long userId,int type,Long id,boolean add) {
        if(userId==null)throw new BizException(ErrorCode.UNAUTHORIZED);
        if(add)forumLikes.add(userId,type,id);else forumLikes.remove(userId,type,id);
        // 直接统计关系表，不新增冗余计数字段，重复请求和取消都不会导致计数漂移。
        return forumLikes.count(type,id);
    }

    /** 作者ID去重后批量查询，转成 userId→User 映射，便于组装每条评论/回复。 */
    private Map<Long,User> authors(List<Long> ids) {
        return ids.isEmpty()?Map.of():users.selectBatchIds(new HashSet<>(ids)).stream().collect(Collectors.toMap(User::getId,Function.identity()));
    }
    /** 用户已逻辑删除时查不到记录，使用占位昵称，避免空指针。 */
    private String name(User u){return u==null?"已注销书友":u.getNickname();}
    /** 资源归属校验：当前用户必须与资源作者一致；前端隐藏按钮不能替代这一步。 */
    private void owner(Long viewer,Long owner){if(!Objects.equals(viewer,owner))throw new BizException(ErrorCode.FORBIDDEN);}
    /** 对分页参数兜底：页码至少1，每页1～50条，限制单次查询量。 */
    private <T> Page<T> page(int p,int s){return new Page<>(Math.max(1,p),Math.min(50,Math.max(1,s)));}

    /** 转换分页记录类型时保留页码、每页数量和总条数；只替换records内容。 */
    private <A,T> Page<T> mapped(Page<A> source,List<T> records){
        Page<T> out=new Page<>(source.getCurrent(),source.getSize(),source.getTotal());
        out.setRecords(records);
        return out;
    }
}
