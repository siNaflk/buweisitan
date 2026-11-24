package com.xunye.dao;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xunye.entity.Comment;
import org.apache.ibatis.annotations.Select;
/** 评论基础增删改查继承BaseMapper；只为并发修改额外提供一条行锁查询。 */
public interface CommentMapper extends BaseMapper<Comment> {
    /**
     * SELECT FOR UPDATE锁住评论，须在@Transactional事务内调用，锁在事务结束时释放。
     * 自定义SQL显式过滤is_deleted，避免锁到已经逻辑删除的记录。
     */
    @Select("SELECT * FROM comment WHERE id=#{id} AND is_deleted=0 FOR UPDATE")
    Comment lock(Long id);
}
