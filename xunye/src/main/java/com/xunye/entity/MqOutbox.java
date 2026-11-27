package com.xunye.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
/** 本地消息表：业务事务里写入，定时发布；消费状态与消费产生的数据在同一事务提交。 */
@Data @TableName("mq_outbox")
public class MqOutbox {
    @TableId(type=IdType.INPUT) private String id;
    private String eventKey;
    private String eventType;
    private Long userId;
    private Long refId;
    private LocalDateTime deliverAt;
    private Integer status;
    private Integer attempts;
    private LocalDateTime nextAttemptAt;
    private LocalDateTime consumedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
