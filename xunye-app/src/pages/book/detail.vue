<template>
  <view class="page" v-if="book">
    <!-- 书籍头部 -->
    <view class="header">
      <image
        class="cover"
        :src="book.coverUrl || DEFAULT_COVER"
        mode="aspectFill"
      />
      <view class="info">
        <text class="title">{{ book.title }}</text>
        <text class="author">{{ book.authorName }}</text>
        <view class="tags">
          <text class="tag vip" v-if="Number(book.isVipLibrary) === 1">会员专属</text>
          <text class="tag" :class="Number(book.status) === 1 ? 'grey' : 'red'">
            {{ Number(book.status) === 1 ? '完本' : '连载中' }}
          </text>
        </view>
        <view class="stats">
          <view class="stat">
            <text class="stat-num">{{ formatWordCount(book.wordCount) }}</text>
            <text class="stat-label">字数</text>
          </view>
          <view class="stat">
            <text class="stat-num">{{ book.chapterCount || 0 }}</text>
            <text class="stat-label">章节</text>
          </view>
          <view class="stat">
            <text class="stat-num">{{ Number(book.collectCount) }}</text>
            <text class="stat-label">收藏</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 操作按钮 -->
    <view class="actions">
      <view class="btn primary" @click="goChapters">开始阅读</view>
      <view class="btn normal" :class="{ collected: isCollected }" @click="toggleCollect">
        {{ isCollected ? '✓ 已收藏' : '+ 收藏' }}
      </view>
    </view>

    <view class="block chapters-entry" @click="tip"><text class="block-title">支持作者</text><text class="arrow">打赏 ›</text></view>

    <!-- 简介 -->
    <view class="block">
      <view class="block-title">简介</view>
      <text class="intro">{{ book.intro || '暂无简介' }}</text>
      <view class="price-row" v-if="Number(book.chapterPrice) > 0">
        <text class="price-label">章节定价</text>
        <text class="price-value">{{ fenToYuan(book.chapterPrice) }} 元/章</text>
      </view>
    </view>

    <!-- 章节预览入口 -->
    <view class="block chapters-entry" @click="goChapters">
      <text class="block-title">查看目录（{{ book.chapterCount || 0 }}章）</text>
      <text class="arrow">›</text>
    </view>

    <CommentPanel :bookId="bookId" ref="comments" />

    <!-- 相关推荐（同分类随机） -->
    <view class="block" v-if="related.length">
      <view class="block-title">相关推荐</view>
      <scroll-view scroll-x class="related-scroll">
        <view class="related-list">
          <view
            v-for="item in related"
            :key="item.id"
            class="related-card"
            @click="goRelated(item)"
          >
            <image
              class="related-cover"
              :src="item.coverUrl || DEFAULT_COVER"
              mode="aspectFill"
            />
            <text class="related-title ellipsis-2">{{ item.title }}</text>
            <text class="related-author ellipsis-1">{{ item.authorName }}</text>
          </view>
        </view>
      </scroll-view>
    </view>
  </view>
  <view v-else class="tip-block">{{ errorMsg || '加载中...' }}</view>
</template>

<script>
import { getBookDetail, getRelatedBooks } from '../../api/book'
import { addToBookshelf, removeFromBookshelf } from '../../api/user'
import { DEFAULT_COVER } from '../../utils/cover'
import { formatWordCount, fenToYuan } from '../../utils/text'
import { requireLogin } from '../../utils/auth'
import CommentPanel from '../../components/CommentPanel.vue'

export default {
  components: { CommentPanel },
  data() {
    return {
      bookId: null,
      book: null,
      related: [],
      errorMsg: '',
      DEFAULT_COVER,
      formatWordCount,
      fenToYuan
    }
  },
  computed: {
    isCollected() {
      return !!this.book.isCollected
    }
  },
  onLoad(options) {
    this.bookId = options.bookId
    this.loadDetail()
  },
  onShow() { if(this.book) { this.loadDetail(); this.$refs.comments?.refresh() } },
  methods: {
    async loadDetail() {
      this.errorMsg = ''
      try {
        this.book = await getBookDetail(this.bookId)
        uni.setNavigationBarTitle({ title: this.book.title })
        this.loadRelated()
      } catch (e) {
        this.errorMsg = e.msg || '加载失败'
      }
    },
    async loadRelated() {
      try {
        this.related = await getRelatedBooks(this.bookId)
      } catch (e) {
        this.related = []
      }
    },
    goRelated(item) {
      uni.redirectTo({ url: `/pages/book/detail?bookId=${item.id}` })
    },
    async toggleCollect() {
      if (!requireLogin()) return
      try {
        if (this.isCollected) {
          await removeFromBookshelf(this.bookId)
          this.book.isCollected = false
          this.book.collectCount = Number(this.book.collectCount) - 1 + ''
          uni.showToast({ title: '已移出书架', icon: 'none' })
        } else {
          await addToBookshelf(this.bookId)
          this.book.isCollected = true
          this.book.collectCount = Number(this.book.collectCount) + 1 + ''
          uni.showToast({ title: '已加入书架', icon: 'none' })
        }
      } catch (e) {
        uni.showToast({ title: e.msg || '操作失败', icon: 'none' })
      }
    },
    tip(){if(requireLogin())uni.navigateTo({url:'/pages/wallet/tip?bookId='+this.bookId})},
    goChapters() {
      uni.navigateTo({
        url: `/pages/book/chapters?bookId=${this.bookId}&title=${encodeURIComponent(this.book.title)}`
      })
    }
  }
}
</script>

<style scoped>
.page {
  padding-bottom: 40rpx;
}
.header {
  display: flex;
  gap: 24rpx;
  padding: 32rpx 24rpx;
  background: #fff;
}
.cover {
  width: 180rpx;
  height: 240rpx;
  border-radius: 12rpx;
  background: #eee;
  flex-shrink: 0;
}
.info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12rpx;
  min-width: 0;
}
.title {
  font-size: 36rpx;
  font-weight: bold;
}
.author {
  font-size: 26rpx;
  color: #666;
}
.tags {
  display: flex;
  gap: 12rpx;
}
.tag {
  font-size: 20rpx;
  padding: 4rpx 14rpx;
  border-radius: 6rpx;
  border: 1rpx solid #c2543a;
  color: #c2543a;
}
.tag.vip {
  border-color: #d4a24e;
  color: #d4a24e;
}
.tag.grey {
  border-color: #ccc;
  color: #999;
}
.tag.red {
  background: #f3e0da;
}
.stats {
  display: flex;
  gap: 48rpx;
  margin-top: auto;
}
.stat {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.stat-num {
  font-size: 30rpx;
  font-weight: bold;
  color: #333;
}
.stat-label {
  font-size: 22rpx;
  color: #999;
}
.actions {
  display: flex;
  gap: 20rpx;
  padding: 24rpx;
  background: #fff;
  margin-top: 16rpx;
}
.btn {
  flex: 1;
  height: 80rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 40rpx;
  font-size: 30rpx;
}
.btn.primary {
  background: #c2543a;
  color: #fff;
}
.btn.normal {
  border: 1rpx solid #c2543a;
  color: #c2543a;
}
.btn.normal.collected {
  border-color: #ccc;
  color: #999;
}
.block {
  margin-top: 16rpx;
  padding: 24rpx;
  background: #fff;
}
.block-title {
  font-size: 30rpx;
  font-weight: bold;
  margin-bottom: 16rpx;
}
.intro {
  font-size: 26rpx;
  color: #666;
  line-height: 1.6;
}
.price-row {
  display: flex;
  justify-content: space-between;
  margin-top: 20rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid #f0f0f0;
}
.price-label {
  font-size: 26rpx;
  color: #999;
}
.price-value {
  font-size: 26rpx;
  color: #d4a24e;
}
.chapters-entry {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.chapters-entry .block-title {
  margin-bottom: 0;
}
.arrow {
  color: #ccc;
  font-size: 36rpx;
}
.related-scroll {
  white-space: nowrap;
  width: 100%;
}
.related-list {
  display: inline-flex;
  gap: 20rpx;
}
.related-card {
  width: 150rpx;
  display: inline-flex;
  flex-direction: column;
}
.related-cover {
  width: 150rpx;
  height: 200rpx;
  border-radius: 10rpx;
  background: #eee;
}
.related-title {
  margin-top: 10rpx;
  font-size: 24rpx;
  line-height: 1.3;
  height: 62rpx;
  white-space: normal;
}
.related-author {
  margin-top: 4rpx;
  font-size: 20rpx;
  color: #999;
}
</style>
