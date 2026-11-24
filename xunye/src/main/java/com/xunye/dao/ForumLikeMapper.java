package com.xunye.dao;
import org.apache.ibatis.annotations.*;
/** 同一用户对同一帖子/回复最多一条点赞关系，取消时物理删除。 */
public interface ForumLikeMapper {
    @Insert("INSERT IGNORE INTO forum_like(user_id,target_type,target_id) VALUES(#{userId},#{type},#{id})")
    int add(@Param("userId")Long userId,@Param("type")int type,@Param("id")Long id);
    @Delete("DELETE FROM forum_like WHERE user_id=#{userId} AND target_type=#{type} AND target_id=#{id}")
    int remove(@Param("userId")Long userId,@Param("type")int type,@Param("id")Long id);
    @Select("SELECT COUNT(*) FROM forum_like WHERE target_type=#{type} AND target_id=#{id}")
    long count(@Param("type")int type,@Param("id")Long id);
    @Select("SELECT COUNT(*) FROM forum_like WHERE user_id=#{userId} AND target_type=#{type} AND target_id=#{id}")
    int liked(@Param("userId")Long userId,@Param("type")int type,@Param("id")Long id);
}
