package com.xunye.dao;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xunye.entity.VipSubscriptionDetail;
import org.apache.ibatis.annotations.*;
public interface VipSubscriptionDetailMapper extends BaseMapper<VipSubscriptionDetail> {
    /**
     * 兼容旧折扣查询，不参与当前VIP阅读授权或扣款。
     * JOIN用subscription_id关联主记录；只选择start_time <= now且expire_time > now的生效时段。
     * 多条满足时按开始时间、ID倒序取一条；尚未开始的续费时段不会被提前选中。
     * #{userId}、#{now}由MyBatis绑定参数，@Param指定SQL中可使用的参数名称。
     */
    @Select("SELECT d.discount FROM vip_subscription_detail d JOIN vip_subscription s ON s.id=d.subscription_id WHERE s.user_id=#{userId} AND s.is_deleted=0 AND s.start_time<=#{now} AND s.expire_time>#{now} ORDER BY s.start_time DESC,s.id DESC LIMIT 1")
    Integer activeDiscount(@Param("userId")Long userId,@Param("now")java.time.LocalDateTime now);
}
