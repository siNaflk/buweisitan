import { get, post, del } from '../utils/request'

/** 我的书架（需登录） */
export const getBookshelf = () => get('/api/app/user/bookshelves')

/** 加入书架（需登录） */
export const addToBookshelf = (bookId) => post(`/api/app/user/bookshelf/${bookId}`)

/** 移出书架（需登录） */
export const removeFromBookshelf = (bookId) => del(`/api/app/user/bookshelf/${bookId}`)
