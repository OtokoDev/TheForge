<script>
  // Aires superposées (ex. CA + bénéfice par jour).
  let { data = [], x, series = [], height = 288, format = (n) => Number(n).toLocaleString('fr-FR') } = $props()

  let W = $state(600)
  const PAD = { t: 10, r: 12, b: 22, l: 48 }
  let innerW = $derived(Math.max(0, W - PAD.l - PAD.r))
  const innerH = height - PAD.t - PAD.b
  let max = $derived(Math.max(1, ...data.flatMap((d) => series.map((s) => Number(d[s.key]) || 0))))
  let n = $derived(data.length)
  const xOf = (i) => PAD.l + (n <= 1 ? innerW / 2 : (innerW * i) / (n - 1))
  const yOf = (v) => PAD.t + innerH * (1 - (Number(v) || 0) / max)

  function linePath(key) {
    if (!data.length) return ''
    return 'M ' + data.map((d, i) => `${xOf(i)},${yOf(d[key])}`).join(' L ')
  }
  function areaPath(key) {
    if (!data.length) return ''
    const pts = data.map((d, i) => `${xOf(i)},${yOf(d[key])}`).join(' L ')
    return `M ${pts} L ${xOf(n - 1)},${PAD.t + innerH} L ${xOf(0)},${PAD.t + innerH} Z`
  }
  let step = $derived(Math.max(1, Math.ceil(n / 12)))
</script>

<div bind:clientWidth={W} class="w-full">
  <svg width={W} {height} role="img">
    {#each [0, 0.5, 1] as g}
      <line x1={PAD.l} x2={W - PAD.r} y1={PAD.t + innerH * g} y2={PAD.t + innerH * g} stroke="rgba(255,255,255,0.06)" />
    {/each}
    {#each series as s (s.key)}
      <path d={areaPath(s.key)} fill={s.color} fill-opacity="0.18" />
      <path d={linePath(s.key)} fill="none" stroke={s.color} stroke-width="2" />
    {/each}
    {#each data as d, i (i)}
      {#if i % step === 0}
        <text x={xOf(i)} y={height - 6} text-anchor="middle" font-size="10" fill="#8f8880">{d[x]}</text>
      {/if}
    {/each}
  </svg>
</div>
