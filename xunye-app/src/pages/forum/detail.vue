<template>
  <view class="m2-page">
    <view v-if="post" class="m2-card"><view class="m2-heading"><text class="post-title">{{post.title}}</text><text v-if="post.mine" class="m2-badge">{{Number(post.status)===1?'已封帖':audit(post.auditStatus)}}</text></view><text class="m2-muted">{{post.nickname}} · {{date(post.createTime)}}</text><text class="m2-body">{{post.content}}</text><view class="m2-row"><text class="m2-link" @click="togglePostLike">{{post.liked?'已赞':'点赞'}} {{post.likeCount || 0}}</text><text v-if="post.mine" class="m2-link" @click="removePost">删除帖子</text></view></view>
    <view v-if="error" class="m2-error" @click="refresh">{{error}}（点击重试）</view>
    <view v-if="post" class="m2-card"><text class="m2-heading">书友回复 · {{post.replyCount}}</text>
      <template v-if="canReply"><textarea class="m2-textarea" v-model="content" maxlength="500" placeholder="留下你的线索与想法…" /><button size="mini" class="m2-primary" :loading="posting" :disabled="posting" @click="submit">回复并提交审核</button></template>
      <text v-else class="m2-muted">帖子暂不能回复</text>
      <view v-for="item in items" :key="item.id" class="reply"><view class="m2-heading"><text>{{item.nickname}}</text><text class="m2-muted">{{date(item.createTime)}}</text></view><text v-if="item.mine" class="m2-badge">{{audit(item.auditStatus)}}</text><text class="m2-body">{{item.content}}</text><view class="m2-row"><text v-if="Number(item.auditStatus)===1 && canReply" class="m2-link" @click="toggleReplyLike(item)">{{item.liked?'已赞':'点赞'}} {{item.likeCount || 0}}</text><text v-if="item.mine" class="m2-link" @click="removeReply(item)">删除</text></view></view>
      <view v-if="!items.length&&!loading" class="m2-empty">还没有公开回复</view><view v-if="hasMore || loading" class="m2-link m2-center" @click="loadReplies">{{loading?'加载中…':'加载更多'}}</view>
    </view>
  </view>
</template>
<script>
import { getPost, getReplies, addReply, deletePost, deleteReply, likePost, likeReply } from '../../api/interaction'
import { requireLogin } from '../../utils/auth'
export default{
  data(){return{id:null,post:null,items:[],page:1,total:0,loading:false,posting:false,content:'',error:'',likeBusy:{}}},
  computed:{canReply(){return this.post&&Number(this.post.auditStatus)===1&&Number(this.post.status)===0},hasMore(){return this.items.length<this.total}},
  onLoad(options){this.id=options.id},onShow(){this.refresh()},
  methods:{
    async togglePostLike(){if(this.likeBusy.post||!requireLogin())return;if(!this.canReply){uni.showToast({title:'帖子暂未公开，不能点赞',icon:'none'});return}this.likeBusy.post=true;try{this.post.likeCount=await likePost(this.id,this.post.liked);this.post.liked=!this.post.liked}catch(e){uni.showToast({title:e.msg,icon:'none'})}finally{this.likeBusy.post=false}},
    async toggleReplyLike(item){if(this.likeBusy[item.id]||!requireLogin())return;this.likeBusy[item.id]=true;try{item.likeCount=await likeReply(item.id,item.liked);item.liked=!item.liked}catch(e){uni.showToast({title:e.msg,icon:'none'})}finally{this.likeBusy[item.id]=false}},
    audit(v){return ['待审核','已通过','未通过'][Number(v)]||'待审核'},date(v){return(v||'').replace('T',' ').slice(0,16)},
    async refresh(){this.error='';try{this.post=await getPost(this.id);this.items=[];this.page=1;this.total=0;await this.loadReplies()}catch(e){this.post=null;this.items=[];this.error=e.msg}},
    async loadReplies(){if(this.loading)return;this.loading=true;try{const d=await getReplies(this.id,{pageNum:this.page,pageSize:20});this.items.push(...d.records);this.total=Number(d.total);this.page++}catch(e){this.error=e.msg}finally{this.loading=false}},
    async submit(){if(this.posting||!requireLogin()||!this.content.trim())return;this.posting=true;try{await addReply(this.id,this.content.trim());this.content='';uni.showToast({title:'回复已提交审核',icon:'none'});await this.refresh()}catch(e){this.error=e.msg}finally{this.posting=false}},
    removePost(){uni.showModal({title:'删除帖子',content:'确定删除这篇帖子吗？',success:async r=>{if(!r.confirm)return;try{await deletePost(this.id);uni.navigateBack()}catch(e){this.error=e.msg}}})},
    removeReply(item){uni.showModal({title:'删除回复',content:'确定删除这条回复吗？',success:async r=>{if(!r.confirm)return;try{await deleteReply(item.id);await this.refresh()}catch(e){this.error=e.msg}}})}
  }
}
</script>
<style scoped>.post-title{font-size:36rpx;font-weight:bold;flex:1}.reply{padding:24rpx 0;border-top:1rpx solid #eee;margin-top:24rpx}</style>
