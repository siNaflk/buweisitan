<template>
  <view class="m2-page">
    <view class="vip-card"><view class="vip-top"><text class="vip-label">寻页 VIP</text><text class="vip-pill">{{status.active?'会员权益生效中':'让好故事陪你更久'}}</text></view><text class="vip-title">{{status.active?'继续你的阅读旅程':'开通会员，畅读好书'}}</text><text class="vip-desc">{{status.active?'有效期至 '+date(status.expireTime):'全站畅读 · 解锁会员专属作品'}}</text><view class="vip-bottom"><text>{{status.active?'普通作品全章畅读':'登录后可使用余额开通'}}</text><text @click="library">进入会员专属 ›</text></view></view>
    <view class="m2-card"><view class="m2-heading"><text>选择会员套餐</text><text v-if="loggedIn" class="m2-muted">余额 ¥ {{money(status.balance)}}</text></view>
      <view class="plans"><view v-for="p in plans" :key="p.id" class="plan" :class="{selected:String(selected)===String(p.id)}" @click="choose(p)"><text class="plan-name">{{p.name}}</text><text class="plan-price">¥ {{money(p.price)}}</text><text class="plan-days">{{p.durationDays}}天 · 全站畅读</text></view></view>
      <view v-if="current" class="privileges"><text v-for="s in benefits(current)" :key="s">✓ {{s}}</text></view>
      <text class="notice">续费从当前到期时间顺延，剩余天数不会丢失。所有套餐均享畅读权益，区别在于有效天数。会员专属作品只允许VIP阅读。</text>
      <button class="vip-button" :disabled="busy||loading||(!current&&!draft)" :loading="busy" @click="confirm">{{draft?'重试确认上笔订阅结果':(status.active?'余额续费':'余额开通')}}</button>
      <text v-if="draft" class="notice">上笔请求结果尚未确认，重试会沿用同一提交标识，不会重复扣款。</text>
      <text v-if="!plans.length&&!loading" class="m2-muted">暂无可购买套餐</text><text class="m2-link" @click="wallet">前往钱包充值 ›</text>
    </view>
    <view v-if="loggedIn" class="m2-card"><view class="m2-heading"><text>我的订阅记录</text><text class="m2-link" @click="loadRecords(true)">刷新</text></view><view v-for="r in records" :key="r.id" class="record"><view class="m2-heading"><text>{{r.planName}}</text><text>¥ {{money(r.amount)}}</text></view><text class="m2-muted">{{date(r.startTime)}} 至 {{date(r.expireTime)}}</text><text class="m2-muted record-sub">VIP畅读权益</text></view><text v-if="!records.length&&!recordLoading" class="m2-muted">还没有订阅记录</text><view class="m2-link m2-center" v-if="recordLoading||records.length<total" @click="loadRecords(false)">{{recordLoading?'加载中…':'加载更多'}}</view></view>
    <text v-if="error" class="m2-error">{{error}}</text><view class="m2-link m2-center" @click="refresh">刷新会员信息</view>
  </view>
</template>
<script>
import {getVipPlans,getVipStatus,subscribeVip,getVipSubscriptions} from '../../api/vip'
import {money,requestId} from '../../utils/money'
import {tokenStore} from '../../utils/request'
import {requireLogin} from '../../utils/auth'
export default{
  data(){return{plans:[],status:{active:false,discount:100,balance:0},selected:null,loggedIn:false,busy:false,loading:false,error:'',draft:null,records:[],page:1,total:0,recordLoading:false}},
  computed:{current(){return this.plans.find(p=>String(p.id)===String(this.selected))}},
  onShow(){this.refresh()},
  methods:{money,date(v){return(v||'').replace('T',' ').slice(0,16)},benefits(){return ['会员专属作品畅读','普通作品全部章节畅读']},
    key(){return 'vipSubscribeDraft:'+uni.getStorageSync('userInfo')?.id},
    choose(p){if(this.busy||this.draft)return;this.selected=p.id},
    library(){uni.navigateTo({url:'/pages/vip/library'})},wallet(){if(requireLogin())uni.navigateTo({url:'/pages/wallet/index'})},
    clearDraft(){uni.removeStorageSync(this.key());this.draft=null},
    async refresh(){if(this.loading||this.busy)return;this.loading=true;this.error='';this.loggedIn=!!tokenStore.get();this.status={active:false,discount:100,balance:0};this.records=[];this.page=1;this.total=0
      try{this.plans=await getVipPlans();this.draft=this.loggedIn?(uni.getStorageSync(this.key())||null):null;this.selected=this.draft?.planId||this.selected||this.plans[0]?.id
        if(this.loggedIn){this.status=await getVipStatus();await this.loadRecords(true)}}catch(e){this.error=e.msg||'会员信息加载失败'}finally{this.loading=false}},
    async loadRecords(reset){if(this.recordLoading||!this.loggedIn)return;this.recordLoading=true;try{if(reset){this.records=[];this.page=1}const d=await getVipSubscriptions({pageNum:this.page,pageSize:10});this.records.push(...d.records);this.total=Number(d.total);this.page++}catch(e){this.error=e.msg}finally{this.recordLoading=false}},
    confirm(){if(this.busy||!requireLogin())return;const p=this.current;if(!p&&!this.draft)return
      const pending=this.draft||{planId:p.id,expectedPrice:p.price,requestId:requestId()}
      uni.showModal({title:this.status.active?'确认续费':'确认开通',content:'使用余额支付 ¥ '+money(pending.expectedPrice)+'。续费保留剩余时长，是否继续？',success:async r=>{
        if(!r.confirm||this.busy)return;this.busy=true;this.error='';this.draft=pending;uni.setStorageSync(this.key(),pending)
        try{const result=await subscribeVip(pending.planId,pending);this.clearDraft();uni.showToast({title:result.alreadyProcessed?'该笔订阅已完成':'订阅成功',icon:'none'});this.busy=false;await this.refresh()}
        catch(e){this.error=e.msg||'结果暂未确认，请重试';if([400,409,5001].includes(Number(e.code))){this.clearDraft();if(Number(e.code)===5001)uni.showModal({title:'余额不足',content:'是否前往钱包充值？',success:r=>{if(r.confirm)this.wallet()}})}}finally{this.busy=false}
      }})}
  }
}
</script>
<style scoped>
.vip-card{background:linear-gradient(135deg,#352b28,#67503c);color:#f6e5c5;border-radius:28rpx;padding:34rpx;margin-bottom:24rpx}.vip-top,.vip-bottom{display:flex;justify-content:space-between;gap:16rpx;align-items:center}.vip-label{font-size:30rpx;letter-spacing:4rpx}.vip-pill{font-size:20rpx;border:1rpx solid #af9470;border-radius:24rpx;padding:6rpx 14rpx}.vip-title{display:block;font-size:40rpx;font-weight:bold;margin:34rpx 0 16rpx}.vip-desc{font-size:24rpx;line-height:1.7;display:block;color:#d9c5a6}.vip-bottom{border-top:1rpx solid #80674f;padding-top:24rpx;margin-top:28rpx;font-size:23rpx}.plans{display:flex;gap:14rpx;flex-wrap:wrap;margin:28rpx 0}.plan{flex:1;min-width:140rpx;text-align:center;border:2rpx solid #e3d9cb;border-radius:18rpx;padding:26rpx 8rpx;display:flex;flex-direction:column;gap:16rpx}.selected{border-color:#a97939;background:#fff8eb}.plan-name{font-size:28rpx}.plan-price{font-size:38rpx;font-weight:bold;color:#8f622f}.plan-days{font-size:22rpx;color:#967449}.privileges{display:flex;flex-direction:column;gap:14rpx;color:#805e38;font-size:25rpx}.notice{display:block;font-size:23rpx;color:#8c8276;line-height:1.8;margin:24rpx 0}.vip-button{background:#66503a;color:#fff3d8;border-radius:40rpx;font-size:28rpx;margin-bottom:24rpx}.record{padding:24rpx 0;border-top:1rpx solid #eee}.record-sub{display:block;margin-top:10rpx}
</style>
