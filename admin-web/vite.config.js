import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import ip from 'ip'

const localIp = ip.address()
const port = 5174

export default defineConfig({
  plugins: [vue()],
  server: {
    port: port,
    host: true,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      }
    }
  },
  define: {
    __LOCAL_ORIGIN__: JSON.stringify(`http://${localIp}:${port}`)
  }
})