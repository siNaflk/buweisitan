<template><view class="m2-page"><view class="m2-card"><text class="m2-heading">写给同样热爱故事的人</text><input class="m2-input" v-model="title" maxlength="100" placeholder="标题：你在找什么书？" /><textarea class="m2-textarea post-content" v-model="content" maxlength="5000" placeholder="描述记得的情节、人物，或分享你的阅读体验…" /><text class="m2-muted">{{content.length}} / 5000 · 提交后等待审核</text><view v-if="error" class="m2-error">{{error}}</view><button class="m2-primary" :loading="busy" :disabled="busy" @click="submit">提交审核</button></view></view></template>
<script>
import { addPost } from '../../api/interaction';import { requireLogin } from '../../utils/auth'
export default{data(){return{title:'',content:'',busy:false,error:''}},methods:{async submit(){if(this.busy||!requireLogin())return;if(!this.title.trim()||!this.content.trim()){this.error='请填写标题与正文';return}this.busy=true;try{await addPost({title:this.title.trim(),content:this.content.trim()});uni.showToast({title:'已提交审核',icon:'none'});uni.redirectTo({url:'/pages/forum/mine'})}catch(e){this.error=e.msg}finally{this.busy=false}}}}
</script>
<style scoped>.post-content{height:400rpx}.m2-primary{margin-top:32rpx}</style>
