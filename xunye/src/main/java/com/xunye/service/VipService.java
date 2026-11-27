package com.xunye.service;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xunye.dto.resp.*;
import com.xunye.entity.User;
import java.util.List;
/** VIP读业务及全项目统一权益判断；支付仍由WalletService在资金事务中完成。 */
public interface VipService {
    /** 只判断传入用户的VIP到期时间是否晚于现在；账号登录和封禁校验由调用方负责。 */
    boolean isVip(User user);
    /** 历史折扣兼容接口，当前会员改为畅读，不参与新购章计费。 */
    @Deprecated
    int discount(User user);
    /** 普通作品购章计价：有效VIP返回0，非VIP返回原价；会员专属必须先单独检查VIP门槛。 */
    long chapterPrice(long originalPrice,User user);
    /** 查询当前用户表中的到期时间和余额，并计算是否为有效VIP。 */
    VipStatusRespDto status(Long userId);
    /** 查询上架套餐；各套餐价格和天数不同，阅读权益相同。 */
    List<VipPlanRespDto> plans();
    /** 按用户ID分页查询历史订阅，并补充购买当时的套餐快照。 */
    Page<VipSubscriptionRespDto> subscriptions(Long userId,int page,int size);
    /** 查询审核通过且未下架的会员专属作品简介，不返回章节正文。 */
    Page<BookBriefRespDto> library(int page,int size);
}
