<template>
  <view class="page">
    <!-- 已登录且有收藏 -->
    <view v-if="shelfList.length > 0" class="shelf-list">
      <view
        v-for="item in shelfList"
        :key="item.shelfId"
        class="shelf-item"
        @click="goDetailOrReader(item)"
      >
        <image
          class="cover"
          :src="item.coverUrl || DEFAULT_COVER"
          mode="aspectFill"
        />
        <view class="info">
          <text class="title ellipsis-1">{{ item.title }}</text>
          <text class="author ellipsis-1">{{ item.authorName }}</text>
          <text class="progress" v-if="item.lastChapterId">
            续读 {{ item.progress || 0 }}%
          </text>
          <text class="progress grey" v-else>未开始阅读</text>
        </view>
        <text
          class="remove-btn"
          @click.stop="removeItem(item)"
        >移出</text>
      </view>
    </view>

    <!-- 未登录 -->
    <view v-else-if="!loggedIn" class="empty">
      <text class="empty-icon">📚</text>
      <text class="empty-text">登录后可同步书架</text>
      <view class="empty-btn" @click="login">登录 / 注册</view>
    </view>

    <!-- 已登录但书架空 -->
    <view v-else class="empty">
      <text class="empty-icon">📚</text>
      <text class="empty-text">书架还是空的</text>
      <text class="empty-sub">去书城收藏几本喜欢的书吧</text>
      <view class="empty-btn" @click="goHome">去书城逛逛</view>
    </view>

    <view v-if="loading" class="tip-block">加载中...</view>
  </view>
</template>

<script>
import { getBookshelf, removeFromBookshelf } from '../../api/user'
import { DEFAULT_COVER } from '../../utils/cover'
import { tokenStore } from '../../utils/request'

export default {
  data() {
    return {
      shelfList: [],
      loading: false,
      loggedIn: false,
      DEFAULT_COVER
    }
  },
  onShow() {
    this.loggedIn=!!tokenStore.get()
    this.shelfList=[]
    this.loadShelf()
  },
  methods: {
    login() { uni.navigateTo({url:'/pages/auth/index'}) },
    async loadShelf() {
      if (!tokenStore.get()) return
      this.loading = true
      try {
        this.shelfList = await getBookshelf()
      } catch (e) {
        this.loggedIn=!!tokenStore.get()
        uni.showToast({title:e.msg || '书架加载失败',icon:'none'})
      } finally {
        this.loading = false
      }
    },
    async removeItem(item) {
      uni.showModal({
        title: '提示',
        content: `将《${item.title}》移出书架？`,
        success: async (res) => {
          if (!res.confirm) return
          try {
            await removeFromBookshelf(item.bookId)
            this.shelfList = this.shelfList.filter(
              (b) => b.shelfId !== item.shelfId
            )
            uni.showToast({ title: '已移出', icon: 'none' })
          } catch (e) {
            uni.showToast({ title: e.msg || '操作失败', icon: 'none' })
          }
        }
      })
    },
    goDetailOrReader(item) {
      if (item.lastChapterId) {
        uni.navigateTo({
          url: `/pages/reader/index?bookId=${item.bookId}&chapterId=${item.lastChapterId}`
        })
      } else {
        uni.navigateTo({ url: `/pages/book/detail?bookId=${item.bookId}` })
      }
    },
    goHome() {
      uni.switchTab({ url: '/pages/home/index' })
    }
  }
}
</script>

<style scoped>
.shelf-list {
  background: #fff;
}
.shelf-item {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 24rpx;
  border-bottom: 1rpx solid #f0f0f0;
}
.cover {
  width: 110rpx;
  height: 148rpx;
  border-radius: 10rpx;
  background: #eee;
  flex-shrink: 0;
}
.info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 10rpx;
}
.title {
  font-size: 30rpx;
  font-weight: bold;
}
.author {
  font-size: 24rpx;
  color: #999;
}
.progress {
  font-size: 24rpx;
  color: #c2543a;
}
.progress.grey {
  color: #bbb;
}
.remove-btn {
  font-size: 24rpx;
  color: #999;
  padding: 10rpx 20rpx;
  border: 1rpx solid #ddd;
  border-radius: 10rpx;
  flex-shrink: 0;
}
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16rpx;
  padding-top: 200rpx;
}
.empty-icon {
  font-size: 80rpx;
}
.empty-text {
  font-size: 30rpx;
  color: #666;
}
.empty-sub {
  font-size: 24rpx;
  color: #bbb;
}
.empty-btn {
  margin-top: 24rpx;
  padding: 18rpx 64rpx;
  background: #c2543a;
  color: #fff;
  border-radius: 36rpx;
  font-size: 28rpx;
}
</style>
