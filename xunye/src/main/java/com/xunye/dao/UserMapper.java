package com.xunye.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xunye.entity.User;

/**
 * 用户表实体 Mapper
 */
public interface UserMapper extends BaseMapper<User> {
    /** 资金交易必须在事务内获取行锁；多账户按ID升序加锁，避免相互打赏时死锁。 */
    @org.apache.ibatis.annotations.Select("SELECT * FROM `user` WHERE id=#{id} AND is_deleted=0 FOR UPDATE")
    User lock(Long id);
}
