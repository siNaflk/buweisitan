<script>
export default {
  onLaunch() {},
  onShow() {},
  onHide() {}
}
</script>

<style>
page {
  background-color: #f8f8f9;
  font-size: 28rpx;
  color: #333;
}

/* 通用容器 */
.container {
  padding: 24rpx;
}

/* 加载/空态/错误提示 */
.tip-block {
  padding: 60rpx 24rpx;
  text-align: center;
  color: #999;
  font-size: 26rpx;
}

/* 通用书名两行省略 */
.ellipsis-2 {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.ellipsis-1 {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.m2-page{padding:28rpx 24rpx 70rpx}.m2-card{background:#fff;border-radius:20rpx;padding:28rpx;margin-bottom:24rpx}
.m2-heading{display:flex;justify-content:space-between;align-items:center;gap:16rpx;font-size:30rpx;font-weight:600;margin-bottom:18rpx}
.m2-muted{font-size:24rpx;color:#999;font-weight:normal}.m2-link{color:#c2543a;font-size:26rpx;font-weight:normal}
.m2-row{display:flex;align-items:center;justify-content:space-between;gap:20rpx;margin-top:20rpx}
.m2-primary{background:#c2543a!important;color:#fff!important;border-radius:40rpx;font-size:28rpx}.m2-secondary{background:#fff;color:#c2543a;border-radius:40rpx;font-size:28rpx}
.m2-input{background:#f8f8f9;padding:24rpx;margin-bottom:20rpx;border-radius:12rpx;font-size:28rpx;height:48rpx}
.m2-textarea{box-sizing:border-box;background:#f8f8f9;border-radius:12rpx;padding:20rpx;width:100%;height:180rpx;font-size:28rpx;line-height:1.7}
.m2-body{display:block;white-space:pre-wrap;overflow-wrap:anywhere;line-height:1.8;font-size:28rpx;margin:22rpx 0}
.m2-error{display:block;color:#b14436;font-size:26rpx;padding:20rpx 0}.m2-empty{padding:60rpx 20rpx;text-align:center;font-size:26rpx;color:#999}
.m2-center{text-align:center;padding:24rpx}.m2-badge{font-size:22rpx;background:#f7eee8;color:#a97345;border-radius:8rpx;padding:6rpx 12rpx;white-space:nowrap}
.m2-tabs{display:flex;gap:36rpx;margin-bottom:32rpx;color:#999;font-size:32rpx}.m2-tabs .active{color:#c2543a;font-weight:bold}

</style>
