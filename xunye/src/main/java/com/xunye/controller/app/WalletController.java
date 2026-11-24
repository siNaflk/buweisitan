package com.xunye.controller.app;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xunye.common.*;
import com.xunye.dto.req.*;
import com.xunye.dto.resp.*;
import com.xunye.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
/** 全部交易接口由WebConfig强制登录；不能由客户端指定交易用户或作者。 */
// @RestController：注册为Spring控制器，方法返回值由框架序列化成JSON响应。
@RestController
// @RequestMapping：设置这个控制器的公共路径前缀；完整接口路径还要拼接方法上的映射。
@RequestMapping("/api/app")
// @RequiredArgsConstructor：Lombok为未初始化的final字段生成构造方法，Spring通过构造方法注入依赖。
@RequiredArgsConstructor
public class WalletController {

    private final WalletService wallet;

    // 接口：GET /api/app/wallet；由Spring根据HTTP方法和路径匹配到下方方法。
    @GetMapping("/wallet")
    public Result<WalletRespDto> wallet(){
        return Result.ok(wallet.wallet(UserContext.getUserId()));
    }

    // 接口：GET /api/app/wallet/plans；由Spring根据HTTP方法和路径匹配到下方方法。
    @GetMapping("/wallet/plans")
    public Result<List<RechargePlanRespDto>> plans(){
        return Result.ok(wallet.plans());
    }

    // 接口：GET /api/app/wallet/flows；由Spring根据HTTP方法和路径匹配到下方方法。
    // @RequestParam：读取URL查询参数；required=false允许省略，defaultValue提供缺省值。
    @GetMapping("/wallet/flows")
    public Result<Page<BalanceFlowRespDto>> flows(@RequestParam(defaultValue="1") int pageNum,@RequestParam(defaultValue="20") int pageSize){
        return Result.ok(wallet.flows(UserContext.getUserId(),pageNum,pageSize));
    }

    // 接口：POST /api/app/wallet/recharge；由Spring根据HTTP方法和路径匹配到下方方法。
    // @RequestBody：将请求体JSON转换为DTO；@Valid触发DTO上的非空、长度、金额范围等校验。
    @PostMapping("/wallet/recharge")
    public Result<RechargeOrderRespDto> recharge(@Valid @RequestBody RechargeReqDto req){
        return Result.ok(wallet.recharge(UserContext.getUserId(),req));
    }

    // 接口：GET /api/app/wallet/recharge/{id}；由Spring根据HTTP方法和路径匹配到下方方法。
    // @PathVariable：读取URL路径中的资源ID，例如{id}；它不是当前登录用户ID。
    @GetMapping("/wallet/recharge/{id}")
    public Result<RechargeOrderRespDto> order(@PathVariable Long id){
        return Result.ok(wallet.order(UserContext.getUserId(),id));
    }
    /** 比赛演示入口，明确标注模拟支付，不接收客户端提供的支付金额。 */
    // 接口：POST /api/app/wallet/recharge/{id}/simulate-payment；由Spring根据HTTP方法和路径匹配到下方方法。
    // @PathVariable：读取URL路径中的资源ID，例如{id}；它不是当前登录用户ID。
    @PostMapping("/wallet/recharge/{id}/simulate-payment")
    public Result<RechargeOrderRespDto> pay(@PathVariable Long id){
        return Result.ok(wallet.simulatePayment(UserContext.getUserId(),id));
    }
    // 接口：POST /api/app/chapter/buy/{id}；由Spring根据HTTP方法和路径匹配到下方方法。
    // @PathVariable：读取URL路径中的资源ID，例如{id}；它不是当前登录用户ID。
    // @RequestBody：将请求体JSON转换为DTO；@Valid触发DTO上的非空、长度、金额范围等校验。
    @PostMapping("/chapter/buy/{id}")
    public Result<TransactionRespDto> buy(@PathVariable Long id,@Valid @RequestBody BuyChapterReqDto req){
        return Result.ok(wallet.buy(UserContext.getUserId(),id,req));
    }
    // 接口：POST /api/app/book/tip/{id}；由Spring根据HTTP方法和路径匹配到下方方法。
    // @PathVariable：读取URL路径中的资源ID，例如{id}；它不是当前登录用户ID。
    // @RequestBody：将请求体JSON转换为DTO；@Valid触发DTO上的非空、长度、金额范围等校验。
    @PostMapping("/book/tip/{id}")
    public Result<TransactionRespDto> tip(@PathVariable Long id,@Valid @RequestBody TipReqDto req){
        return Result.ok(wallet.tip(UserContext.getUserId(),id,req));
    }
}
