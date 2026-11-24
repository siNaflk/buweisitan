package com.xunye.dao;
import org.apache.ibatis.annotations.*;
import java.util.List;
/**
 * 评论点赞关系访问层。表中(user_id,comment_id)唯一，取消点赞物理删除关系。
 * 关系变化与comment.like_count更新在InteractionService同一事务中完成。
 */
public interface CommentLikeMapper {
    /** 重复键由INSERT IGNORE忽略；返回1表示新增，0表示已经点过赞。 */
    @Insert("INSERT IGNORE INTO comment_like(user_id,comment_id) VALUES(#{userId},#{commentId})")
    int add(@Param("userId") Long userId,@Param("commentId") Long commentId);
    /** 返回删除行数；不存在时返回0，用于确保重复取消不会多减计数。 */
    @Delete("DELETE FROM comment_like WHERE user_id=#{userId} AND comment_id=#{commentId}")
    int remove(@Param("userId") Long userId,@Param("commentId") Long commentId);
    /** 查询用户点赞过的评论ID，Service转为Set后用于组装每条评论的liked字段。 */
    @Select("SELECT comment_id FROM comment_like WHERE user_id=#{userId}")
    List<Long> likedIds(Long userId);
}
