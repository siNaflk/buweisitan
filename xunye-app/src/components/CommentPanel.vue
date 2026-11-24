<template>
  <view class="m2-card">
    <view class="m2-heading"><text>{{chapterId ? '本章讨论' : '书友评价'}} · {{total}}</text><text class="m2-link" @click="refresh">刷新</text></view>
    <textarea class="m2-textarea" v-model="content" maxlength="500" :placeholder="chapterId ? '分享这一章的感受…' : '写下你的阅读感受…'" />
    <view class="m2-row"><text class="m2-muted">{{content.length}} / 500</text><button size="mini" class="m2-primary" :loading="posting" :disabled="posting" @click="publish">发表</button></view>
    <text class="m2-error" v-if="error">{{error}}</text>
    <view v-for="item in items" :key="item.id" class="comment">
      <view class="m2-heading"><text>{{item.nickname || '书友'}}</text><text class="m2-muted">{{date(item.createTime)}}</text></view>
      <text class="m2-body">{{item.content}}</text>
      <view class="m2-row"><text class="m2-link like-button" :class="{liked:item.liked}" @click="toggleLike(item)">{{item.liked?'已赞':'点赞'}} {{item.likeCount || 0}}</text><text v-if="item.mine" class="m2-muted" @click="remove(item)">删除</text></view>
    </view>
    <view class="m2-muted m2-center" v-if="!items.length && !loading && !error">还没有讨论，来写第一条吧</view>
    <view class="m2-link m2-center" v-if="hasMore || loading" @click="load">{{loading?'加载中…':'加载更多'}}</view>
  </view>
</template>
<script>
import { getComments, addComment, deleteComment, likeComment } from '../api/interaction'
import { requireLogin } from '../utils/auth'
export default {
  props:{bookId:{type:[String,Number],required:true},chapterId:{type:[String,Number],default:null}},
  data(){return{items:[],page:1,total:0,content:'',loading:false,posting:false,error:'',version:0,likeBusy:{}}},
  computed:{hasMore(){return this.items.length<this.total}},
  watch:{bookId(){this.refresh()},chapterId(){this.refresh()}},
  mounted(){this.refresh()},
  methods:{
    date(v){return (v||'').replace('T',' ').slice(0,16)},
    refresh(){this.version++;this.items=[];this.page=1;this.total=0;this.loading=false;this.error='';this.load()},
    async load(){if(this.loading)return;this.loading=true;const version=this.version
      try{const data=await getComments({bookId:this.bookId,...(this.chapterId?{chapterId:this.chapterId}:{}),pageNum:this.page,pageSize:10});if(version!==this.version)return;this.items.push(...data.records);this.total=Number(data.total);this.page++}
      catch(e){if(version===this.version)this.error=e.msg}finally{if(version===this.version)this.loading=false}},
    async publish(){if(this.posting || !requireLogin())return;if(!this.content.trim())return;this.posting=true;this.error=''
      try{await addComment({bookId:this.bookId,chapterId:this.chapterId,content:this.content.trim()});this.content='';this.refresh()}
      catch(e){this.error=e.msg}finally{this.posting=false}},
    async toggleLike(item){if(this.likeBusy[item.id] || !requireLogin())return;this.likeBusy[item.id]=true
      try{item.likeCount=await likeComment(item.id,item.liked);item.liked=!item.liked}catch(e){uni.showToast({title:e.msg,icon:'none'})}finally{this.likeBusy[item.id]=false}},
    remove(item){uni.showModal({title:'删除评论',content:'确定删除这条评论吗？',success:async r=>{if(!r.confirm)return;try{await deleteComment(item.id);this.refresh()}catch(e){this.error=e.msg}}})}
  }
}
</script>
<style scoped>.comment{border-top:1rpx solid #eee;padding:24rpx 0}.m2-heading{font-size:26rpx}.like-button{padding:10rpx 20rpx;border:1rpx solid #eaded6;border-radius:24rpx}.liked{background:#fcf2e9;font-weight:600}</style>
