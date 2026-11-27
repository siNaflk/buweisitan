package com.xunye.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.xunye.constant.*;
import com.xunye.dao.*;
import com.xunye.entity.*;
import com.xunye.service.BusinessEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDateTime;
import java.util.UUID;
/** 本地消息与业务同事务保存；消费在行锁保护下更新业务和consumedAt，数据库提交前绝不确认消息。 */
@Service
@RequiredArgsConstructor
public class BusinessEventServiceImpl implements BusinessEventService {
    private final MqOutboxMapper outbox;
    private final RechargeOrderMapper orders;
    private final VipSubscriptionMapper subscriptions;
    private final UserMapper users;
    private final UserNotificationMapper notices;
    private final TransactionTemplate transactions;
    @org.springframework.beans.factory.annotation.Value("${xunye.mq.enabled:true}") private boolean enabled;
    /** MANDATORY要求调用者已有事务，避免出现业务回滚却留下通知事件。 */
    @Override
    @Transactional(propagation=Propagation.MANDATORY)
    public void enqueue(String type,Long userId,Long refId,LocalDateTime deliverAt,String eventKey) {
        MqOutbox e=new MqOutbox();
        LocalDateTime now=LocalDateTime.now();
        e.setId(UUID.randomUUID().toString().replace("-",""));
        e.setEventKey(eventKey);
        e.setEventType(type);
        e.setUserId(userId);
        e.setRefId(refId);
        e.setDeliverAt(deliverAt);
        e.setNextAttemptAt(now);
        e.setCreateTime(now);
        e.setUpdateTime(now);
        outbox.addOnce(e);
    }
    /** eventId只用于找到服务端已保存的消息，绝不信任MQ载荷提供的用户、金额或订单状态。 */
    @Override
    @Transactional(rollbackFor=Exception.class)
    public void consume(String eventId) {
        MqOutbox e=outbox.lock(eventId);
        if(e==null || e.getConsumedAt()!=null)return;
        LocalDateTime now=LocalDateTime.now();
        switch(e.getEventType()) {
            case MqConstant.ORDER_TIMEOUT -> {
                RechargeOrder order=orders.lock(e.getRefId());
                if(order!=null && Integer.valueOf(WalletConstant.ORDER_PENDING).equals(order.getStatus())) {
                    // broker定时精度或系统时钟差导致提前投递时，重新排队，不能提前关闭。
                    if(order.getExpiresAt().isAfter(now)) {
                        e.setStatus(MqConstant.PENDING);
                        e.setDeliverAt(order.getExpiresAt());
                        e.setNextAttemptAt(now.plusSeconds(1));
                        e.setUpdateTime(now);
                        outbox.updateById(e);
                        return;
                    }
                    order.setStatus(WalletConstant.ORDER_CLOSED);
                    orders.updateById(order);
                }
            }
            case MqConstant.VIP_OPENED -> {
                // 订阅与扣款已在前一个事务完成；这里仅生成站内成功通知，不再开通会员或扣款。
                // 外层已锁定outbox并检查consumedAt，通知与消费完成标记同事务提交，MQ重投不会重复通知。
                VipSubscription s=subscriptions.selectById(e.getRefId());
                if(s!=null)notice(e,"VIP订阅成功","会员权益已确认。本次订阅区间："+s.getStartTime()+" 至 "+s.getExpireTime()+"。VIP有效期内可畅读普通作品全部章节及会员专属作品。");
            }
            case MqConstant.VIP_EXPIRING -> {
                // 到期提醒事件的deliverAt保存“扫描时的会员到期时间”，用于识别提醒是否过时。
                // 它不是在这里等到过期才提醒：扫描到提前提醒范围内的用户后，就会发送该事件。
                User user=users.lock(e.getUserId());
                // 用户已续费或已过期时丢弃旧提醒，不能向新有效期重复发送过时通知。
                if(user!=null && user.getVipExpireTime()!=null && user.getVipExpireTime().isAfter(now)
                && user.getVipExpireTime().equals(e.getDeliverAt()))
                notice(e,"VIP即将到期","你的VIP将于 "+user.getVipExpireTime()+" 到期，续费可保留剩余时长。");
            }
            default -> throw new IllegalArgumentException("不支持的业务事件："+e.getEventType());
        }
        e.setStatus(MqConstant.CONSUMED);
        e.setConsumedAt(now);
        e.setUpdateTime(now);
        outbox.updateById(e);
    }
    private void notice(MqOutbox e,String title,String content) {
        UserNotification n=new UserNotification();
        n.setEventId(e.getId());
        n.setUserId(e.getUserId());
        n.setTitle(title);
        n.setContent(content);
        n.setIsRead(CommonConstant.NORMAL);
        notices.insert(n);
    }
    /** 每小时扫描提前3天内到期的会员。事件唯一键包含到期时间，同一有效期只提醒一次。 */
    @Scheduled(initialDelayString="${xunye.mq.reminder-initial-ms:60000}",fixedDelayString="${xunye.mq.reminder-scan-ms:3600000}")
    public void remindExpiring() {
        if(!enabled)return;
        LocalDateTime now=LocalDateTime.now();
        // @Scheduled的fixedDelay表示本轮结束后再等指定毫秒数；不是处理每个用户时单独等待。
        // gt排除已过期会员，le只选未来REMIND_AHEAD_DAYS天内到期的正常账户。
        for(User u:users.selectList(new LambdaQueryWrapper<User>().eq(User::getStatus,CommonConstant.NORMAL)
        .gt(User::getVipExpireTime,now).le(User::getVipExpireTime,now.plusDays(VipConstant.REMIND_AHEAD_DAYS)))) {
            // 每个用户单独一个事务；enqueue的MANDATORY要求此时已有事务。
            // 唯一键含用户ID和到期时间：每小时重复扫描同一有效期不会新增事件，续费后则属于新的有效期。
            transactions.executeWithoutResult(tx->enqueue(MqConstant.VIP_EXPIRING,u.getId(),u.getId(),u.getVipExpireTime(),MqConstant.VIP_EXPIRING+":"+u.getId()+":"+u.getVipExpireTime()));
        }
    }
    /** 兼容M3已创建的待支付订单；重启后也能补充超时消息，唯一键保证扫描重复不会新增事件。 */
    @Scheduled(initialDelayString="${xunye.mq.recovery-initial-ms:15000}",fixedDelayString="${xunye.mq.recovery-scan-ms:60000}")
    public void recoverOrders() {
        if(!enabled)return;
        // 只补近期仍待支付且缺少事件的订单，查询上限避免单轮长时间占用线程。
        for(RechargeOrder o:orders.missingTimeoutEvents(MqConstant.ORDER_TIMEOUT+":",MqConstant.BATCH_SIZE)) {
            transactions.executeWithoutResult(tx->enqueue(MqConstant.ORDER_TIMEOUT,o.getUserId(),o.getId(),o.getExpiresAt(),MqConstant.ORDER_TIMEOUT+":"+o.getId()));
        }
    }
}
