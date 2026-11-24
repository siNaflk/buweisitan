package com.xunye.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xunye.entity.ForumPost;

/**
 * 论坛帖子表实体（找书） Mapper
 */
public interface ForumPostMapper extends BaseMapper<ForumPost> {
    @org.apache.ibatis.annotations.Select("SELECT * FROM forum_post WHERE id=#{id} AND is_deleted=0 FOR UPDATE")
    ForumPost lock(Long id);

}
