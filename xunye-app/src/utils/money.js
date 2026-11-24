// 金额以整数字符串传输；格式化时不用浮点计算，避免大额余额丢失精度。
export function money(value) {
  const s = String(value ?? 0)
  if (!/^\d+$/.test(s)) return '0.00'
  const padded = s.padStart(3, '0')
  return padded.slice(0, -2) + '.' + padded.slice(-2)
}
// 幂等号只负责标记同一次提交，不作为身份凭证；发生网络错误时必须保留重试。
export const requestId = () => Date.now().toString(36) + '-' + Math.random().toString(36).slice(2) + '-' + Math.random().toString(36).slice(2)
