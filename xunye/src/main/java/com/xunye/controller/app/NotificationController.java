package com.xunye.controller.app;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xunye.common.*;
import com.xunye.dto.resp.NotificationRespDto;
import com.xunye.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
/** 本人消息接口：WebConfig统一强制登录，Service再次核对消息归属。 */
@RestController
@RequestMapping("/api/app/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notices;
    /** 分页查询本人消息，不能由客户端指定接收人。 */
    @GetMapping
    public Result<Page<NotificationRespDto>> list(@RequestParam(defaultValue="1")int pageNum,@RequestParam(defaultValue="20")int pageSize){
        return Result.ok(notices.list(UserContext.getUserId(),pageNum,pageSize));
    }
    /** 统计本人未读消息数量。 */
    @GetMapping("/unread-count")
    public Result<Long> unread(){
        return Result.ok(notices.unread(UserContext.getUserId()));
    }
    /** 标记本人消息已读；重复标记是安全的，其他人的消息会被拒绝。 */
    @PostMapping("/{id}/read")
    public Result<Void> read(@PathVariable Long id){
        notices.read(UserContext.getUserId(),id);
        return Result.ok();
    }
}
