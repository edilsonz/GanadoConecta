export const MANIFEST_PWA = {
  id: '/',
  name: 'GanadoConecta',
  short_name: 'Ganado',
  description: 'Gestión sencilla y segura de ganado vacuno.',
  lang: 'es',
  dir: 'ltr' as const,
  start_url: '/',
  scope: '/',
  display: 'standalone' as const,
  background_color: '#fffdf7',
  theme_color: '#174b38',
  categories: ['business', 'productivity'],
  icons: [
    {
      src: '/icons/ganadoconecta-192.png',
      sizes: '192x192',
      type: 'image/png',
      purpose: 'any' as const,
    },
    {
      src: '/icons/ganadoconecta-512.png',
      sizes: '512x512',
      type: 'image/png',
      purpose: 'any' as const,
    },
    {
      src: '/icons/ganadoconecta-maskable-512.png',
      sizes: '512x512',
      type: 'image/png',
      purpose: 'maskable' as const,
    },
  ],
}

export const CONFIGURACION_PRECACHE = {
  globPatterns: ['**/*.{js,css,html,svg,png,ico,woff,woff2,webmanifest}'],
  globIgnores: [
    '**/*.map',
    '**/manifest.webmanifest',
    '**/icons/ganadoconecta-*.png',
  ],
  navigateFallback: '/index.html',
  navigateFallbackDenylist: [/^\/api(?:\/|$)/],
  runtimeCaching: [],
  cleanupOutdatedCaches: true,
  clientsClaim: false,
  skipWaiting: false,
}
