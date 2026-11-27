package com.xunye.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xunye.constant.CommonConstant;
import com.xunye.dao.UserNotificationMapper;
import com.xunye.dto.resp.NotificationRespDto;
import com.xunye.entity.UserNotification;
import com.xunye.enums.ErrorCode;
import com.xunye.exception.BizException;
import com.xunye.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final UserNotificationMapper notices;
    @Override
    public Page<NotificationRespDto> list(Long userId,int page,int size) {
        Page<UserNotification> data=notices.selectPage(new Page<>(Math.max(1,page),Math.clamp(size,1,CommonConstant.MAX_PAGE_SIZE)),
        new LambdaQueryWrapper<UserNotification>().eq(UserNotification::getUserId,userId).orderByDesc(UserNotification::getId));
        Page<NotificationRespDto> out=new Page<>(data.getCurrent(),data.getSize(),data.getTotal());
        out.setRecords(data.getRecords().stream().map(n->new NotificationRespDto(n.getId(),n.getTitle(),n.getContent(),Integer.valueOf(CommonConstant.YES).equals(n.getIsRead()),n.getCreateTime())).toList());
        return out;
    }
    @Override
    public long unread(Long userId) {
        return notices.selectCount(new LambdaQueryWrapper<UserNotification>().eq(UserNotification::getUserId,userId).eq(UserNotification::getIsRead,CommonConstant.NORMAL));
    }
    @Override
    public void read(Long userId,Long id) {
        UserNotification n=notices.selectById(id);
        if(n==null || !userId.equals(n.getUserId()))throw new BizException(ErrorCode.FORBIDDEN);
        notices.update(null,new UpdateWrapper<UserNotification>().eq("id",id).eq("user_id",userId).set("is_read",CommonConstant.YES));
    }
}
