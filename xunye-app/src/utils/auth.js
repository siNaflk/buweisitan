import { tokenStore } from './request'
export function requireLogin() {
  if (tokenStore.get()) return true
  uni.navigateTo({ url: '/pages/auth/index' })
  return false
}
