package com.xunye.dao;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xunye.entity.MqOutbox;
import org.apache.ibatis.annotations.*;
public interface MqOutboxMapper extends BaseMapper<MqOutbox> {
    @Select("SELECT * FROM mq_outbox WHERE id=#{id} FOR UPDATE")
    MqOutbox lock(String id);
    /** event_key唯一：重启补偿/定时提醒重复扫描，不会创建多条同一业务事件。 */
    @Insert("INSERT IGNORE INTO mq_outbox(id,event_key,event_type,user_id,ref_id,deliver_at,status,attempts,next_attempt_at,create_time,update_time) VALUES(#{id},#{eventKey},#{eventType},#{userId},#{refId},#{deliverAt},0,0,#{nextAttemptAt},#{createTime},#{updateTime})")
    int addOnce(MqOutbox event);
}
