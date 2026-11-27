package com.xunye.mq;
import com.xunye.entity.MqOutbox;
/** 传输接口方便测试模拟发送失败；真正业务仍在Service接口/实现中处理。 */
public interface MessageTransport {
    void send(MqOutbox event) throws Exception;
}
