import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/home': 'http://localhost:8083',
      '/auth': 'http://localhost:8083',
      '/login':'http://localhost:8083',
      '/logout':'http://localhost:8083',
    },
  },
})