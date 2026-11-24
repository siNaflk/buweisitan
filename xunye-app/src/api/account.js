import { get, post } from '../utils/request'
export const getCaptcha = () => get('/api/app/user/captcha')
export const login = (data) => post('/api/app/user/login', data)
export const register = (data) => post('/api/app/user/register', data)
export const getMe = () => get('/api/app/user/me')
export const logout = () => post('/api/app/user/logout')
