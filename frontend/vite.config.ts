import { fileURLToPath } from 'node:url'
import { readFileSync } from 'node:fs'
import react from '@vitejs/plugin-react'
import { loadEnv } from 'vite'
import { defineConfig } from 'vitest/config'

export default defineConfig(({ mode }) => {
  const environment = loadEnv(mode, fileURLToPath(new URL('..', import.meta.url)), '')
  const certificado = environment.DEV_HTTPS_CERT_PATH
  const clave = environment.DEV_HTTPS_KEY_PATH
  if (Boolean(certificado) !== Boolean(clave)) {
    throw new Error('DEV_HTTPS_CERT_PATH y DEV_HTTPS_KEY_PATH deben configurarse juntos')
  }
  const https = certificado && clave
    ? { cert: readFileSync(certificado), key: readFileSync(clave) }
    : undefined
  return {
    plugins: [react()],
    server: {
      https,
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
