import { fileURLToPath } from 'node:url'
import react from '@vitejs/plugin-react'
import { loadEnv } from 'vite'
import { defineConfig } from 'vitest/config'

export default defineConfig(({ mode }) => {
  const environment = loadEnv(mode, fileURLToPath(new URL('..', import.meta.url)), '')
  return {
    plugins: [react()],
    server: {
      proxy: {
        '/api': {
          target: environment.API_PROXY_TARGET || 'http://127.0.0.1:8080',
          changeOrigin: false,
        },
      },
    },
    test: {
      environment: 'happy-dom',
      setupFiles: ['./src/test/setup.ts'],
      clearMocks: true,
    },
  }
})
