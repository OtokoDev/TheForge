<script>
  import { toasts } from '../lib/notifications.js'

  const styles = {
    info: 'border-border bg-card',
    success: 'border-emerald-500/40 bg-emerald-500/15 text-emerald-300',
    error: 'border-destructive/50 bg-destructive/15 text-destructive',
  }
</script>

<div class="pointer-events-none fixed bottom-4 right-4 z-50 flex flex-col gap-2">
  {#each $toasts as t (t.id)}
    <div class="pointer-events-auto rounded-md border px-4 py-2 text-sm shadow-lg {styles[t.type] ?? styles.info}">
      {t.message}
    </div>
  {/each}
</div>
