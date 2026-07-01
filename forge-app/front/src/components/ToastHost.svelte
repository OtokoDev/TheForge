<script>
  import { toasts } from '../lib/notifications.js'

  const styles = {
    info: 'border-border bg-card',
    success: 'border-emerald-500/40 bg-emerald-500/15 text-emerald-300',
    error: 'border-destructive/50 bg-destructive/15 text-destructive',
  }
</script>

<div class="pointer-events-none fixed bottom-4 right-4 z-[2000] flex flex-col gap-2">
  {#each $toasts as t (t.id)}
    <div class="pointer-events-auto rounded-lg border px-5 py-3 text-base font-medium shadow-xl {styles[t.type] ?? styles.info}">
      {t.message}
    </div>
  {/each}
</div>
