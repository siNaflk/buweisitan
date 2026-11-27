package com.xunye.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xunye.constant.*;
import com.xunye.dao.*;
import com.xunye.dto.resp.*;
import com.xunye.entity.*;
import com.xunye.enums.ErrorCode;
import com.xunye.exception.BizException;
import com.xunye.service.VipService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
/**
 * 会员查询与权益判断的实现类，不处理余额扣款（扣款见WalletServiceImpl.subscribe）。
 * <p>三类数据的作用：User.vipExpireTime表示当前总有效期；VipSubscription记录每次购买的时段和实付金额；
 * VipSubscriptionDetail保存请求标识和购买当时的套餐信息，防止套餐后续修改影响历史记录。
 * <p>阅读规则：会员专属仅有效VIP可读；普通作品有效VIP全章畅读，非会员按免费/已购权限阅读。
 */
// @Service：交给Spring管理，其他类可以通过VipService接口注入此实现。
@Service
// Lombok根据final字段生成构造方法，Spring负责注入下面的Mapper。
@RequiredArgsConstructor
public class VipServiceImpl implements VipService {
    private final UserMapper users;
    private final VipPlanMapper plans;
    private final VipSubscriptionMapper subscriptions;
    private final VipSubscriptionDetailMapper details;
    private final BookMapper books;
    /**
     * 有效VIP必须同时满足：用户存在、设置了到期时间、到期时间严格晚于当前时间。
     * 例如到期时间为明天则为true，未开通过或已经到期则为false；恰好到期也为false。
     * 本方法只检查传入对象，不会查询数据库；调用方应使用当前查询到的用户信息。
     * 登录校验、账号封禁校验属于其他职责，不能仅凭此方法认定请求身份合法。
     */
    @Override
    public boolean isVip(User user) {
        return user!=null && user.getVipExpireTime()!=null && user.getVipExpireTime().isAfter(LocalDateTime.now());
    }
    /** 历史折扣快照查询，仅兼容旧记录；当前阅读权限与扣款不再使用折扣。 */
    @Deprecated
    @Override
    public int discount(User user) {
        if(!isVip(user))return VipConstant.NO_DISCOUNT;
        // 仅查询此刻已生效的旧订阅区间，尚未开始的续费区间不参与历史折扣查询。
        Integer d=details.activeDiscount(user.getId(),LocalDateTime.now());
        // 没有历史订阅快照时无法推导旧折扣；当前畅读权益不依赖此查询。
        return d==null?VipConstant.NO_DISCOUNT:d;
    }
    /**
     * 返回普通作品本次购章应付的金额，单位仍为分。
     * 返回0表示VIP无需购买，调用方应直接返回而不是创建一条0元购买记录。
     * 此方法没有作品参数，不能判断是否为会员专属；购买服务必须先拦截非VIP购买专属作品。
     */
    @Override
    public long chapterPrice(long price,User user) {
        // 新规则：VIP普通作品全章畅读；专属作品是否允许阅读，由章节服务先判断VIP门槛。
        return isVip(user) ? 0L : price;
    }
    /** 每次查询以数据库中的到期时间为准；不缓存余额或VIP状态，避免到期后继续授权。 */
    @Override
    public VipStatusRespDto status(Long userId) {
        User u=users.selectById(userId);
        if(u==null)throw new BizException(ErrorCode.UNAUTHORIZED);
        // active表示当前是否有效，expireTime供页面展示；discount是旧接口兼容值，不再代表购章折扣。
        return new VipStatusRespDto(isVip(u),u.getVipExpireTime(),isVip(u)?0:VipConstant.NO_DISCOUNT,u.getBalance());
    }
    /** 套餐价格、时长读取当前上架配置；所有套餐统一展示畅读权益，折扣字段仅兼容旧数据。 */
    @Override
    public List<VipPlanRespDto> plans() {
        // status=0只取上架套餐；先按运营配置sort排序，sort相同时按ID保证顺序稳定。
        return plans.selectList(new LambdaQueryWrapper<VipPlan>().eq(VipPlan::getStatus,CommonConstant.NORMAL)
        .orderByAsc(VipPlan::getSort).orderByAsc(VipPlan::getId)).stream()
        // stream.map逐个把实体转为DTO；
        .map(p->new VipPlanRespDto(p.getId(),p.getName(),p.getDurationDays(),p.getPrice(),p.getDiscount(),VipConstant.READING_PRIVILEGES)).toList();
    }
    /** 历史记录展示购买快照，不用当前套餐名称/折扣覆盖过去的购买信息。 */
    @Override
    public Page<VipSubscriptionRespDto> subscriptions(Long userId,int page,int size) {
        // 页码至少为1；Math.clamp将每页数量限制在1到MAX_PAGE_SIZE之间。
        // userId是查询条件，确保用户只能查到自己的订阅；ID倒序使最新订阅排在前面。
        Page<VipSubscription> data=subscriptions.selectPage(new Page<>(Math.max(1,page),Math.clamp(size,1,CommonConstant.MAX_PAGE_SIZE)),
        new LambdaQueryWrapper<VipSubscription>().eq(VipSubscription::getUserId,userId).orderByDesc(VipSubscription::getId));
        // 分页元数据沿用原查询结果，只把records从数据库实体转换为前端DTO。
        Page<VipSubscriptionRespDto> out=new Page<>(data.getCurrent(),data.getSize(),data.getTotal());
        out.setRecords(data.getRecords().stream().map(s->{
            // 用subscriptionId找到这次购买的快照，不能取当前VipPlan覆盖旧套餐名称或折扣。
            // M4之前的历史记录可能没有detail，因此使用“历史套餐”等兜底值，仍展示原金额和有效期。
            VipSubscriptionDetail d=details.selectOne(new LambdaQueryWrapper<VipSubscriptionDetail>().eq(VipSubscriptionDetail::getSubscriptionId,s.getId()));
            return new VipSubscriptionRespDto(
                    s.getId(),s.getOrderNo(),s.getPlanId(),d==null?"历史套餐":d.getPlanName(),s.getAmount(),
                    d==null?VipConstant.NO_DISCOUNT:d.getDiscount(),s.getStartTime(),s.getExpireTime(),true);
        }).toList());
        return out;
    }
    /**
     * 会员专属是作品级标记，不由某一章的isFree决定。
     * 这里只返回公开简介；章节正文接口独立校验VIP，因此游客浏览此列表不会获得正文权限。
     */
    @Override
    public Page<BookBriefRespDto> library(int page,int size) {
        // 三个条件同时成立：属于会员专属、审核通过、作品未下架；最近更新的排在前面。
        Page<Book> data=books.selectPage(new Page<>(Math.max(1,page),Math.clamp(size,1,CommonConstant.MAX_PAGE_SIZE)),
        new LambdaQueryWrapper<Book>().eq(Book::getIsVipLibrary,CommonConstant.YES).eq(Book::getAuditStatus,CommonConstant.AUDIT_APPROVED)
        .ne(Book::getStatus,CommonConstant.BOOK_OFFLINE).orderByDesc(Book::getUpdateTime).orderByDesc(Book::getId));
        Page<BookBriefRespDto> out=new Page<>(data.getCurrent(),data.getSize(),data.getTotal());
        out.setRecords(data.getRecords().stream().map(b->{
            BookBriefRespDto dto=new BookBriefRespDto();
            // 复制同名属性到简介DTO，避免把整个数据库实体直接作为接口响应。
            BeanUtils.copyProperties(b,dto);
            return dto;
        }).toList());
        return out;
    }
}
