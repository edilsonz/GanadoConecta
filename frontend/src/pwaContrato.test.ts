import { describe, expect, it } from 'vitest'
import {
  CONFIGURACION_PRECACHE,
  MANIFEST_PWA,
} from './pwaContrato'

describe('contrato PWA', () => {
  it('define un manifest instalable en español con iconos rasterizados', () => {
    expect(MANIFEST_PWA).toMatchObject({
      id: '/',
      name: 'GanadoConecta',
      short_name: 'Ganado',
      lang: 'es',
      display: 'standalone',
      start_url: '/',
      scope: '/',
      theme_color: '#174b38',
      background_color: '#fffdf7',
    })
    expect(MANIFEST_PWA.icons).toEqual(expect.arrayContaining([
      expect.objectContaining({ sizes: '192x192', type: 'image/png' }),
      expect.objectContaining({ sizes: '512x512', type: 'image/png', purpose: 'any' }),
      expect.objectContaining({ sizes: '512x512', purpose: 'maskable' }),
    ]))
  })

  it('precacha solo recursos estáticos y excluye explícitamente la API del fallback', () => {
    expect(CONFIGURACION_PRECACHE.runtimeCaching).toEqual([])
    expect(CONFIGURACION_PRECACHE.globPatterns).toEqual([
      '**/*.{js,css,html,svg,png,ico,woff,woff2,webmanifest}',
    ])
    expect(CONFIGURACION_PRECACHE.globIgnores).toEqual(expect.arrayContaining([
      '**/manifest.webmanifest',
      '**/icons/ganadoconecta-*.png',
    ]))
    expect(CONFIGURACION_PRECACHE.navigateFallback).toBe('/index.html')
    expect(CONFIGURACION_PRECACHE.navigateFallbackDenylist[0].test('/api/v1/auth/me')).toBe(true)
    expect(CONFIGURACION_PRECACHE.navigateFallbackDenylist[0].test('/mi-ruta')).toBe(false)
    expect(CONFIGURACION_PRECACHE.skipWaiting).toBe(false)
    expect(CONFIGURACION_PRECACHE.clientsClaim).toBe(false)
  })
})
