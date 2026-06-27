<script>
  // Camembert/donut SVG + légende (ex. ventes par famille/matériau).
  let { data = [], colors = [], format = (n) => Number(n).toLocaleString('fr-FR') } = $props()

  const COLORS_DEF = ['#E8590C', '#5fa890', '#a288bd', '#d9a441', '#7d90a6', '#d6855a', '#6a8fb0', '#88a06f', '#cf8a5a', '#9ab0c8']
  const R = 80
  const IR = 50
  const C = 110

  let total = $derived(data.reduce((s, d) => s + (Number(d.valeur) || 0), 0))
  let slices = $derived.by(() => {
    let acc = 0
    return data.map((d, i) => {
      const val = Number(d.valeur) || 0
      const start = total > 0 ? acc / total : 0
      acc += val
      const end = total > 0 ? acc / total : 0
      return { d, start, end, color: colors[i] ?? COLORS_DEF[i % COLORS_DEF.length] }
    })
  })

  function arc(start, end) {
    const a0 = 2 * Math.PI * start - Math.PI / 2
    const a1 = 2 * Math.PI * end - Math.PI / 2
    const large = end - start > 0.5 ? 1 : 0
    const x0 = C + R * Math.cos(a0), y0 = C + R * Math.sin(a0)
    const x1 = C + R * Math.cos(a1), y1 = C + R * Math.sin(a1)
    const xi1 = C + IR * Math.cos(a1), yi1 = C + IR * Math.sin(a1)
    const xi0 = C + IR * Math.cos(a0), yi0 = C + IR * Math.sin(a0)
    return `M ${x0} ${y0} A ${R} ${R} 0 ${large} 1 ${x1} ${y1} L ${xi1} ${yi1} A ${IR} ${IR} 0 ${large} 0 ${xi0} ${yi0} Z`
  }
</script>

<div class="flex flex-wrap items-center gap-4">
  <svg width="220" height="220" viewBox="0 0 220 220" role="img">
    {#each slices as s (s.d.nom)}
      {#if s.end > s.start}
        <path d={arc(s.start, s.end)} fill={s.color}><title>{s.d.nom} : {format(s.d.valeur)}</title></path>
      {/if}
    {/each}
  </svg>
  <div class="flex flex-col gap-1 text-xs">
    {#each slices as s (s.d.nom)}
      <div class="flex items-center gap-2">
        <span class="size-3 rounded-sm" style="background:{s.color}"></span>
        <span>{s.d.nom} — {format(s.d.valeur)}</span>
      </div>
    {/each}
  </div>
</div>
