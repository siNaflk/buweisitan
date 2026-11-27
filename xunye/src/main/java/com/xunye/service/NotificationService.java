package com.xunye.service;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xunye.dto.resp.NotificationRespDto;
/** 本人通知查询/标记已读，不允许读取别人的消息。 */
public interface NotificationService {
    Page<NotificationRespDto> list(Long userId,int page,int size);
    long unread(Long userId);
    void read(Long userId,Long id);
}
