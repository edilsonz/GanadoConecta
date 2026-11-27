import { fileURLToPath } from 'node:url'
import { readFileSync } from 'node:fs'
import react from '@vitejs/plugin-react'
import { loadEnv } from 'vite'
import { defineConfig } from 'vitest/config'
import { VitePWA } from 'vite-plugin-pwa'
import {
  CONFIGURACION_PRECACHE,
  MANIFEST_PWA,
} from './src/pwa/pwaContrato.ts'

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
  const proxy = {
    '/api': {
      target: environment.API_PROXY_TARGET || 'http://127.0.0.1:8080',
      changeOrigin: false,
    },
  }
  return {
    plugins: [
      react(),
      VitePWA({
        registerType: 'prompt',
        injectRegister: null,
        manifest: MANIFEST_PWA,
        workbox: CONFIGURACION_PRECACHE,
        devOptions: { enabled: false },
      }),
    ],
    server: {
      https,
      proxy,
    },
    preview: {
      https,
      proxy,
    },
    test: {
      environment: 'happy-dom',
      setupFiles: ['./src/test/setup.ts'],
      clearMocks: true,
      coverage: {
        include: ['src/**/*.{ts,tsx}'],
        exclude: ['**/*.test.{ts,tsx}', '**/*.d.ts', 'src/test/**'],
      },
    },
  }
})
