/**
 * 环境配置
 * - H5 端走 vite 代理（/api → http://localhost:8890），BASE_URL 留空即可
 * - App/小程序端没有代理，直连后端：真机调试时改成电脑的局域网 IP
 */
// #ifdef H5
export const BASE_URL = ''
// #endif
// #ifndef H5
export const BASE_URL = 'http://10.195.67.27:8890'
// #endif

/** 后端统一返回的成功码 */
export const SUCCESS_CODE = '200'
