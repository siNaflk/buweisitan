/** 封面缺省图（纯色 data URI，避免占位图文件依赖） */
export const DEFAULT_COVER =
  'data:image/svg+xml;charset=utf-8,' +
  encodeURIComponent(
    '<svg xmlns="http://www.w3.org/2000/svg" width="200" height="260"><rect width="200" height="260" fill="#f3e0da"/><text x="100" y="140" font-size="60" text-anchor="middle" fill="#c2543a">寻</text></svg>'
  )
