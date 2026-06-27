import { writable } from 'svelte/store'

// Toasts éphémères (remplace sonner). Affichés par <ToastHost>.
export const toasts = writable([])
let seq = 0

export function toast(message, type = 'info') {
  const t = { id: ++seq, message, type }
  toasts.update((a) => [...a, t])
  setTimeout(() => toasts.update((a) => a.filter((x) => x.id !== t.id)), 4000)
}

export const notifySuccess = (m) => toast(m, 'success')
export const notifyError = (m) => toast(m, 'error')
