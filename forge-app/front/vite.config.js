import { defineConfig } from 'vite'
import { svelte } from '@sveltejs/vite-plugin-svelte'
import tailwindcss from '@tailwindcss/vite'

// Build : sort directement l'export statique dans les ressources Micronaut (embarqué au jar).
// Dev : proxifie API / OAuth / logout / WebSocket vers le back Micronaut (:8080).
export default defineConfig({
  plugins: [svelte(), tailwindcss()],
  build: {
    outDir: '../src/main/resources/public',
    emptyOutDir: true,
  },
  server: {
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
      '/oauth': { target: 'http://localhost:8080', changeOrigin: true },
      '/logout': { target: 'http://localhost:8080', changeOrigin: true },
      '/ws': { target: 'ws://localhost:8080', ws: true, changeOrigin: true },
    },
  },
})
