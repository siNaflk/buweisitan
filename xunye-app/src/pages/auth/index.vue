<template>
  <view class="m2-page auth-page">
    <text class="auth-logo">寻页</text>
    <text class="auth-caption">让故事有回响，让书友相遇</text>
    <view class="m2-card">
      <view class="m2-tabs"><text :class="{active:!registerMode}" @click="switchMode(false)">欢迎回来</text><text :class="{active:registerMode}" @click="switchMode(true)">加入寻页</text></view>
      <input class="m2-input" v-model="phone" type="number" maxlength="11" placeholder="手机号" />
      <input v-if="registerMode" class="m2-input" v-model="nickname" maxlength="30" placeholder="给自己取个昵称（可选）" />
      <input class="m2-input" v-model="password" password maxlength="64" placeholder="密码，8至64位" />
      <input v-if="registerMode" class="m2-input" v-model="confirmation" password maxlength="64" placeholder="再次输入密码" />
      <view class="captcha-row"><input class="m2-input" v-model="captcha" type="number" maxlength="4" placeholder="图形验证码" /><image v-if="captchaImage" class="captcha-img" :src="captchaImage" @click="refreshCaptcha" /><text v-else @click="refreshCaptcha">加载验证码</text></view>
      <text class="m2-muted">点击图片可换一张，验证码5分钟内一次有效</text>
      <view v-if="error" class="m2-error">{{ error }}</view>
      <button class="m2-primary" :loading="busy" :disabled="busy || !captchaId" @click="submit">{{ registerMode ? '注册并登录' : '登录' }}</button>
    </view>
    <text class="m2-muted">手机号用于账号登录，本阶段不发送短信</text>
  </view>
</template>
<script>
import { getCaptcha, login, register } from '../../api/account'
import { tokenStore } from '../../utils/request'
export default {
  data() { return { registerMode:false, phone:'', password:'', confirmation:'', nickname:'', captcha:'', captchaId:'', captchaImage:'', busy:false, error:'', captchaVersion:0 } },
  onLoad() { this.refreshCaptcha() },
  methods: {
    switchMode(mode) { if(this.busy)return; this.registerMode=mode; this.error=''; this.refreshCaptcha() },
    async refreshCaptcha() {
      const version=++this.captchaVersion
      this.captchaId=''; this.captcha=''
      try { const data=await getCaptcha(); if(version!==this.captchaVersion)return; this.captchaId=data.captchaId; this.captchaImage=data.image }
      catch(e) { this.error=e.msg || '验证码加载失败，请重试' }
    },
    async submit() {
      if(this.busy)return
      this.error=''
      if(!/^1[3-9]\d{9}$/.test(this.phone)){this.error='请输入正确的手机号';return}
      if(this.password.length<8 || this.password.length>64){this.error='密码须为8至64位';return}
      if(this.registerMode && this.password!==this.confirmation){this.error='两次密码不一致';return}
      if(!/^\d{4}$/.test(this.captcha)){this.error='请输入4位图形验证码';return}
      this.busy=true
      try {
        const data=await (this.registerMode?register:login)({phone:this.phone,password:this.password,nickname:this.nickname,captchaId:this.captchaId,captcha:this.captcha})
        tokenStore.set(data.token); uni.setStorageSync('userInfo',data.user)
        uni.showToast({title:'登录成功',icon:'success'})
        const pages=getCurrentPages()
        if(pages.length>1)uni.navigateBack(); else uni.switchTab({url:'/pages/mine/index'})
      } catch(e) { this.error=e.msg || '操作失败'; await this.refreshCaptcha() }
      finally { this.busy=false }
    }
  }
}
</script>
<style scoped>
.auth-page{padding-top:90rpx}.auth-logo{display:block;font-size:64rpx;font-weight:bold;color:#c2543a;letter-spacing:12rpx}.auth-caption{display:block;color:#999;margin:20rpx 0 50rpx}.captcha-row{display:flex;align-items:center;gap:20rpx}.captcha-row .m2-input{flex:1;min-width:0}.captcha-img{width:200rpx;height:72rpx}.m2-primary{margin-top:30rpx}
</style>
