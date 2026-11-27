package com.xunye.service;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xunye.dto.req.*;
import com.xunye.dto.resp.*;
import java.util.List;
/** M3资金服务：余额、交易记录和流水在同一个数据库事务中提交。 */
public interface WalletService {
    /** VIP余额订阅/续费；余额、流水、有效期、订阅快照与待发通知同事务提交。 */
    VipSubscriptionRespDto subscribe(Long userId,Long planId,VipSubscribeReqDto req);
    /** 查询本人最新余额，不从Redis读取。 */
    WalletRespDto wallet(Long userId);
    /** 查询当前上架档位。 */
    List<RechargePlanRespDto> plans();
    /** 创建快照订单；相同requestId返回同一订单。 */
    RechargeOrderRespDto recharge(Long userId, RechargeReqDto req);
    /** 查询本人订单，过期未支付订单关闭。 */
    RechargeOrderRespDto order(Long userId, Long orderId);
    /** 模拟支付；重复确认不会重复入账。 */
    RechargeOrderRespDto simulatePayment(Long userId, Long orderId);
    /** 购章与双方流水在同一事务内完成。 */
    TransactionRespDto buy(Long userId, Long chapterId, BuyChapterReqDto req);
    /** 打赏全额转入作者余额，以requestId防止重试双扣。 */
    TransactionRespDto tip(Long userId, Long bookId, TipReqDto req);
    /** 只分页查询本人流水。 */
    Page<BalanceFlowRespDto> flows(Long userId, int page, int size);
}
