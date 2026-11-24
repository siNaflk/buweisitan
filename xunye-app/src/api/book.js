import { get } from '../utils/request'

/** 书城首页推荐（热门 / 最新 / 会员书库） */
export const getHomeBooks = () => get('/api/app/home/books')

/** 启用中的分类列表 */
export const getCategoryList = () => get('/api/app/category/list')

/**
 * 分类书籍分页列表
 * @param {{categoryId?, keyword?, sort?, pageNum?, pageSize?}} params
 */
export const getBookList = (params) => get('/api/app/book/list', params)

/** 书籍详情（登录后带收藏状态） */
export const getBookDetail = (id) => get(`/api/app/book/detail/${id}`)

/** 相关推荐（同分类随机，limit 默认 4） */
export const getRelatedBooks = (id, limit = 4) => get(`/api/app/book/related/${id}`, { limit })

/** 章节目录 */
export const getChapterList = (bookId) => get(`/api/app/book/chapters/${bookId}`)

/** 章节内容（locked=true 时无正文） */
export const getChapterContent = (chapterId) => get(`/api/app/chapter/content/${chapterId}`)

/** 搜索（书名/作者模糊） */
export const searchBooks = (keyword, pageNum = 1, pageSize = 10) =>
  get('/api/app/book/search', { keyword, pageNum, pageSize })
