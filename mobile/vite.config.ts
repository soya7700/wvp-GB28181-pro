import { defineConfig, loadEnv } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  return {
    plugins: [uni()],
    server: {
      host: '0.0.0.0',
      port: 5173,
      proxy: {
        '/dev-api': {
          target: env.VITE_PROXY_TARGET || 'http://127.0.0.1:18080',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/dev-api/, ''),
        },
      },
    },
  }
})
