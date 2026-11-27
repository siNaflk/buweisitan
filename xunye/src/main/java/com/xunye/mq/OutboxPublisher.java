package com.xunye.mq;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.xunye.constant.MqConstant;
import com.xunye.dao.MqOutboxMapper;
import com.xunye.entity.MqOutbox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDateTime;
/** 本地消息重试投递：Broker确认后标记SENT；确认后进程崩溃可能重复发送，消费端通过本地消息行锁去重。 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {
    private final MqOutboxMapper outbox;
    private final MessageTransport transport;
    private final TransactionTemplate transactions;
    @Value("${xunye.mq.enabled:true}")
    private boolean enabled;
    @Scheduled(initialDelayString="${xunye.mq.publish-initial-ms:4000}",
            fixedDelayString="${xunye.mq.publish-interval-ms:2000}")
    public void publish() {
        if(!enabled)return;
        LocalDateTime now=LocalDateTime.now(),threshold=now.minusSeconds(MqConstant.RETRY_MAX_SECONDS);
        // sent却长期没消费时重新投递，覆盖消费者长时间停机、消息死信等场景；已消费记录永不重新发送。
        outbox.update(null, new UpdateWrapper<MqOutbox>().eq("status",MqConstant.SENT).isNull("consumed_at")
        .le("update_time",threshold).and(
                w->w.ne("event_type",MqConstant.ORDER_TIMEOUT).
                        or().isNull("deliver_at").or().le("deliver_at",threshold))
        .set("status",MqConstant.PENDING).set("next_attempt_at",now));
        for(MqOutbox candidate:outbox.selectList(new LambdaQueryWrapper<MqOutbox>().eq(MqOutbox::getStatus,MqConstant.PENDING)
        .le(MqOutbox::getNextAttemptAt,now).orderByAsc(MqOutbox::getCreateTime).last("LIMIT "+MqConstant.BATCH_SIZE))) {
            transactions.executeWithoutResult(tx->{
                MqOutbox e=outbox.lock(candidate.getId());
                if(e==null || e.getStatus()!=MqConstant.PENDING || e.getConsumedAt()!=null || e.getNextAttemptAt().isAfter(LocalDateTime.now()))return;
                try {
                    transport.send(e);
                    e.setStatus(MqConstant.SENT);
                }
                catch(Exception error){
                    e.setAttempts(e.getAttempts()+1);
                    long retry=Math.min(MqConstant.RETRY_MAX_SECONDS,1L<<Math.min(e.getAttempts(),8));
                    e.setNextAttemptAt(LocalDateTime.now().plusSeconds(retry));
                    log.warn("消息{}投递失败，第{}次，{}秒后重试：{}",e.getId(),e.getAttempts(),retry,error.getMessage());
                }
                e.setUpdateTime(LocalDateTime.now());
                outbox.updateById(e);
            });
        }
    }
}
