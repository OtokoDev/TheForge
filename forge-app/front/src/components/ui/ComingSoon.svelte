<script>
  import { Hammer } from '@lucide/svelte'
  import Card from './Card.svelte'
  import CardContent from './CardContent.svelte'

  let { title } = $props()
</script>

<Card>
  <CardContent class="flex flex-col items-center gap-3 py-16 text-center">
    <Hammer size={32} class="text-muted-foreground" />
    <h2 class="text-lg font-semibold">{title}</h2>
    <p class="max-w-md text-sm text-muted-foreground">
      Module en cours de migration vers la nouvelle architecture (back Micronaut).
      Disponible dans un prochain lot.
    </p>
  </CardContent>
</Card>
