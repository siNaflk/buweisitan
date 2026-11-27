package com.xunye.controller.app;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xunye.common.*;
import com.xunye.dto.req.VipSubscribeReqDto;
import com.xunye.dto.resp.*;
import com.xunye.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
/**
 * App会员功能的HTTP入口，负责接收参数、调用Service、包装响应。
 * <p>调用流程：前端请求 → 登录拦截器（需要登录的接口）→ 本Controller → Service → 数据库。
 * 套餐和会员专属列表允许游客查看；会员状态、订阅和历史记录要求登录（见WebConfig）。
 * 当前用户ID来自拦截器设置的UserContext，不能让前端传userId决定给谁开通会员。
 * <p>VipService负责查询和权益判断；WalletService负责涉及余额扣款的订阅交易。
 */
// @RestController：注册控制器，并把方法返回的Result对象转换为JSON响应。
@RestController
// 类上的路径前缀会与方法路径拼接，例如GET /api/app/vip/plans。
@RequestMapping("/api/app/vip")
// 为final依赖生成构造方法，由Spring注入对应的Service实现类，无需自己new。
@RequiredArgsConstructor
public class VipController {

    private final VipService vip;
    private final WalletService wallet;

    /**
     * 获取当前上架套餐，游客也能调用。只展示套餐，不会扣款。
     * 价格、天数来自数据库；所有套餐均为畅读权益，旧discount字段仅为兼容保留。
     */
    @GetMapping("/plans")
    public Result<List<VipPlanRespDto>> plans(){
        return Result.ok(vip.plans());
    }
    /**
     * 获取本人的会员状态、到期时间和余额。
     * UserContext.getUserId()读取本次请求已验证的登录身份；过期判断在Service中实时计算。
     */
    @GetMapping("/status")
    public Result<VipStatusRespDto> status(){
        return Result.ok(vip.status(UserContext.getUserId()));
    }
    /**
     * 使用余额开通或续费VIP，POST /api/app/vip/subscribe/{id}。
     * @PathVariable把路径中的id绑定为套餐ID；@RequestBody把JSON转换为请求DTO。
     * @Valid触发DTO字段校验，例如requestId不能为空、expectedPrice必须为正数。
     * requestId用于识别同一次提交的重试；expectedPrice只核对用户确认价格，实际价格取数据库。
     * Controller不直接扣款：锁账户、检查重试、扣余额、延长到期时间均由WalletService统一完成。
     */
    @PostMapping("/subscribe/{id}")
    public Result<VipSubscriptionRespDto> subscribe(@PathVariable Long id,@Valid @RequestBody VipSubscribeReqDto req){
        return Result.ok(wallet.subscribe(UserContext.getUserId(),id,req));
    }

    /**
     * 分页查看本人的历史订阅，不返回其他用户记录。
     * @RequestParam读取查询参数；未传时默认第1页、每页20条，Service还会限制分页范围。
     * 返回Page包含records（本页记录）、total（总条数）等信息，供前端实现加载更多。
     */
    @GetMapping("/subscriptions")
    public Result<Page<VipSubscriptionRespDto>> subscriptions(@RequestParam(defaultValue="1")int pageNum,@RequestParam(defaultValue="20")int pageSize){
        return Result.ok(vip.subscriptions(UserContext.getUserId(),pageNum,pageSize));
    }
    /**
     * 公开展示会员专属作品列表，游客能看书名、封面等简介。
     * 能浏览列表不代表能读正文：ChapterServiceImpl会再次检查有效VIP，专属作品的免费章也不例外。
     */
    @GetMapping("/library")
    public Result<Page<BookBriefRespDto>> library(@RequestParam(defaultValue="1")int pageNum,@RequestParam(defaultValue="20")int pageSize){
        return Result.ok(vip.library(pageNum,pageSize));
    }
}
