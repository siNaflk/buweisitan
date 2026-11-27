package com.xunye.dao;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xunye.entity.RechargeOrder;
import org.apache.ibatis.annotations.*;
public interface RechargeOrderMapper extends BaseMapper<RechargeOrder> {
    @Select("SELECT * FROM recharge_order WHERE id=#{id} FOR UPDATE")
    RechargeOrder lock(Long id);
    /** 只扫描缺少超时事件的订单，避免总是被最早20笔待支付订单占满补偿批次。 */
    @Select("SELECT r.* FROM recharge_order r WHERE r.status=0 AND NOT EXISTS(SELECT 1 FROM mq_outbox e WHERE e.event_key=CONCAT(#{prefix},r.id)) ORDER BY r.id LIMIT #{limit}")
    java.util.List<RechargeOrder> missingTimeoutEvents(@Param("prefix")String prefix,@Param("limit")int limit);
}
