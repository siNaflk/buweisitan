<template>
  <view>
    <view v-for="item in items" :key="item.id" class="m2-card" @click="open(item)">
      <view class="m2-heading"><text>{{item.title}}</text><text v-if="mine" class="m2-badge">{{Number(item.status)===1?'已封帖':audit(item.auditStatus)}}</text></view>
      <text class="m2-body preview">{{item.content}}</text>
      <view class="m2-row m2-muted"><text>{{item.nickname}} · {{date(item.createTime)}}</text><view class="feed-actions"><text :class="{liked:item.liked}" @click.stop="toggleLike(item)">{{item.liked?'已赞':'点赞'}} {{item.likeCount || 0}}</text><text>{{item.replyCount}} 回复</text></view></view>
    </view>
    <view v-if="error" class="m2-error" @click="load">{{error}}（点击重试）</view>
    <view v-if="!items.length && !loading && !error" class="m2-empty">{{mine?'还没有帖子，写下你的第一条找书需求吧':'这里还很安静，来分享一本好书吧'}}</view>
    <view v-if="loading || hasMore" class="m2-link m2-center" @click="load">{{loading?'加载中…':'加载更多'}}</view>
  </view>
</template>
<script>
import { getPosts, likePost } from '../api/interaction'
import {requireLogin} from '../utils/auth'
export default {
  props:{mine:{type:Boolean,default:false}},
  data(){return{items:[],page:1,total:0,loading:false,error:'',version:0,likeBusy:{}}},
  computed:{hasMore(){return this.items.length<this.total}},
  methods:{
    async toggleLike(item){if(this.likeBusy[item.id]||!requireLogin())return;if(Number(item.auditStatus)!==1||Number(item.status)!==0){uni.showToast({title:'帖子暂未公开，不能点赞',icon:'none'});return}this.likeBusy[item.id]=true;try{item.likeCount=await likePost(item.id,item.liked);item.liked=!item.liked}catch(e){uni.showToast({title:e.msg,icon:'none'})}finally{this.likeBusy[item.id]=false}},
    audit(v){return ['待审核','已通过','未通过'][Number(v)] || '待审核'},date(v){return(v||'').replace('T',' ').slice(0,16)},
    open(item){uni.navigateTo({url:'/pages/forum/detail?id='+item.id})},
    refresh(){this.version++;this.items=[];this.page=1;this.total=0;this.loading=false;this.load()},
    async load(){if(this.loading)return;this.loading=true;this.error='';const version=this.version
      try{const data=await getPosts(this.mine,{pageNum:this.page,pageSize:10});if(version!==this.version)return;this.items.push(...data.records);this.total=Number(data.total);this.page++}
      catch(e){if(version===this.version)this.error=e.msg}finally{if(version===this.version)this.loading=false}}
  }
}
</script>
<style scoped>.preview{display:-webkit-box;-webkit-line-clamp:3;-webkit-box-orient:vertical;overflow:hidden}.feed-actions{display:flex;gap:24rpx}.liked{color:#b5533d;font-weight:600}</style>
