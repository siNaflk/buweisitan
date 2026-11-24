<template>
  <view class="page">
    <!-- 搜索模式顶部输入框 -->
    <view v-if="mode === 'search'" class="search-bar">
      <input
        v-model="keyword"
        class="search-input"
        placeholder="输入书名或作者"
        confirm-type="search"
        @confirm="reload"
      />
      <text class="search-btn" @click="reload">搜索</text>
    </view>

    <!-- 分类模式标题 -->
    <view v-else class="page-title">{{ categoryName || '全部作品' }}</view>

    <!-- 排序 tab（仅分类模式） -->
    <view v-if="mode !== 'search'" class="sort-tabs">
      <view
        v-for="tab in sortTabs"
        :key="tab.value"
        class="sort-tab"
        :class="{ active: sort === tab.value }"
        @click="switchSort(tab.value)"
      >
        <text>{{ tab.label }}</text>
      </view>
    </view>

    <!-- 书籍列表 -->
    <view class="book-list">
      <view
        v-for="book in books"
        :key="book.id"
        class="book-item"
        @click="goDetail(book)"
      >
        <image
          class="item-cover"
          :src="book.coverUrl || DEFAULT_COVER"
          mode="aspectFill"
        />
        <view class="item-info">
          <text class="item-title ellipsis-1">{{ book.title }}</text>
          <text class="item-author ellipsis-1">{{ book.authorName }} · {{ categoryName(book) }}</text>
          <text class="item-intro ellipsis-2">{{ book.intro || '暂无简介' }}</text>
          <view class="item-meta">
            <text class="meta-tag" v-if="Number(book.isVipLibrary) === 1">会员书库</text>
            <text class="meta-tag grey" v-else-if="Number(book.status) === 1">完本</text>
            <text class="meta-text">{{ formatWordCount(book.wordCount) }}字</text>
            <text class="meta-text">{{ Number(book.visitCount) }}点击</text>
          </view>
        </view>
      </view>
    </view>

    <view v-if="loading" class="tip-block">加载中...</view>
    <view v-else-if="books.length === 0" class="tip-block">
      {{ mode === 'search' && !searched ? '输入关键词开始搜索' : '暂无更多书籍' }}
    </view>
    <view v-else-if="finished" class="tip-block">没有更多了</view>
    <view v-if="errorMsg" class="tip-block">{{ errorMsg }}</view>
  </view>
</template>

<script>
import { getBookList, searchBooks, getCategoryList } from '../../api/book'
import { DEFAULT_COVER } from '../../utils/cover'
import { formatWordCount } from '../../utils/text'

export default {
  data() {
    return {
      mode: 'category', // category | search
      categoryId: null,
      categoryName: '',
      keyword: '',
      searched: false,
      sort: 'visit',
      sortTabs: [
        { value: 'visit', label: '最热' },
        { value: 'newest', label: '最新' },
        { value: 'update', label: '更新' }
      ],
      books: [],
      pageNum: 1,
      pageSize: 10,
      total: 0,
      loading: false,
      finished: false,
      errorMsg: '',
      categoryNameMap: {},
      DEFAULT_COVER,
      formatWordCount
    }
  },
  onLoad(options) {
    if (options.mode === 'search') {
      this.mode = 'search'
      uni.setNavigationBarTitle({ title: '搜索' })
    } else {
      this.categoryId = options.categoryId || null
      this.categoryName = decodeURIComponent(options.categoryName || '')
      uni.setNavigationBarTitle({ title: this.categoryName || '全部作品' })
    }
    // 拉分类映射，列表项里展示分类名
    getCategoryList().then((cats) => {
      const map = {}
      cats.forEach((c) => { map[c.id] = c.name })
      this.categoryNameMap = map
    }).catch(() => {})
    this.reload()
  },
  onReachBottom() {
    if (!this.loading && !this.finished) {
      this.loadPage()
    }
  },
  methods: {
    categoryName(book) {
      return this.categoryNameMap[book.categoryId] || ''
    },
    switchSort(sort) {
      this.sort = sort
      this.reload()
    },
    reload() {
      this.books = []
      this.pageNum = 1
      this.finished = false
      this.searched = !!this.keyword
      this.loadPage()
    },
    async loadPage() {
      this.loading = true
      this.errorMsg = ''
      try {
        let page
        if (this.mode === 'search') {
          if (!this.keyword) {
            this.loading = false
            return
          }
          page = await searchBooks(this.keyword, this.pageNum, this.pageSize)
        } else {
          page = await getBookList({
            categoryId: this.categoryId,
            sort: this.sort,
            pageNum: this.pageNum,
            pageSize: this.pageSize
          })
        }
        // 后端 Page 结构 { records, total, ... }，Long 已序列化为字符串
        const records = page.records || []
        this.books = this.books.concat(records)
        this.total = Number(page.total || 0)
        this.finished = this.books.length >= this.total || records.length === 0
        this.pageNum += 1
      } catch (e) {
        this.errorMsg = e.msg || '加载失败'
      } finally {
        this.loading = false
      }
    },
    goDetail(book) {
      uni.navigateTo({ url: `/pages/book/detail?bookId=${book.id}` })
    }
  }
}
</script>

<style scoped>
.page {
  padding-bottom: 40rpx;
}
.search-bar {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 20rpx 24rpx;
  background: #fff;
}
.search-input {
  flex: 1;
  height: 68rpx;
  padding: 0 24rpx;
  background: #f2f2f4;
  border-radius: 34rpx;
  font-size: 26rpx;
}
.search-btn {
  color: #c2543a;
  font-size: 28rpx;
}
.page-title {
  padding: 24rpx;
  font-size: 34rpx;
  font-weight: bold;
  background: #fff;
}
.sort-tabs {
  display: flex;
  background: #fff;
  padding: 0 24rpx 20rpx;
  gap: 40rpx;
}
.sort-tab {
  font-size: 28rpx;
  color: #666;
  padding-bottom: 8rpx;
}
.sort-tab.active {
  color: #c2543a;
  font-weight: bold;
  border-bottom: 4rpx solid #c2543a;
}
.book-list {
  margin-top: 16rpx;
  background: #fff;
}
.book-item {
  display: flex;
  gap: 20rpx;
  padding: 24rpx;
  border-bottom: 1rpx solid #f0f0f0;
}
.item-cover {
  width: 140rpx;
  height: 186rpx;
  border-radius: 10rpx;
  background: #eee;
  flex-shrink: 0;
}
.item-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10rpx;
  min-width: 0;
}
.item-title {
  font-size: 30rpx;
  font-weight: bold;
}
.item-author {
  font-size: 24rpx;
  color: #999;
}
.item-intro {
  font-size: 24rpx;
  color: #666;
  line-height: 1.4;
}
.item-meta {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-top: auto;
}
.meta-tag {
  font-size: 20rpx;
  color: #d4a24e;
  border: 1rpx solid #d4a24e;
  padding: 2rpx 12rpx;
  border-radius: 6rpx;
}
.meta-tag.grey {
  color: #999;
  border-color: #ccc;
}
.meta-text {
  font-size: 22rpx;
  color: #999;
}
</style>
