package com.xunye.service;
import java.time.LocalDateTime;
/** 只负责订单关单与VIP通知，资金流水不经过MQ。 */
public interface BusinessEventService {
    void enqueue(String type,Long userId,Long refId,LocalDateTime deliverAt,String eventKey);
    void consume(String eventId);
}
