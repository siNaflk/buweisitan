<template>
  <view class="page">
    <!-- 工具栏：正/倒序切换 -->
    <view class="toolbar">
      <text class="toolbar-text">共 {{ chapters.length }} 章</text>
      <text class="toolbar-btn" @click="toggleOrder">
        {{ reverse ? '倒序' : '正序' }}
      </text>
    </view>

    <view class="chapter-list">
      <view
        v-for="(ch, index) in displayChapters"
        :key="ch.id"
        class="chapter-item"
        @click="goReader(ch)"
      >
        <view class="chapter-info">
          <text class="chapter-title ellipsis-1">{{ ch.title }}</text>
          <text class="chapter-meta">{{ ch.wordCount }}字 · {{ ch.sort }}</text>
        </view>
        <text v-if="ch.vipLibrary" class="chapter-tag paid">会员专属</text>
        <text v-else-if="Number(ch.isFree) === 1" class="chapter-tag free">免费</text>
        <text v-else class="chapter-tag paid">{{ fenToYuan(ch.price) }}元</text>
      </view>
    </view>

    <view v-if="loading" class="tip-block">加载中...</view>
    <view v-else-if="chapters.length === 0" class="tip-block">{{ errorMsg || '暂无章节' }}</view>
  </view>
</template>

<script>
import { getChapterList } from '../../api/book'
import { fenToYuan } from '../../utils/text'

export default {
  data() {
    return {
      bookId: null,
      chapters: [],
      reverse: false,
      loading: true,
      errorMsg: '',
      fenToYuan
    }
  },
  computed: {
    displayChapters() {
      return this.reverse ? [...this.chapters].reverse() : this.chapters
    }
  },
  onLoad(options) {
    this.bookId = options.bookId
    if (options.title) {
      uni.setNavigationBarTitle({ title: decodeURIComponent(options.title) })
    }
    this.loadChapters()
  },
  methods: {
    async loadChapters() {
      this.errorMsg = ''
      try {
        this.chapters = await getChapterList(this.bookId)
      } catch (e) {
        this.errorMsg = e.msg || '加载失败'
      } finally {
        this.loading = false
      }
    },
    toggleOrder() {
      this.reverse = !this.reverse
    },
    goReader(ch) {
      uni.navigateTo({
        url: `/pages/reader/index?bookId=${this.bookId}&chapterId=${ch.id}`
      })
    }
  }
}
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  padding: 24rpx;
  background: #fff;
}
.toolbar-text {
  font-size: 26rpx;
  color: #999;
}
.toolbar-btn {
  font-size: 26rpx;
  color: #c2543a;
}
.chapter-list {
  margin-top: 16rpx;
  background: #fff;
}
.chapter-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 26rpx 24rpx;
  border-bottom: 1rpx solid #f0f0f0;
}
.chapter-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}
.chapter-title {
  font-size: 28rpx;
  color: #333;
}
.chapter-meta {
  font-size: 22rpx;
  color: #bbb;
}
.chapter-tag {
  font-size: 22rpx;
  flex-shrink: 0;
  margin-left: 16rpx;
}
.chapter-tag.free {
  color: #4caf50;
}
.chapter-tag.paid {
  color: #d4a24e;
}
</style>
