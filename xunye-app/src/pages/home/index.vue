<template>
  <view class="home">
    <view class="top-bar">
      <view class="search-input" @click="goSearch">
        <view class="search-icon" aria-hidden="true" />
        <text class="search-placeholder">搜索书名或作者</text>
      </view>
      <view class="category-btn" @click="showCategoryPopup = true">
        <view class="category-icon"><view /><view /><view /><view /></view><text>分类</text>
      </view>
    </view>

    <!-- 三榜共用一张卡片，每个榜单只显示前三本。 -->
    <view class="rank-card">
      <view class="rank-tabs">
        <view v-for="tab in rankTabs" :key="tab.key" class="rank-tab" :class="{ active: activeRank === tab.key }" @click="activeRank = tab.key">{{ tab.title }}</view>
        <text class="rank-hint">TOP 3</text>
      </view>
      <view v-if="loading" class="rank-placeholder">正在整理好故事…</view>
      <view v-else-if="errorMsg" class="rank-placeholder" @click="loadData">{{ errorMsg }}，点击重试</view>
      <view v-else-if="!activeBooks.length" class="rank-placeholder">这个榜单还没有作品</view>
      <view v-else class="rank-books">
        <view v-for="(book, index) in activeBooks" :key="book.id" class="rank-book" @click="goDetail(book)">
          <view class="rank-cover-wrap">
            <image class="rank-cover" :src="book.coverUrl || DEFAULT_COVER" mode="aspectFill" />
            <text class="rank-number" :class="'rank-number-' + index">{{ index + 1 }}</text>
          </view>
          <text class="rank-title ellipsis-1">{{ book.title }}</text>
          <text class="rank-author ellipsis-1">{{ book.authorName }}</text>
        </view>
      </view>
    </view>

    <view class="recommend-section">
      <view class="recommend-heading">
        <view>
          <view class="recommend-title-row"><text class="section-title">为你推荐</text><text class="recommend-badge">{{ personalized ? '猜你喜欢' : '书城精选' }}</text></view>
          <text class="section-subtitle">{{ personalized ? '从阅读偏好出发，发现下一本心动好书' : '翻开新故事，总有一本与你相遇' }}</text>
        </view>
        <view v-if="recommendations.length > 1" class="refresh-btn" @click="refreshRecommendations">换一换</view>
      </view>
      <view v-if="recommendLoading" class="recommend-empty">正在为你寻找好书…</view>
      <view v-else-if="!recommendations.length" class="recommend-empty"><text>好故事正在路上</text><text class="empty-caption">可以先从分类中寻找感兴趣的作品</text></view>
      <view v-else class="recommend-list">
        <view v-for="book in recommendations" :key="book.id" class="recommend-book" @click="goDetail(book)">
          <image class="recommend-cover" :src="book.coverUrl || DEFAULT_COVER" mode="aspectFill" />
          <view class="recommend-info">
            <text class="recommend-name ellipsis-1">{{ book.title }}</text>
            <text class="recommend-intro ellipsis-2">{{ book.intro || '把日常留在书外，去故事里看看。' }}</text>
            <view class="recommend-meta"><text class="recommend-author ellipsis-1">{{ book.authorName || '寻页作者' }}</text><text class="book-status">{{ Number(book.status) === 1 ? '已完结' : '连载中' }}</text></view>
          </view>
        </view>
      </view>
    </view>

    <view v-if="showCategoryPopup" class="popup-mask" @click="showCategoryPopup = false">
      <view class="popup-panel" @click.stop>
        <view class="popup-header"><text class="popup-title">选择分类</text><text class="popup-close" @click="showCategoryPopup = false">×</text></view>
        <view v-if="categories.length" class="popup-grid"><view v-for="cat in categories" :key="cat.id" class="popup-cat" @click="goCategory(cat)">{{ cat.name }}</view></view>
        <view v-else class="category-empty" @click="loadCategories">暂无分类，点击重试</view>
      </view>
    </view>
  </view>
</template>

<script>
import { getHomeBooks, getCategoryList } from '../../api/book'
import { getPersonalizedBooks, PERSONALIZED_RECOMMENDATION_ENABLED } from '../../api/recommendation'
import { tokenStore } from '../../utils/request'
import { DEFAULT_COVER } from '../../utils/cover'

export default {
  data() {
    return {
      categories: [], hot: [], newest: [], vipLibrary: [], recommendations: [],
      activeRank: 'hot', showCategoryPopup: false, loading: true, errorMsg: '',
      recommendLoading: false, personalized: false, recommendationOffset: 0,
      recommendationVersion: 0, sessionToken: '', DEFAULT_COVER
    }
  },
  computed: {
    rankTabs() { return [{ key: 'hot', title: '热门榜' }, { key: 'vipLibrary', title: '会员专属' }, { key: 'newest', title: '最新上架' }] },
    activeBooks() { return (this[this.activeRank] || []).slice(0, 3) },
    featuredBooks() {
      const seen = new Set()
      return [...this.hot, ...this.newest, ...this.vipLibrary].filter(book => {
        if (!book || book.id == null || seen.has(String(book.id))) return false
        seen.add(String(book.id))
        return true
      })
    }
  },
  onLoad() { this.sessionToken = tokenStore.get(); this.loadData() },
  onShow() {
    const token = tokenStore.get()
    if (token !== this.sessionToken) {
      this.sessionToken = token
      this.recommendations = []
      this.loadRecommendations()
    }
  },
  onPullDownRefresh() { this.loadData().finally(() => uni.stopPullDownRefresh()) },
  methods: {
    async loadData() {
      this.loading = true
      this.errorMsg = ''
      try {
        const [home] = await Promise.all([getHomeBooks(), this.loadCategories()])
        this.hot = home.hot || []; this.newest = home.newest || []; this.vipLibrary = home.vipLibrary || []
      } catch (e) { this.errorMsg = e.msg || '书城加载失败' }
      finally { this.loading = false; await this.loadRecommendations() }
    },
    async loadCategories() {
      try { this.categories = await getCategoryList() } catch (e) { this.categories = [] }
    },
    async loadRecommendations() {
      const version = ++this.recommendationVersion
      this.personalized = false
      if (!PERSONALIZED_RECOMMENDATION_ENABLED) {
        this.recommendLoading = false; this.showFeaturedBooks(); return
      }
      this.recommendLoading = true
      try {
        const books = await getPersonalizedBooks()
        if (version !== this.recommendationVersion) return
        this.recommendations = books; this.personalized = books.length > 0 && !!tokenStore.get()
      } catch (e) {
        if (version === this.recommendationVersion) this.showFeaturedBooks()
      } finally { if (version === this.recommendationVersion) this.recommendLoading = false }
    },
    showFeaturedBooks() {
      const books = this.featuredBooks
      const offset = books.length ? this.recommendationOffset % books.length : 0
      this.recommendations = [...books.slice(offset), ...books.slice(0, offset)].slice(0, 6)
    },
    refreshRecommendations() { if (!this.recommendLoading) { this.recommendationOffset++; this.loadRecommendations() } },
    goSearch() { uni.navigateTo({ url: '/pages/book/list?mode=search' }) },
    goCategory(cat) {
      this.showCategoryPopup = false
      uni.navigateTo({ url: `/pages/book/list?categoryId=${cat.id}&categoryName=${encodeURIComponent(cat.name)}` })
    },
    goDetail(book) { uni.navigateTo({ url: `/pages/book/detail?bookId=${book.id}` }) }
  }
}
</script>

<style scoped>
.home{padding-bottom:36rpx}
.top-bar{display:flex;align-items:center;gap:22rpx;padding:22rpx 28rpx;background:#fff}
.search-input{flex:1;min-width:0;box-sizing:border-box;display:flex;align-items:center;gap:20rpx;height:72rpx;padding:0 28rpx;background:#f5f4f2;border-radius:36rpx}
.search-icon{position:relative;box-sizing:border-box;width:27rpx;height:27rpx;border:3rpx solid #8b8a87;border-radius:50%;flex-shrink:0;margin-right:4rpx}
.search-icon::after{content:'';position:absolute;width:13rpx;height:3rpx;background:#8b8a87;right:-10rpx;bottom:-5rpx;border-radius:2rpx;transform:rotate(45deg)}
.search-placeholder{color:#aaa6a1;font-size:26rpx}
.category-btn{display:flex;align-items:center;gap:12rpx;height:72rpx;color:#59514b;font-size:26rpx;padding:0 4rpx}
.category-icon{display:flex;flex-wrap:wrap;width:28rpx;height:28rpx;gap:5rpx}.category-icon view{box-sizing:border-box;width:11rpx;height:11rpx;border:2rpx solid #7d7168;border-radius:3rpx}
.rank-card{margin:24rpx 24rpx 0;padding:0 24rpx 24rpx;background:#fff;border:1rpx solid #eee9e4;border-radius:24rpx;overflow:hidden}
.rank-tabs{display:flex;align-items:center;gap:24rpx;border-bottom:1rpx solid #f3f0ec;margin-bottom:22rpx}
.rank-tab{position:relative;padding:24rpx 0 20rpx;color:#99918b;font-size:26rpx;white-space:nowrap}
.rank-tab.active{color:#b3543d;font-weight:600}.rank-tab.active::after{content:'';position:absolute;width:32rpx;height:5rpx;left:50%;bottom:0;transform:translateX(-50%);background:#b3543d;border-radius:4rpx}
.rank-hint{margin-left:auto;color:#c6b5a3;font-size:19rpx;letter-spacing:2rpx;white-space:nowrap}
.rank-books{display:flex;gap:26rpx}.rank-book{flex:1;min-width:0;display:flex;flex-direction:column;align-items:center}
.rank-cover-wrap{position:relative;width:138rpx;height:180rpx;margin-bottom:14rpx}.rank-cover{width:100%;height:100%;border-radius:8rpx;background:#f4ebe4;box-shadow:0 5rpx 14rpx rgba(82,57,36,.08)}
.rank-number{position:absolute;left:-7rpx;bottom:-3rpx;width:33rpx;height:37rpx;line-height:37rpx;text-align:center;border-radius:5rpx;font-size:25rpx;font-weight:bold;font-style:italic;background:#d3b6a1;color:#fff}.rank-number-0{background:#bb6247}.rank-number-1{background:#bc9473}
.rank-title{width:100%;text-align:center;font-size:25rpx;color:#3d3631;line-height:36rpx;font-weight:500}.rank-author{width:100%;text-align:center;font-size:21rpx;color:#aaa099;line-height:32rpx}
.rank-placeholder{min-height:262rpx;display:flex;align-items:center;justify-content:center;color:#aaa099;font-size:25rpx}
.recommend-section{padding:34rpx 28rpx 0}.recommend-heading{display:flex;align-items:center;justify-content:space-between;gap:16rpx;margin-bottom:26rpx}.recommend-title-row{display:flex;align-items:center;gap:14rpx}
.section-title{color:#342f2b;font-size:35rpx;font-weight:600;letter-spacing:1rpx}.recommend-badge{color:#a77a5b;background:#f0e7dc;font-size:19rpx;padding:5rpx 12rpx;border-radius:5rpx}.section-subtitle{display:block;font-size:22rpx;color:#aaa29a;margin-top:9rpx}
.refresh-btn{color:#9e9186;font-size:23rpx;padding:12rpx 0 12rpx 14rpx;white-space:nowrap}
.recommend-list{background:#fff;border-radius:20rpx;padding:0 22rpx}.recommend-book{display:flex;gap:24rpx;padding:26rpx 0}.recommend-book+.recommend-book{border-top:1rpx solid #f4f0ec}
.recommend-cover{width:116rpx;height:156rpx;flex-shrink:0;background:#f4ebe4;border-radius:7rpx}.recommend-info{flex:1;min-width:0;display:flex;flex-direction:column}
.recommend-name{color:#3d3631;font-size:29rpx;font-weight:500;line-height:40rpx}.recommend-intro{color:#9c938d;font-size:23rpx;line-height:34rpx;margin-top:10rpx}
.recommend-meta{display:flex;align-items:center;justify-content:space-between;gap:16rpx;margin-top:auto;padding-top:10rpx}.recommend-author{min-width:0;color:#9c938d;font-size:21rpx}.book-status{flex-shrink:0;font-size:19rpx;color:#aaa099;border:1rpx solid #e9e2db;border-radius:5rpx;padding:2rpx 9rpx}
.recommend-empty{background:#fff;border-radius:20rpx;padding:54rpx 20rpx;text-align:center;color:#9d9289;font-size:26rpx}.empty-caption{display:block;font-size:23rpx;color:#b3aaa2;margin-top:16rpx}
.popup-mask{position:fixed;inset:0;background:rgba(35,28,22,.4);z-index:999;display:flex;align-items:flex-start;justify-content:center}.popup-panel{box-sizing:border-box;margin-top:200rpx;width:650rpx;max-height:70vh;overflow-y:auto;background:#fff;border-radius:24rpx;padding:32rpx}
.popup-header{display:flex;align-items:center;justify-content:space-between;margin-bottom:28rpx}.popup-title{font-size:32rpx;font-weight:600}.popup-close{font-size:42rpx;color:#a79b91;padding:0 8rpx}.popup-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:20rpx}.popup-cat{padding:20rpx 0;text-align:center;background:#f7f1eb;color:#a67153;font-size:26rpx;border-radius:12rpx}.category-empty{text-align:center;color:#aaa099;padding:24rpx;font-size:26rpx}
</style>
