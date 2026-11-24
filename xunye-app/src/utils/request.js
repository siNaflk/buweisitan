import { BASE_URL, SUCCESS_CODE } from './config'

/**
 * token 存取
 * 后端 JWT 放在 Authorization 头里，裸 token，无 "Bearer " 前缀
 */
export const tokenStore = {
  get() {
    return uni.getStorageSync('token') || ''
  },
  set(token) {
    uni.setStorageSync('token', token)
  },
  clear() {
    uni.removeStorageSync('token')
  }
}

/** 是否已登录 */
export const isLogin = () => !!tokenStore.get()

/**
 * 统一请求封装
 * - 自动携带 Authorization
 * - 后端 Result 结构 { code, msg, data }，code === '200' 时 resolve(data)
 * - 失败 reject({ code, msg })，页面侧 catch 处理
 */
export function request(options) {
  const { url, method = 'GET', data = {}, header = {} } = options
  return new Promise((resolve, reject) => {
    const token = tokenStore.get()
    uni.request({
      url: BASE_URL + url,
      method,
      data,
      header: {
        ...(token ? { Authorization: token } : {}),
        ...header
      },
      success(res) {
        const body = res.data
        if (body && String(body.code) === String(SUCCESS_CODE)) {
          resolve(body.data)
          return
        }
        // HTTP 401：登录状态失效，清除本地登录态
        if (res.statusCode === 401 || (body && [401, 5005].includes(Number(body.code)))) {
          tokenStore.clear()
          uni.removeStorageSync('userInfo')
        }
        reject({
          code: (body && body.code) || 'ERROR',
          msg: (body && body.msg) || '请求失败 HTTP ' + res.statusCode,
          httpStatus: res.statusCode
        })
      },
      fail(err) {
        reject({ code: 'NETWORK_ERROR', msg: err.errMsg || '网络异常' })
      }
    })
  })
}

/** GET 快捷方式 */
export const get = (url, data) => request({ url, method: 'GET', data })

/** POST 快捷方式（JSON 体） */
export const post = (url, data) => request({ url, method: 'POST', data, header: { 'Content-Type': 'application/json' } })

/** DELETE 快捷方式 */
export const del = (url) => request({ url, method: 'DELETE' })
