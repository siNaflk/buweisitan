package com.xunye.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xunye.dto.req.CommentReqDto;
import com.xunye.dto.req.ForumPostReqDto;
import com.xunye.dto.req.ForumReplyReqDto;
import com.xunye.dto.resp.CommentRespDto;
import com.xunye.dto.resp.ForumPostRespDto;
import com.xunye.dto.resp.ForumReplyRespDto;

/**
 * 互动服务接口：定义书评、章评、点赞、论坛帖子及回复业务。
 */
public interface InteractionService {

    /** 确认作品已通过审核且未下架；逻辑删除记录由MyBatis-Plus自动过滤。 */
    void checkBook(Long id);

    /**
     * 分页查询书评或章评。viewer是当前登录用户ID，游客为null。
     * 返回DTO附带liked（我是否点过赞）和mine（是否本人评论），供前端展示操作按钮。
     */
    Page<CommentRespDto> comments(Long bookId, Long chapterId, Long viewer, int page, int size);

    /** 发布书评/章评并返回新ID；评论写入与作品评论数加一在同一事务中完成。 */
    Long addComment(Long userId, CommentReqDto req);

    /** 删除本人评论；行锁串行化同一评论的删除/点赞，事务保证删除与计数同步。 */
    void deleteComment(Long userId,Long id);

    /**
     * add=true为点赞，false为取消点赞，返回最新点赞数。
     * 行锁保护计数，关系表唯一约束保护同一用户同一评论只能有一条点赞记录。
     */
    int like(Long userId,Long id,boolean add);

    /**
     * mine=false：社区广场只显示已审核通过、未封禁的帖子。
     * mine=true：只查当前用户本人帖子，保留待审/驳回/封帖状态供本人查看。
     */
    Page<ForumPostRespDto> posts(Long viewer,boolean mine,int page,int size);

    /** 帖子详情先做可见性校验，再返回完整正文。 */
    ForumPostRespDto post(Long id,Long viewer);

    /** 发布帖子；审核状态由服务端固定为0（待审），客户端不能自行指定“已通过”。 */
    Long addPost(Long userId,ForumPostReqDto req);

    /** 只允许作者逻辑删除自己的帖子；回复查询需先通过visiblePost，因此删除后也无法继续查看其回复。 */
    void deletePost(Long userId,Long id);

    /** 校验帖子可见后，返回已审核回复以及查看者自己的回复，后者可带待审/驳回状态。 */
    Page<ForumReplyRespDto> replies(Long postId,Long viewer,int page,int size);

    /** 只有已通过且未封帖的帖子可回复；新回复仍待审，不立即进入公开列表。 */
    Long addReply(Long userId,Long postId,ForumReplyReqDto req);

    /** 删除本人回复；查询条件再次带上userId，防止越权操作。 */
    void deleteReply(Long userId,Long id);
    /** 只允许对公开的帖子/回复点赞；重复点赞/取消保持幂等。 */
    long likePost(Long userId,Long id,boolean add);
    long likeReply(Long userId,Long id,boolean add);
}
