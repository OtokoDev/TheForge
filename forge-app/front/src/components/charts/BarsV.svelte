<script>
  // Histogramme vertical, séries groupées (ex. CA + bénéfice par semaine/jour).
  let { data = [], x, series = [], height = 288, format = (n) => Number(n).toLocaleString('fr-FR') } = $props()

  let W = $state(600)
  const PAD = { t: 10, r: 12, b: 28, l: 48 }
  let innerW = $derived(Math.max(0, W - PAD.l - PAD.r))
  const innerH = height - PAD.t - PAD.b
  let max = $derived(Math.max(1, ...data.flatMap((d) => series.map((s) => Number(d[s.key]) || 0))))
  let groupW = $derived(data.length ? innerW / data.length : 0)
  let barW = $derived(series.length ? Math.max(2, (groupW * 0.7) / series.length) : 0)
  const yOf = (v) => PAD.t + innerH * (1 - (Number(v) || 0) / max)
</script>

<div bind:clientWidth={W} class="w-full">
  <svg width={W} {height} role="img">
    {#each [0, 0.25, 0.5, 0.75, 1] as g}
      <line x1={PAD.l} x2={W - PAD.r} y1={PAD.t + innerH * g} y2={PAD.t + innerH * g} stroke="rgba(255,255,255,0.06)" />
    {/each}
    {#each data as d, i (i)}
      {#each series as s, si (si)}
        {@const bx = PAD.l + i * groupW + groupW * 0.15 + si * barW}
        {@const by = yOf(d[s.key])}
        <rect x={bx} y={by} width={barW} height={Math.max(0, PAD.t + innerH - by)} fill={s.color} rx="2">
          <title>{s.label} : {format(d[s.key])}</title>
        </rect>
      {/each}
      <text x={PAD.l + i * groupW + groupW / 2} y={height - 8} text-anchor="middle" font-size="10" fill="#8f8880">{d[x]}</text>
    {/each}
  </svg>
</div>
