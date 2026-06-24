/// <reference types="vite/client" />

interface Window {
  // expose in the `electron/preload/index.ts`
  mpa: import('../shared/api').MpaApi
}
