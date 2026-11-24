/**
 * 寻页 H5 静态服务器（生产构建版，替代 vite dev）
 * - 静态文件来自 dist/build/h5
 * - /api 请求代理到后端 8890（与 vite dev 的 proxy 行为一致）
 * 用法：node scripts/serve.js  （端口 1026）
 */
const http = require('http')
const fs = require('fs')
const path = require('path')

const PORT = 1026
const BACKEND_HOST = 'localhost'
const BACKEND_PORT = 8890
const ROOT = path.join(__dirname, '..', 'dist', 'build', 'h5')

const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.gif': 'image/gif',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
  '.ttf': 'font/ttf',
  '.map': 'application/json',
}

const server = http.createServer((req, res) => {
  // /api 开头 → 转发后端
  if (req.url.startsWith('/api')) {
    const proxyReq = http.request(
      {
        host: BACKEND_HOST,
        port: BACKEND_PORT,
        path: req.url,
        method: req.method,
        headers: { ...req.headers, host: `${BACKEND_HOST}:${BACKEND_PORT}` },
      },
      (proxyRes) => {
        res.writeHead(proxyRes.statusCode, proxyRes.headers)
        proxyRes.pipe(res)
      }
    )
    proxyReq.on('error', () => {
      res.writeHead(502, { 'Content-Type': 'application/json' })
      res.end(JSON.stringify({ code: '502', msg: '后端服务不可用', data: null }))
    })
    req.pipe(proxyReq)
    return
  }

  // 其余 → 静态文件；带扩展名的文件不存在时返回 404（避免把 HTML 回退给 JS/CSS 请求）
  let urlPath = decodeURIComponent(req.url.split('?')[0])
  let filePath = path.join(ROOT, urlPath)
  if (!filePath.startsWith(ROOT)) {
    res.writeHead(403)
    return res.end('Forbidden')
  }
  const hasExt = path.extname(urlPath) !== ''
  if (hasExt && !fs.existsSync(filePath)) {
    res.writeHead(404, { 'Content-Type': 'text/plain' })
    return res.end('Not Found: ' + urlPath)
  }
  if (urlPath === '/' || !fs.existsSync(filePath) || fs.statSync(filePath).isDirectory()) {
    filePath = path.join(ROOT, 'index.html')
  }
  const ext = path.extname(filePath).toLowerCase()
  res.writeHead(200, { 'Content-Type': MIME[ext] || 'application/octet-stream' })
  fs.createReadStream(filePath).pipe(res)
})

server.listen(PORT, () => {
  console.log(`xunye-app static server: http://localhost:${PORT}  (api -> ${BACKEND_HOST}:${BACKEND_PORT})`)
})
