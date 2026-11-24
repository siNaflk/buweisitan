package com.xunye.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xunye.entity.ForumReply;

/**
 * 论坛回复表实体 Mapper
 */
public interface ForumReplyMapper extends BaseMapper<ForumReply> {
    @org.apache.ibatis.annotations.Select("SELECT * FROM forum_reply WHERE id=#{id} AND is_deleted=0 FOR UPDATE")
    ForumReply lock(Long id);

}
