import {get,post} from '../utils/request'
export const getVipPlans=()=>get('/api/app/vip/plans')
export const getVipStatus=()=>get('/api/app/vip/status')
export const subscribeVip=(id,data)=>post(`/api/app/vip/subscribe/${id}`,data)
export const getVipSubscriptions=(data)=>get('/api/app/vip/subscriptions',data)
export const getVipLibrary=(data)=>get('/api/app/vip/library',data)
export const getNotifications=(data)=>get('/api/app/notifications',data)
export const getUnreadCount=()=>get('/api/app/notifications/unread-count')
export const readNotification=(id)=>post(`/api/app/notifications/${id}/read`)
