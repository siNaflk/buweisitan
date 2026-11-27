package com.xunye.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xunye.dao.*;
import com.xunye.dto.req.*;
import com.xunye.dto.resp.*;
import com.xunye.entity.*;
import com.xunye.enums.ErrorCode;
import com.xunye.exception.BizException;
import com.xunye.service.WalletService;
import com.xunye.service.VipService;
import com.xunye.service.BusinessEventService;
import com.xunye.constant.*;
import static com.xunye.constant.WalletConstant.*;
import static com.xunye.constant.CommonConstant.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Redis短锁用于防连点；数据库行锁、唯一索引和事务才是资金一致性的最终保障。
 * 不使用异步消息写资金流水：任何一步失败，余额、购买/打赏记录及流水一起回滚。
 */
// @Service：把该实现类交给Spring管理，Controller注入Service接口时可获得这个Bean。
@Service
// @RequiredArgsConstructor：Lombok为未初始化的final字段生成构造方法，Spring通过构造方法注入依赖。
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final VipService vip;
    private final BusinessEventService events;
    private final VipPlanMapper vipPlans;
    private final VipSubscriptionMapper vipSubscriptions;
    private final VipSubscriptionDetailMapper vipDetails;
    private final UserMapper users;
    private final BookMapper books;
    private final BookChapterMapper chapters;
    private final ChapterPurchaseMapper purchases;
    private final TipRecordMapper tips;
    private final RechargePlanMapper rechargePlans;
    private final RechargeOrderMapper orders;
    private final BalanceFlowMapper balanceFlows;
    private final StringRedisTemplate redis;
    private final TransactionTemplate transactions;

    // @Value：读取配置 xunye.payment.simulated-enabled，未配置时使用 true。
    @Value("${xunye.payment.simulated-enabled:true}")
    private boolean simulatedEnabled;
    // @Value：读取配置 xunye.payment.author-share-percent，未配置时使用 50。
    @Value("${xunye.payment.author-share-percent:50}")
    private int authorShare;

    /** 查询当前用户余额；金额单位为分，直接读取数据库，避免缓存余额过期。 */
    // @Override：标记这是Service接口方法的实现，编译器会检查签名；它本身不负责开启事务。
    @Override
    public WalletRespDto wallet(Long userId) {
        return new WalletRespDto(active(users.selectById(userId)).getBalance(),simulatedEnabled);
    }

    /** 只查询未删除且上架的充值档位，按排序值、ID排列后转换为响应DTO。 */
    @Override
    public List<RechargePlanRespDto> plans() {
        return rechargePlans.selectList(new LambdaQueryWrapper<RechargePlan>().eq(RechargePlan::getStatus,0)
            .orderByAsc(RechargePlan::getSort).orderByAsc(RechargePlan::getId)).stream()
            .map(p->new RechargePlanRespDto(p.getId(),p.getTitle(),p.getAmount(),p.getBonusAmount())).toList();
    }

    /** 创建充值订单。先锁用户、检查requestId是否已用，再将档位金额和赠送额保存为订单快照。 */
    @Override
    public RechargeOrderRespDto recharge(Long userId,RechargeReqDto req) {
        // guarded会创建事务；users.lock内部SELECT ... FOR UPDATE的锁会持续到事务结束。
        return guarded(userId,()->{
            active(users.lock(userId));
            RechargeOrder existing=orders.selectOne(new LambdaQueryWrapper<RechargeOrder>()
                .eq(RechargeOrder::getUserId,userId).eq(RechargeOrder::getRequestId,req.requestId()));
            if(existing!=null) {
                if(!existing.getPlanId().equals(req.planId()))conflict();
                return orderDto(closeExpired(existing));
            }
            RechargePlan p=rechargePlans.selectById(req.planId());
            if(p==null || !Integer.valueOf(NORMAL).equals(p.getStatus()))bad("充值档位已下架");
            if(p.getAmount()==null || p.getAmount()<=0 || p.getBonusAmount()==null || p.getBonusAmount()<0)bad("充值档位金额配置错误");
            Math.addExact(p.getAmount(),p.getBonusAmount());
            RechargeOrder o=new RechargeOrder();
            o.setOrderNo(number());o.setRequestId(req.requestId());o.setUserId(userId);o.setPlanId(p.getId());
            o.setAmount(p.getAmount());o.setBonusAmount(p.getBonusAmount());o.setStatus(ORDER_PENDING);
            o.setExpiresAt(LocalDateTime.now().plusMinutes(RECHARGE_EXPIRE_MINUTES));
            orders.insert(o);
            // 消息写入与订单同事务：只有订单提交成功，发布任务才看得到这条超时事件。
            events.enqueue(MqConstant.ORDER_TIMEOUT,userId,o.getId(),o.getExpiresAt(),MqConstant.ORDER_TIMEOUT+":"+o.getId());
            return orderDto(o);
        });
    }

    /** 查询本人订单；若订单已过有效期但仍待支付，将其关闭后返回。 */
    @Override
    public RechargeOrderRespDto order(Long userId,Long orderId) {
        return guarded(userId,()->{
            active(users.lock(userId));
            return orderDto(closeExpired(ownedOrder(userId,orderId)));});
    }
    /** 确认模拟支付。只有本人未过期、未入账订单才能充值；余额、流水和支付状态一起提交。 */
    @Override
    public RechargeOrderRespDto simulatePayment(Long userId,Long orderId) {
        if(!simulatedEnabled)throw new BizException(ErrorCode.FORBIDDEN,"模拟支付未启用");
        // guarded先做Redis防重复点击，再开启数据库事务；lambda内正常结束才提交，抛异常则回滚。
        // () -> {...}表示把“开通会员这一整段操作”作为Supplier传给guarded执行。
        return guarded(userId,()->{
            // 1. SELECT ... FOR UPDATE锁住该用户行，锁一直持有到事务结束；active检查账户存在且未封禁。
            // 同一用户的资金变动和VIP续费必须依次处理，防止两次续费同时读取同一个旧到期时间。
            User user=active(users.lock(userId));
            RechargeOrder o=closeExpired(ownedOrder(userId,orderId));
            // 已支付与已关闭订单都直接返回原状态，绝不再次入账。
            if(o.getStatus()!=ORDER_PENDING)return orderDto(o);
            change(user,Math.addExact(
                    o.getAmount(),o.getBonusAmount()),FLOW_RECHARGE,REF_PLAN,o.getPlanId(),"模拟充值 "+o.getOrderNo());
            o.setStatus(ORDER_PAID);o.setPaidAt(LocalDateTime.now());orders.updateById(o);
            return orderDto(o);
        });
    }
    /** 购买章节。核对上线状态、已购记录和确认价格，再完成读者扣款、作者分成及购买记录写入。 */
    @Override
    public TransactionRespDto buy(Long userId,Long chapterId,BuyChapterReqDto req) {
        return guarded(userId,()->{
            BookChapter c=chapters.selectById(chapterId);
            if(c==null || !Integer.valueOf(AUDIT_APPROVED).equals(c.getAuditStatus()))throw new BizException(ErrorCode.CHAPTER_NOT_ONLINE);
            Book b=online(c.getBookId());
            Map<Long,User> accounts=lockAccounts(userId,b.getAuthorId());
            User buyer=active(accounts.get(userId));
            // 专属作品不出售单章。先检查专属门槛，不能让免费标记、作者身份或旧购章记录绕过VIP。
            if(Integer.valueOf(YES).equals(b.getIsVipLibrary())) {
                if(!vip.isVip(buyer))throw new BizException(ErrorCode.VIP_INVALID,"本书为会员专属，请先开通VIP，不支持单章购买");
                return new TransactionRespDto(TYPE_PURCHASE,null,0L,buyer.getBalance(),true);
            }
            ChapterPurchase existing=purchases.selectOne(new LambdaQueryWrapper<ChapterPurchase>()
                .eq(ChapterPurchase::getUserId,userId).eq(ChapterPurchase::getChapterId,chapterId));
            if(existing!=null)return new TransactionRespDto(TYPE_PURCHASE,existing.getId(),existing.getAmount(),buyer.getBalance(),true);
            // 普通作品：有效VIP、免费章节或作者本人都无需购章，不生成购买记录或分成流水。
            if(vip.isVip(buyer) || Integer.valueOf(YES).equals(c.getIsFree()) || userId.equals(b.getAuthorId()))
                return new TransactionRespDto(TYPE_PURCHASE,null,0L,buyer.getBalance(),true);
            long originalPrice=c.getPrice()==null?b.getChapterPrice():c.getPrice();
            long price=vip.chapterPrice(originalPrice,buyer);
            if(price<=0)bad("付费章节价格配置错误");
            if(price!=req.expectedPrice())throw new BizException(HTTP_CONFLICT,"章节价格已变化，请刷新后重新确认");
            if(authorShare<0 || authorShare>PERCENT_BASE)bad("作者分成配置错误");
            long income=Math.multiplyExact(price,authorShare)/PERCENT_BASE;
            User author=recipient(accounts.get(b.getAuthorId()));
            change(buyer,-price,FLOW_PURCHASE,REF_CHAPTER,chapterId,"购买章节："+c.getTitle());
            if(income>0)change(author,income,FLOW_AUTHOR_INCOME,REF_CHAPTER,chapterId,"章节分成："+c.getTitle());
            ChapterPurchase p=new ChapterPurchase();
            p.setUserId(userId);p.setChapterId(chapterId);p.setBookId(b.getId());
            p.setAmount(price);p.setAuthorIncome(income);
            purchases.insert(p);
            return new TransactionRespDto(TYPE_PURCHASE,p.getId(),price,buyer.getBalance(),false);
        });
    }
    /** 打赏作品。禁止自我打赏；按提交标识去重，在同一事务完成读者扣款、作者入账和双方流水。 */
    @Override
    public TransactionRespDto tip(Long userId,Long bookId,TipReqDto req) {
        return guarded(userId,()->{
            Book b=online(bookId);
            if(userId.equals(b.getAuthorId()))bad("不能给自己的作品打赏");
            Map<Long,User> accounts=lockAccounts(userId,b.getAuthorId());
            User sender=active(accounts.get(userId));
            String tipNo=tipNumber(userId,req.requestId());
            TipRecord old=tips.selectOne(new LambdaQueryWrapper<TipRecord>().eq(TipRecord::getTipNo,tipNo));
            String message=req.message()==null?"":req.message().strip();
            if(old!=null) {
                if(!old.getUserId().equals(userId) ||
                        !old.getBookId().equals(bookId) ||
                        !old.getAmount().equals(req.amount()) ||
                        !Objects.equals(old.getMessage(),message))conflict();
                return new TransactionRespDto(TYPE_TIP,old.getId(),old.getAmount(),sender.getBalance(),true);
            }
            User author=recipient(accounts.get(b.getAuthorId()));
            change(sender,-req.amount(),FLOW_TIP_OUT,REF_BOOK,bookId,"作品打赏："+b.getTitle());
            change(author,req.amount(),FLOW_TIP_IN,REF_BOOK,bookId,"收到打赏："+b.getTitle());
            TipRecord t=new TipRecord();
            t.setTipNo(tipNo);t.setUserId(userId);t.setBookId(bookId);t.setAmount(req.amount());t.setMessage(message);
            tips.insert(t);
            return new TransactionRespDto(TYPE_TIP,t.getId(),t.getAmount(),sender.getBalance(),false);
        });
    }

    /**
     * VIP订阅与续费：先锁用户，再检查提交标识；剩余时长不丢失，新套餐从旧有效期结束后开始。
     * 只有同一数据库事务中的余额、流水、订阅记录、权益快照、用户到期时间和通知事件全部写入成功才提交。
     */
    @Override
    public VipSubscriptionRespDto subscribe(Long userId,Long planId,VipSubscribeReqDto req) {
        return guarded(userId,()->{
            User user=active(users.lock(userId));
            // 2. 先按“用户ID + requestId”查重。同一次点击因网络超时重试，必须沿用原requestId。
            // 先查重再查当前套餐：即使用户第一次已成功、套餐随后下架，重试仍可返回原交易。
            VipSubscriptionDetail prior=vipDetails.selectOne(new LambdaQueryWrapper<VipSubscriptionDetail>()
                .eq(VipSubscriptionDetail::getUserId,userId).eq(VipSubscriptionDetail::getRequestId,req.requestId()));
            if(prior!=null) {
                VipSubscription old=vipSubscriptions.selectById(prior.getSubscriptionId());
                // 同一个标识不能改成另一个套餐或另一个确认金额，否则返回409，防止串单。
                if(old==null || !old.getPlanId().equals(planId) || !old.getAmount().equals(req.expectedPrice()))conflict();
                // true表示此前已经处理成功；本次不再扣款、不再延长有效期。
                return subscriptionDto(old,prior,true);
            }
            // 3. 新交易以服务端套餐配置为准，校验上架状态、价格和天数；discount仍校验旧字段兼容格式。
            VipPlan plan=vipPlans.selectById(planId);
            if(plan==null || !Integer.valueOf(NORMAL).equals(plan.getStatus()))bad("VIP套餐已下架");
            if(plan.getPrice()==null || plan.getPrice()<=0 || plan.getDurationDays()==null || plan.getDurationDays()<=0
                || plan.getDurationDays()>VipConstant.MAX_PLAN_DAYS || plan.getDiscount()==null || plan.getDiscount()<1 || plan.getDiscount()>PERCENT_BASE)
                bad("VIP套餐配置不合法");
            // 前端金额只是“用户确认过的报价”；报价发生变化时要求重新确认，不能静默按新价扣款。
            if(!plan.getPrice().equals(req.expectedPrice()))throw new BizException(HTTP_CONFLICT,"套餐价格已变化，请刷新后重新确认");
            // 4. 未到期：从旧到期时间接着续，剩余天数不会丢；已过期/首次开通：从现在开始。
            // 例如还剩5天再买30天，总有效期延长为约35天；本次订阅记录的start是5天后的旧到期日。
            // withNano(0)对齐数据库DATETIME的秒精度，避免小数秒舍入产生略晚于当前时间的开始值。
            LocalDateTime start=vip.isVip(user)?user.getVipExpireTime():LocalDateTime.now().withNano(0);
            LocalDateTime expire=start.plusDays(plan.getDurationDays());
            // 5. 负数代表支出；change同时更新余额并写资金流水，余额不足会抛异常使整笔交易回滚。
            change(user,-plan.getPrice(),FLOW_VIP_SUBSCRIPTION,REF_VIP_PLAN,planId,"VIP订阅："+plan.getName());
            // 6. 保存这次交易：订单号、实付金额以及本次新增的有效时段。insert后会回填主键sub.id。
            VipSubscription sub=new VipSubscription();
            sub.setOrderNo(number());sub.setUserId(userId);sub.setPlanId(planId);
            sub.setAmount(plan.getPrice());sub.setStartTime(start);sub.setExpireTime(expire);sub.setIsDeleted(NORMAL);
            vipSubscriptions.insert(sub);
            // 7. 保存快照和重试标识；即使套餐以后改名/改天数，历史订阅仍显示购买时的信息。
            // discount仅保留历史兼容，不控制当前VIP畅读权限；数据库唯一约束也防止同用户重复requestId。
            VipSubscriptionDetail detail=new VipSubscriptionDetail();detail.setSubscriptionId(sub.getId());detail.setUserId(userId);
            detail.setRequestId(req.requestId());detail.setPlanName(plan.getName());detail.setDurationDays(plan.getDurationDays());detail.setDiscount(plan.getDiscount());
            vipDetails.insert(detail);
            // 8. 更新用户总到期时间。阅读接口查看这个字段，因此提交后立即获得统一VIP畅读权益。
            users.update(null,new UpdateWrapper<User>().eq("id",userId).set("vip_expire_time",expire));
            // 9. 仅把“订阅成功通知事件”写入本地消息表，与扣款在同一事务提交；这里没有直接向MQ发消息。
            // 提交后由OutboxPublisher投递，消费者写站内通知；MQ暂时不可用也不应让用户重复扣款。
            // 最后一个参数是事件唯一键，同一订阅只应生成一条成功通知事件。
            events.enqueue(MqConstant.VIP_OPENED,userId,sub.getId(),null,MqConstant.VIP_OPENED+":"+sub.getId());
            return subscriptionDto(sub,detail,false);
        });
    }
    /**
     * 组合订阅记录与套餐快照，返回前端需要的数据。
     * processed=false表示这次刚完成交易；true表示重复请求命中了原交易，两者都可视为成功。
     * 这里仅组装DTO，不会再扣款或修改数据库。
     */
    private VipSubscriptionRespDto subscriptionDto(VipSubscription s,VipSubscriptionDetail d,boolean processed) {
        return new VipSubscriptionRespDto(s.getId(),s.getOrderNo(),s.getPlanId(),d.getPlanName(),s.getAmount(),d.getDiscount(),s.getStartTime(),s.getExpireTime(),processed);
    }

    /** 分页查询本人收支明细，按ID倒序展示；只向前端返回DTO。 */
    @Override
    public Page<BalanceFlowRespDto> flows(Long userId,int page,int size) {
        Page<BalanceFlow> data=balanceFlows.selectPage(new Page<>(Math.max(1,page), Math.clamp(size, 1, MAX_PAGE_SIZE)),
            new LambdaQueryWrapper<BalanceFlow>().eq(BalanceFlow::getUserId,userId).orderByDesc(BalanceFlow::getId));
        Page<BalanceFlowRespDto> out=new Page<>(data.getCurrent(),data.getSize(),data.getTotal());
        out.setRecords(data.getRecords().stream().map(
                f->new BalanceFlowRespDto(f.getId(),f.getFlowNo(),f.getType(),f.getAmount(),f.getBalanceAfter(),f.getRefType(),f.getRefId(),f.getRemark(),f.getCreateTime())).toList());
        return out;
    }

    /** 交易统一入口：获取Redis短锁 → 开启数据库事务 → 执行业务 → 提交或回滚 → 释放短锁。 */
    private <T>T guarded(Long userId,Supplier<T> action) {
        if(userId==null)throw new BizException(ErrorCode.UNAUTHORIZED);
        String key=RedisKeyConstant.WALLET_LOCK+userId, token=number();
        if(!Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key,token,Duration.ofSeconds(LOCK_SECONDS))))
            throw new BizException(HTTP_TOO_MANY_REQUESTS,"正在处理上一笔交易，请稍后重试");
        try {
            // MySQL默认可重复读会保留加锁前的快照；改用读已提交，等待账户锁后能读到刚提交的幂等记录。
            // 编程式事务：使用execute包住业务，正常返回提交，未捕获异常则回滚；因此本类资金方法不再重复加@Transactional。
            TransactionTemplate tx=new TransactionTemplate(transactions.getTransactionManager());
            tx.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED);
            return tx.execute(status->action.get());
        }
        finally {
            // 释放失败不覆盖已经提交的交易结果；短锁会自动到期，重试由数据库幂等保证。
            try {redis.execute(
                    new DefaultRedisScript<>("if redis.call('get',KEYS[1])==ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end",Long.class),
                    List.of(key),token);
            } catch(org.springframework.dao.DataAccessException ignored) {}
        }
    }

    /** 按用户ID从小到大获取账户行锁；读者和作者使用相同顺序，减少相互等待导致死锁的风险。 */
    private Map<Long,User> lockAccounts(Long a,Long b) {
        Map<Long,User> out=new HashMap<>();
        for(Long id:new TreeSet<>(List.of(a,b)))out.put(id,users.lock(id));
        return out;
    }

    /** 所有调用均持有账户行锁；条件更新额外防止负余额，并在同一事务记录变更后余额。 */
    /** 持有账户行锁时修改余额并写流水：delta正数为收入、负数为支出；不足则抛异常，不允许负余额。 */
    private void change(User user,long delta,int type,int refType,Long refId,String remark) {
        long after=Math.addExact(user.getBalance(),delta);
        if(after<0)throw new BizException(ErrorCode.BALANCE_INSUFFICIENT);
        int changed=users.update(null,new UpdateWrapper<User>().eq("id",user.getId()).eq("balance",user.getBalance()).set("balance",after));
        if(changed!=1)throw new BizException(HTTP_CONFLICT,"账户已变化，请重试");
        user.setBalance(after);
        BalanceFlow f=new BalanceFlow();
        f.setFlowNo(number());f.setUserId(user.getId());f.setType(type);
        f.setAmount(Math.abs(delta));f.setBalanceAfter(after);f.setRefType(refType);f.setRefId(refId);f.setRemark(remark);
        balanceFlows.insert(f);
    }
    /** 锁定订单并核对归属，防止用户通过猜测订单ID操作别人的订单。 */
    private RechargeOrder ownedOrder(Long userId,Long id) {
        RechargeOrder o=orders.lock(id);
        if(o==null || !userId.equals(o.getUserId()))throw new BizException(ErrorCode.FORBIDDEN,"订单不存在或无权访问");
        return o;
    }
    /** 仅将超时的待支付订单改为已关闭；已经支付的订单不会被改动。 */
    private RechargeOrder closeExpired(RechargeOrder o) {
        if(o.getStatus()==ORDER_PENDING && !o.getExpiresAt().isAfter(LocalDateTime.now())) {
            o.setStatus(ORDER_CLOSED);
            orders.updateById(o);
        }
        return o;
    }
    /** 把订单实体转为前端响应，只返回订单展示所需字段。 */
    private RechargeOrderRespDto orderDto(RechargeOrder o) {
        return new RechargeOrderRespDto(o.getId(),o.getOrderNo(),o.getPlanId(),o.getAmount(),o.getBonusAmount(),o.getStatus(),o.getExpiresAt());
    }
    /** 校验作品存在、审核通过且未下架，不满足条件就中断交易。 */
    private Book online(Long id) {
        Book b=books.selectById(id);
        if(b==null || !Integer.valueOf(AUDIT_APPROVED).equals(b.getAuditStatus()) || Integer.valueOf(BOOK_OFFLINE).equals(b.getStatus()))throw new BizException(ErrorCode.BOOK_NOT_EXIST);
        return b;
    }

    /** 校验付款用户存在且账号正常，返回后续计算余额所需的用户实体。 */
    private User active(User u) {
        if(u==null)throw new BizException(ErrorCode.UNAUTHORIZED);
        if(!Integer.valueOf(NORMAL).equals(u.getStatus()))throw new BizException(ErrorCode.USER_BANNED);
        return u;
    }
    /** 检查作者收款账户仍存在；作者被封禁并不阻止其收到既有作品的收益。 */
    // 被封禁作者仍可收到既有作品收益；不存在的账户不能接收资金。
    private User recipient(User u) {
        if(u==null)throw new BizException(ErrorCode.PARAM_ERROR,"作者账户不可用");
        return u;
    }
    /** 生成不含连字符的32位随机UUID，作为订单号、流水号或Redis锁持有标识。 */
    private String number() {
        return UUID.randomUUID().toString().replace("-","");
    }
    /** 由用户ID和requestId生成固定打赏单号；同一次提交重试会得到相同单号，用唯一索引防重复。 */
    private String tipNumber(Long userId,String requestId) {
        try{
            return HexFormat.of().formatHex(MessageDigest.getInstance(SHA_256).digest((userId+":"+requestId).getBytes(StandardCharsets.UTF_8))).substring(0,32);
        }
        catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    /** 同一提交标识被用于不同交易参数时返回409，要求重新确认交易。 */
    private void conflict(){throw new BizException(HTTP_CONFLICT,"提交标识已用于另一笔交易，请重新确认");}
    /** 统一抛出参数类业务异常；异常交给全局处理器转换为Result响应。 */
    private void bad(String msg){throw new BizException(ErrorCode.PARAM_ERROR,msg);}
}
