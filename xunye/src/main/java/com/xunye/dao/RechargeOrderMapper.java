package com.xunye.dao;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xunye.entity.RechargeOrder;
import org.apache.ibatis.annotations.*;
public interface RechargeOrderMapper extends BaseMapper<RechargeOrder> {
    @Select("SELECT * FROM recharge_order WHERE id=#{id} FOR UPDATE")
    RechargeOrder lock(Long id);
}
