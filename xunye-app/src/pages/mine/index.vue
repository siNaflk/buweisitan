<template>
  <view class="m2-page">
    <view class="m2-card profile" @click="openLogin">
      <view class="avatar">{{ user ? (user.nickname || '书友').slice(0,1) : '寻' }}</view>
      <view><text class="nickname">{{ user ? user.nickname : '登录寻页' }}</text><text class="m2-muted profile-sub">{{ user ? user.maskedPhone : '同步书架，与书友交流' }}</text></view>
    </view>
    <view class="m2-card">
      <view class="menu" @click="shelf">我的书架 <text>›</text></view>
      <view class="menu" @click="forum">找书社区 <text>›</text></view>
      <view class="menu" @click="notifications">我的消息 <text class="m2-muted">{{unread?unread+'条未读':''}} ›</text></view>
      <view class="menu" @click="myPosts">我的帖子与审核状态 <text>›</text></view>
      <view class="menu" @click="wallet">充值与钱包 <text class="m2-muted">›</text></view>
      <view class="menu" @click="vip">VIP会员 <text class="m2-muted">{{vipStatus.active?'已开通':'查看权益'}} ›</text></view>
      <view class="menu" @click="future('作家专区', 'M5')">作家专区 <text class="m2-muted">M5 ›</text></view>
    </view>
    <button v-if="user" class="m2-secondary" :loading="busy" :disabled="busy" @click="signOut">退出登录</button>
    <text v-if="error" class="m2-error">{{error}}</text>
  </view>
</template>
<script>
import { getMe, logout } from '../../api/account'
import {getVipStatus,getUnreadCount} from '../../api/vip'
import { tokenStore } from '../../utils/request'
import { requireLogin } from '../../utils/auth'
export default {
  data(){return{user:null,busy:false,error:'',vipStatus:{active:false},unread:0}},
  async onShow(){
    this.user=null;this.error='';this.vipStatus={active:false};this.unread=0
    if(!tokenStore.get())return
    this.user=uni.getStorageSync('userInfo') || null
    try{this.user=await getMe();uni.setStorageSync('userInfo',this.user);[this.vipStatus,this.unread]=await Promise.all([getVipStatus(),getUnreadCount()]);this.unread=Number(this.unread)||0}catch(e){this.error=e.msg;if(!tokenStore.get())this.user=null}
  },
  methods:{
    openLogin(){if(!this.user)uni.navigateTo({url:'/pages/auth/index'})},
    shelf(){uni.switchTab({url:'/pages/shelf/index'})},
    forum(){uni.switchTab({url:'/pages/forum/index'})},
    wallet(){if(requireLogin())uni.navigateTo({url:'/pages/wallet/index'})},
    vip(){uni.navigateTo({url:'/pages/vip/index'})},
    notifications(){if(requireLogin())uni.navigateTo({url:'/pages/notifications/index'})},
    myPosts(){if(requireLogin())uni.navigateTo({url:'/pages/forum/mine'})},
    future(name,m){uni.showToast({title:name+'将在'+m+'开放',icon:'none'})},
    async signOut(){
      if(this.busy)return;this.busy=true;this.error=''
      try{await logout();tokenStore.clear();uni.removeStorageSync('userInfo');this.user=null;uni.showToast({title:'已退出',icon:'none'})}
      catch(e){if(!tokenStore.get())this.user=null;this.error=e.msg || '退出失败，请重试'}finally{this.busy=false}
    }
  }
}
</script>
<style scoped>.profile{display:flex;align-items:center;gap:24rpx;padding:40rpx 24rpx}.avatar{width:100rpx;height:100rpx;border-radius:50%;background:#c2543a;color:white;display:flex;align-items:center;justify-content:center;font-size:40rpx}.nickname{font-size:36rpx;font-weight:bold}.profile-sub{display:block;margin-top:12rpx}.menu{display:flex;justify-content:space-between;padding:28rpx 0;border-bottom:1rpx solid #f3f3f3}</style>
