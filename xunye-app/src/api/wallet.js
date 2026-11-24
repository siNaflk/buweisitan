import { get, post } from '../utils/request'
export const getWallet = () => get('/api/app/wallet')
export const getRechargePlans = () => get('/api/app/wallet/plans')
export const createRecharge = (data) => post('/api/app/wallet/recharge', data)
export const getRechargeOrder = (id) => get(`/api/app/wallet/recharge/${id}`)
export const simulatePayment = (id) => post(`/api/app/wallet/recharge/${id}/simulate-payment`)
export const getFlows = (data) => get('/api/app/wallet/flows', data)
export const buyChapter = (id, expectedPrice) => post(`/api/app/chapter/buy/${id}`, { expectedPrice })
export const tipBook = (id, data) => post(`/api/app/book/tip/${id}`, data)
