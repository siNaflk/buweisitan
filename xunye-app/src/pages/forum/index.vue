<template><view class="m2-page"><view class="m2-heading"><view><text class="community-title">书海相遇</text><text class="m2-muted subtitle">找一本念念不忘的书，遇见同好</text></view><text class="m2-link" @click="mine">我的帖子</text></view><button class="m2-primary new-post" @click="compose">＋ 发布找书 / 阅读分享</button><ForumFeed ref="feed" /></view></template>
<script>
import ForumFeed from '../../components/ForumFeed.vue'
import { requireLogin } from '../../utils/auth'
export default {components:{ForumFeed},onShow(){this.$nextTick(()=>this.$refs.feed?.refresh())},onPullDownRefresh(){this.$refs.feed?.refresh();uni.stopPullDownRefresh()},onReachBottom(){if(this.$refs.feed?.hasMore)this.$refs.feed.load()},methods:{compose(){if(requireLogin())uni.navigateTo({url:'/pages/forum/compose'})},mine(){if(requireLogin())uni.navigateTo({url:'/pages/forum/mine'})}}}
</script>
<style scoped>.community-title{font-size:40rpx;font-weight:bold}.subtitle{display:block;margin-top:12rpx}.new-post{margin:30rpx 0}</style>
