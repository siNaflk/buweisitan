/**
 * 文本工具
 */

/** 解码常见 HTML 实体（章节正文里可能出现） */
export function decodeEntities(str) {
  if (!str) return ''
  return str
    .replace(/&nbsp;/g, ' ')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&amp;/g, '&')
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
}

/** 分转元展示：后端金额单位是分，且 Long 序列化为字符串 */
export function fenToYuan(fen) {
  const n = Number(fen || 0)
  return (n / 100).toFixed(n % 100 === 0 ? 0 : 2)
}

/** 字数展示：超过 1 万显示 "x.x万" */
export function formatWordCount(count) {
  const n = Number(count || 0)
  if (n >= 10000) {
    return (n / 10000).toFixed(1) + '万'
  }
  return String(n)
}
