import { get } from '../utils/request'

// 后端已接通；需要临时回退到书城精选时可关闭。
export const PERSONALIZED_RECOMMENDATION_ENABLED = true

/**
 * GET /api/app/book/recommendation，服务端固定最多返回6本，不传limit。
 * 用户身份由 Authorization 识别，不传 userId。
 * 返回 { code: 200, msg: '成功', data: [{ id, title, authorName, coverUrl, intro, status }] }
 * data 是书籍数组。无候选时返回 []；游客由后端全站随机推荐。
 */
export async function getPersonalizedBooks() {
  const data = await get('/api/app/book/recommendation')
  if (!Array.isArray(data)) throw new Error('推荐接口应返回书籍数组')
  const seen = new Set()
  return data.filter(book => {
    if (!book || book.id == null || seen.has(String(book.id))) return false
    seen.add(String(book.id))
    return true
  })
}
