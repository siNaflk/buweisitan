import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

export default defineConfig({
  plugins: [uni()],
  server: {
    host: '0.0.0.0',
    port: Number(process.env.XUNYE_H5_PORT || 1026),
    strictPort: true,
    proxy: {
      '/api': {
        target: process.env.XUNYE_API_TARGET || 'http://127.0.0.1:8890',
        changeOrigin: true
      }
    }
  }
})
