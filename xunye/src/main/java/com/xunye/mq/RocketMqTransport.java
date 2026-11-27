package com.xunye.mq;
import com.xunye.constant.MqConstant;
import com.xunye.entity.MqOutbox;
import com.xunye.service.BusinessEventService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.*;
import org.apache.rocketmq.client.producer.*;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
/** 官方RocketMQ Java客户端。连接失败只阻止消息投递，不阻止充值/订阅事务提交；本地消息保留等待补偿。 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RocketMqTransport implements MessageTransport {
    private final BusinessEventService events;
    @Value("${xunye.mq.enabled:true}")
    private boolean enabled;
    @Value("${xunye.mq.nameserver:127.0.0.1:9876}")
    private String nameserver;
    private DefaultMQProducer producer;
    private DefaultMQPushConsumer consumer;
    @Override
    public synchronized void send(MqOutbox e) throws Exception {
        if(!enabled)throw new IllegalStateException("RocketMQ未启用，消息留在本地表");
        if(producer==null) {
            DefaultMQProducer p=new DefaultMQProducer(MqConstant.PRODUCER_GROUP);
            p.setNamesrvAddr(nameserver);
            p.setVipChannelEnabled(false);
            p.setInstanceName("m4-producer-"+ProcessHandle.current().pid());
            p.setSendMsgTimeout(3000);
            p.setRetryTimesWhenSendFailed(1);
            p.start();
            producer=p;
        }
        String topic=MqConstant.ORDER_TIMEOUT.equals(e.getEventType())?MqConstant.ORDER_TOPIC:MqConstant.VIP_TOPIC;
        Message m=new Message(topic,e.getEventType(),e.getId(),e.getId().getBytes(StandardCharsets.UTF_8));
        if(MqConstant.ORDER_TIMEOUT.equals(e.getEventType()) && e.getDeliverAt()!=null) {
            // 5.x定时消息：精确按订单到期时间投递，broker须启用timerWheelEnable。
            long due=e.getDeliverAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            m.setDeliverTimeMs(Math.max(due,System.currentTimeMillis()+1000));
        }
        SendResult result=producer.send(m);
        if(result.getSendStatus()!=SendStatus.SEND_OK)throw new IllegalStateException("Broker未确认持久化："+result.getSendStatus());
    }
    /** 后台连接消费者，MQ未启动时可重试，不能因为通知连接失败导致整个Web服务启动失败。 */
    @Scheduled(initialDelayString="${xunye.mq.connect-initial-ms:3000}",
            fixedDelayString="${xunye.mq.connect-retry-ms:10000}")
    public synchronized void connectConsumer() {
        if(!enabled || consumer!=null)return;
        DefaultMQPushConsumer c=new DefaultMQPushConsumer(MqConstant.CONSUMER_GROUP);
        try {
            c.setNamesrvAddr(nameserver);
            c.setVipChannelEnabled(false);
            c.setInstanceName("m4-consumer-"+ProcessHandle.current().pid());
            c.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
            c.setConsumeThreadMin(1);
            c.setConsumeThreadMax(2);
            c.setConsumeMessageBatchMaxSize(1);
            c.subscribe(MqConstant.ORDER_TOPIC,"*");
            c.subscribe(MqConstant.VIP_TOPIC,"*");
            c.registerMessageListener((MessageListenerConcurrently)(messages,context)->{
                try {
                    for(var m:messages)events.consume(new String(m.getBody(),StandardCharsets.UTF_8));
                    return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                }
                catch(Exception error){
                    log.warn("业务消息消费失败，交给MQ重试",error);
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                }
            });
            c.start();
            consumer=c;
        }
        catch(Exception error){
            c.shutdown();
            log.warn("RocketMQ消费者暂未连接，稍后自动重试：{}",error.getMessage());
        }
    }
    @PreDestroy public synchronized void close(){
        if(consumer!=null)consumer.shutdown();
        if(producer!=null)producer.shutdown();
    }
}
