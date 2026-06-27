<script>
  // Barres horizontales classées (ex. CA par business, top items).
  let {
    data = [],
    label,
    value,
    color = '#E8590C',
    format = (n) => Number(n).toLocaleString('fr-FR'),
    barHeight = 22,
    gap = 8,
  } = $props()

  let W = $state(600)
  const PAD = { l: 130, r: 48, t: 6, b: 6 }
  let max = $derived(Math.max(1, ...data.map((d) => Number(d[value]) || 0)))
  let innerW = $derived(Math.max(0, W - PAD.l - PAD.r))
  let h = $derived(PAD.t + PAD.b + data.length * (barHeight + gap))
</script>

<div bind:clientWidth={W} class="w-full">
  <svg width={W} height={h} role="img">
    {#each data as d, i (i)}
      {@const yy = PAD.t + i * (barHeight + gap)}
      {@const bw = innerW * ((Number(d[value]) || 0) / max)}
      <text x={PAD.l - 8} y={yy + barHeight / 2} text-anchor="end" dominant-baseline="middle" font-size="11" fill="#cfc8c2">{d[label]}</text>
      <rect x={PAD.l} y={yy} width={bw} height={barHeight} fill={color} rx="3"><title>{format(d[value])}</title></rect>
      <text x={PAD.l + bw + 6} y={yy + barHeight / 2} dominant-baseline="middle" font-size="10" fill="#8f8880">{format(d[value])}</text>
    {/each}
  </svg>
</div>
