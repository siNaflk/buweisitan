<template>
  <view class="m2-page">
    <view class="balance-card"><text class="balance-label">我的余额（元）</text><text class="balance">{{money(wallet.balance)}}</text><view class="balance-footer"><text>用于购章与支持作者</text><text @click="flows">收支明细 ›</text></view></view>
    <view class="m2-card"><view class="m2-heading">充值到钱包</view><text class="notice">比赛演示使用模拟支付，不会产生真实扣费。</text>
      <view class="plans"><view v-for="p in plans" :key="p.id" class="plan" :class="{selected:selected===p.id}" @click="select(p)"><text class="plan-price">¥ {{money(p.amount)}}</text><text>{{p.title}}</text><text class="bonus">赠送 ¥ {{money(p.bonusAmount)}}</text></view></view>
      <text v-if="!plans.length&&!loading" class="m2-muted">暂无可用充值档位</text>
      <button class="m2-primary" :disabled="busy||!selected||!wallet.simulatedPaymentEnabled||!!pending" :loading="busy" @click="create">创建充值订单</button>
      <text v-if="wallet.simulatedPaymentEnabled===false" class="notice">模拟支付已关闭，当前无法充值。</text>
    </view>
    <view v-if="pending" class="m2-card"><view class="m2-heading">待支付订单</view><text class="m2-body">充值 ¥ {{money(pending.amount)}}，赠送 ¥ {{money(pending.bonusAmount)}}</text><text class="m2-muted">订单号：{{pending.orderNo}}</text><text class="m2-muted order-time">有效至 {{date(pending.expiresAt)}}</text><button class="m2-primary" :disabled="busy||!wallet.simulatedPaymentEnabled" :loading="busy" @click="pay">确认模拟支付</button><text class="m2-link" @click="refresh">刷新订单状态</text></view>
    <text v-if="error" class="m2-error">{{error}}</text>
    <text class="m2-link m2-center" @click="refresh">{{loading?'加载中…':'刷新钱包'}}</text>
  </view>
</template>
<script>
import {getWallet,getRechargePlans,createRecharge,getRechargeOrder,simulatePayment} from '../../api/wallet'
import {requireLogin} from '../../utils/auth'
import {money,requestId} from '../../utils/money'
export default {
  data(){return{wallet:{balance:0},plans:[],selected:null,pending:null,busy:false,loading:false,error:'',draft:null}},
  onShow(){if(requireLogin())this.refresh()},
  methods:{money,date(v){return(v||'').replace('T',' ').slice(0,16)},
    storageKey(){return 'rechargeDraft:'+uni.getStorageSync('userInfo')?.id},
    select(p){if(this.pending||this.busy)return;this.selected=p.id},
    clearDraft(){this.pending=null;this.draft=null;uni.removeStorageSync(this.storageKey())},
    async refresh(){if(this.loading||this.busy)return;this.loading=true;this.error=''
      try{[this.wallet,this.plans]=await Promise.all([getWallet(),getRechargePlans()]);this.selected=this.selected||this.plans[0]?.id
        this.draft=uni.getStorageSync(this.storageKey())||null
        if(this.draft){this.pending=this.draft.orderId?await getRechargeOrder(this.draft.orderId):await createRecharge(this.draft)
          if(Number(this.pending.status)!==0){const paid=Number(this.pending.status)===1;this.clearDraft();if(paid)this.wallet=await getWallet();else this.error='上笔订单已过期，请重新选择档位'}
          else{this.draft.orderId=this.pending.id;uni.setStorageSync(this.storageKey(),this.draft)}}
      }catch(e){this.error=e.msg||'加载失败'}finally{this.loading=false}},
    async create(){if(this.busy||this.loading||!this.selected)return;this.busy=true;this.error=''
      try{if(!this.draft){this.draft={planId:this.selected,requestId:requestId()};uni.setStorageSync(this.storageKey(),this.draft)}
        this.pending=await createRecharge(this.draft);this.draft.orderId=this.pending.id;uni.setStorageSync(this.storageKey(),this.draft)
      }catch(e){this.error=e.msg;if(Number(e.code)===400)this.clearDraft()}finally{this.busy=false}},
    pay(){if(this.busy||!this.pending)return;uni.showModal({title:'确认模拟支付',content:'模拟支付 ¥ '+money(this.pending.amount)+'，不会扣取真实资金。',success:async r=>{if(!r.confirm||this.busy)return;this.busy=true;this.error=''
      try{const order=await simulatePayment(this.pending.id);if(Number(order.status)===1){this.clearDraft();this.wallet=await getWallet();uni.showToast({title:'模拟充值成功',icon:'none'})}else if(Number(order.status)===2){this.clearDraft();this.error='订单已过期，请重新下单'}}catch(e){this.error=e.msg||'支付结果暂未确认，请刷新订单后重试'}finally{this.busy=false}}})},
    flows(){uni.navigateTo({url:'/pages/wallet/flows'})}
  }
}
</script>
<style scoped>
.balance-card{background:#9f4434;color:#fff;border-radius:28rpx;padding:40rpx;margin-bottom:24rpx}.balance-label{opacity:.8;font-size:26rpx}.balance{display:block;font-size:64rpx;font-weight:600;margin:24rpx 0}.balance-footer{display:flex;justify-content:space-between;font-size:24rpx}.notice{display:block;color:#967449;font-size:24rpx;line-height:1.7;margin:18rpx 0}.plans{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:18rpx;margin:28rpx 0}.plan{border:2rpx solid #eaded6;border-radius:18rpx;padding:24rpx 12rpx;display:flex;flex-direction:column;align-items:center;gap:12rpx;font-size:24rpx}.selected{border-color:#b5533d;background:#fcf2e9}.plan-price{font-size:36rpx;font-weight:bold;color:#a54733}.bonus{color:#967449}.order-time{display:block;margin:16rpx 0 28rpx}
</style>
