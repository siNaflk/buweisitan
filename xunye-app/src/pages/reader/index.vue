<template>
  <view class="reader" :style="{ background: bgColor }">
    <!-- 正文 -->
    <view class="content">
      <view class="chapter-head">
        <text class="book-title">{{ bookTitle }}</text>
        <text class="chapter-title">{{ chapterTitle }}</text>
      </view>

      <template v-if="!locked">
        <view
          v-for="(para, index) in paragraphs"
          :key="index"
          class="paragraph"
          :style="{ fontSize: fontSize + 'rpx' }"
        >{{ para }}</view>
      </template>

      <!-- 付费锁定态 -->
      <view v-else class="locked-box">
        <text class="locked-icon">🔒</text>
        <text class="locked-text">本章为付费章节</text>
        <text class="locked-price">{{ fenToYuan(price) }} 元/章</text>
        <text class="locked-tip" v-if="Number(isFree) === 0 && Number(isVipFlag) === 1">
          加入 VIP 可免费读会员书库作品（M4 上线）
        </text>
        <view class="locked-btn" @click="onBuyClick">{{buying?'处理中…':'购买本章'}}</view>
      </view>

      <!-- 章节间导航 -->
      <view class="chapter-nav" v-if="!locked">
        <text
          class="nav-btn"
          :class="{ disabled: !hasPrev }"
          @click="prevChapter">上一章</text>
        <text
          class="nav-btn"
          :class="{ disabled: !hasNext }"
          @click="nextChapter">下一章</text>
      </view>
    </view>

    <CommentPanel v-if="!locked && chapterId" :bookId="bookId" :chapterId="chapterId" ref="comments" />

    <!-- 底部工具栏 -->
    <view class="toolbar">
      <text class="tool-btn" @click="showSettings = !showSettings">Aa</text>
      <text class="tool-btn" @click="goCatalog">目录</text>
      <text class="tool-btn" @click="goShelf">书架</text>
    </view>

    <!-- 设置面板：字号 / 背景 -->
    <view class="settings" v-if="showSettings">
      <view class="setting-row">
        <text class="setting-label">字号</text>
        <view class="font-btns">
          <text class="font-btn" @click="changeFont(-2)">A-</text>
          <text class="font-btn" @click="changeFont(2)">A+</text>
        </view>
      </view>
      <view class="setting-row">
        <text class="setting-label">背景</text>
        <view class="bg-btns">
          <view
            v-for="bg in bgList"
            :key="bg.color"
            class="bg-btn"
            :style="{ background: bg.color }"
            :class="{ active: bgColor === bg.color }"
            @click="setBg(bg)"
          />
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { getChapterContent, getChapterList } from '../../api/book'
import { decodeEntities, fenToYuan } from '../../utils/text'
import { tokenStore } from '../../utils/request'
import CommentPanel from '../../components/CommentPanel.vue'
import {buyChapter} from '../../api/wallet'
import {requireLogin} from '../../utils/auth'

const FONT_SIZE_KEY = 'readerFontSize'
const BG_KEY = 'readerBg'

export default {
  components: { CommentPanel },
  data() {
    return {
      bookId: null,
      chapterId: null,
      bookTitle: '',
      chapterTitle: '',
      content: '',
      locked: false,
      price: 0,
      isFree: 1,
      isVipFlag: 0,
      chapterIds: [],
      chapterIndex: -1,
      fontSize: 32,
      bgColor: '#f5f1e8',
      showSettings: false,
      errorMsg: '',
      buying: false,
      contentVersion: 0,
      fenToYuan,
      bgList: [
        { color: '#f5f1e8', name: '米黄' },
        { color: '#ffffff', name: '纯白' },
        { color: '#e8f0e5', name: '护眼' },
        { color: '#2b2b2b', name: '夜间' }
      ]
    }
  },
  computed: {
    paragraphs() {
      return decodeEntities(this.content)
        .split('\n')
        .map((s) => s.trim())
        .filter((s) => s.length > 0)
    },
    hasPrev() {
      return this.chapterIndex > 0
    },
    hasNext() {
      return this.chapterIndex >= 0 && this.chapterIndex < this.chapterIds.length - 1
    }
  },
  onLoad(options) {
    this.bookId = options.bookId
    this.chapterId = options.chapterId
    this.fontSize = uni.getStorageSync(FONT_SIZE_KEY) || 32
    const savedBg = uni.getStorageSync(BG_KEY)
    if (savedBg) this.bgColor = savedBg
    this.loadChapterList()
    this.loadContent()
  },
  onShow() { if(this.chapterId)this.loadContent(); this.$refs.comments?.refresh() },
  methods: {
    /** 拉目录拿到章节顺序，供上一章/下一章导航 */
    async loadChapterList() {
      try {
        const list = await getChapterList(this.bookId)
        this.chapterIds = list.map((c) => c.id)
        this.chapterIndex = this.chapterIds.indexOf(this.chapterId)
      } catch (e) {
        /* 目录加载失败不影响正文阅读 */
      }
    },
    async loadContent() {
      const version=++this.contentVersion
      try {
        const data = await getChapterContent(this.chapterId)
        if(version!==this.contentVersion)return
        this.bookTitle = data.bookTitle || ''
        this.chapterTitle = data.chapterTitle || ''
        this.content = data.content || ''
        this.locked = !!data.locked
        this.price = Number(data.price || 0)
        this.isFree = Number(data.isFree)
        this.isVipFlag = data.isVip ? 1 : 0
        uni.setNavigationBarTitle({ title: this.chapterTitle })
        if(!this.locked)this.saveProgress()
      } catch (e) {
        if(version!==this.contentVersion)return
        this.content='';this.locked=true
        this.errorMsg = e.msg || '加载失败'
        uni.showToast({ title: this.errorMsg, icon: 'none' })
      }
    },
    /** 阅读进度先存本地（服务端 reading_progress 同步在 M2/M3 打通） */
    saveProgress() {
      if (!tokenStore.get()) return
      uni.setStorageSync('readingProgress:' + this.bookId, {
        chapterId: this.chapterId,
        chapterTitle: this.chapterTitle,
        updatedAt: Date.now()
      })
    },
    switchChapter(chapterId) {
      this.chapterId = chapterId
      this.chapterIndex = this.chapterIds.indexOf(chapterId)
      this.content = ''
      this.locked = true
      uni.pageScrollTo({ scrollTop: 0, duration: 0 })
      this.loadContent()
    },
    prevChapter() {
      if (!this.hasPrev) return
      this.switchChapter(this.chapterIds[this.chapterIndex - 1])
    },
    nextChapter() {
      if (!this.hasNext) return
      this.switchChapter(this.chapterIds[this.chapterIndex + 1])
    },
    changeFont(delta) {
      this.fontSize = Math.min(48, Math.max(24, this.fontSize + delta))
      uni.setStorageSync(FONT_SIZE_KEY, this.fontSize)
    },
    setBg(bg) {
      this.bgColor = bg.color
      uni.setStorageSync(BG_KEY, bg.color)
      // 夜间模式导航栏配色
      if (bg.color === '#2b2b2b') {
        uni.setNavigationBarColor({ frontColor: '#ffffff', backgroundColor: '#2b2b2b' })
      } else {
        uni.setNavigationBarColor({ frontColor: '#000000', backgroundColor: '#f5f1e8' })
      }
    },
    goCatalog() {
      uni.navigateBack()
    },
    goShelf() {
      uni.switchTab({ url: '/pages/shelf/index' })
    },
    onBuyClick() {
      if(this.buying||!requireLogin())return
      const id=this.chapterId,price=this.price
      uni.showModal({title:'购买本章',content:'确认使用余额支付 '+fenToYuan(price)+' 元，永久解锁本章？',success:async r=>{
        if(!r.confirm||this.buying||id!==this.chapterId)return
        this.buying=true
        try{const d=await buyChapter(id,price);uni.showToast({title:d.alreadyProcessed?'本章已解锁':'购买成功',icon:'none'});if(id===this.chapterId)await this.loadContent()}
        catch(e){if(Number(e.code)===5001){uni.showModal({title:'余额不足',content:'是否前往钱包充值？',success:r=>{if(r.confirm)uni.navigateTo({url:'/pages/wallet/index'})}})}else{uni.showToast({title:e.msg||'购买失败，请重试',icon:'none'});if(Number(e.code)===409)await this.loadContent()}}
        finally{this.buying=false}
      }})
    }
  }
}
</script>

<style scoped>
.reader {
  min-height: 100vh;
  transition: background 0.2s;
}
.content {
  padding: 32rpx 40rpx 140rpx;
}
.chapter-head {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
  margin-bottom: 32rpx;
  padding-bottom: 24rpx;
  border-bottom: 1rpx solid rgba(0, 0, 0, 0.08);
}
.book-title {
  font-size: 24rpx;
  opacity: 0.6;
}
.chapter-title {
  font-size: 34rpx;
  font-weight: bold;
}
.paragraph {
  line-height: 1.8;
  margin-bottom: 24rpx;
  text-indent: 2em;
  color: #333;
}
.locked-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20rpx;
  padding: 80rpx 40rpx;
}
.locked-icon {
  font-size: 64rpx;
}
.locked-text {
  font-size: 30rpx;
  color: #666;
}
.locked-price {
  font-size: 32rpx;
  color: #d4a24e;
  font-weight: bold;
}
.locked-tip {
  font-size: 24rpx;
  color: #999;
}
.locked-btn {
  margin-top: 20rpx;
  padding: 20rpx 80rpx;
  background: #d4a24e;
  color: #fff;
  border-radius: 40rpx;
  font-size: 28rpx;
}
.chapter-nav {
  display: flex;
  justify-content: space-between;
  margin-top: 40rpx;
  padding-top: 24rpx;
  border-top: 1rpx solid rgba(0, 0, 0, 0.08);
}
.nav-btn {
  padding: 16rpx 40rpx;
  background: rgba(0, 0, 0, 0.05);
  border-radius: 32rpx;
  font-size: 26rpx;
  color: #666;
}
.nav-btn.disabled {
  opacity: 0.35;
}
.toolbar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  justify-content: space-around;
  padding: 20rpx 0 calc(20rpx + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.96);
  border-top: 1rpx solid #eee;
}
.tool-btn {
  font-size: 28rpx;
  color: #666;
  padding: 8rpx 40rpx;
}
.settings {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 100rpx;
  padding: 24rpx 40rpx calc(24rpx + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.98);
  border-top: 1rpx solid #eee;
}
.setting-row {
  display: flex;
  align-items: center;
  gap: 24rpx;
  margin-bottom: 20rpx;
}
.setting-label {
  font-size: 26rpx;
  color: #666;
  width: 80rpx;
}
.font-btns {
  display: flex;
  gap: 20rpx;
}
.font-btn {
  width: 88rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f2f2f4;
  border-radius: 12rpx;
  font-size: 28rpx;
  color: #333;
}
.bg-btns {
  display: flex;
  gap: 24rpx;
}
.bg-btn {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  border: 4rpx solid transparent;
}
.bg-btn.active {
  border-color: #c2543a;
}
</style>
