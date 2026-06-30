// Pipeline carte Bordeciel : SVG source (UESP, CC-BY-SA 2.5) → PNG HD → tuiles XYZ.
// Reproductible : `npm run build:map`. Source de vérité = map-src/skyrim_de.svg.
// Les libellés allemands sont VECTORISÉS (groupe <g id="Text">) → on les masque
// (display:none) ; les toponymes FR sont une surcouche Leaflet (markers.json).
import { readFileSync, writeFileSync, mkdirSync, rmSync, existsSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { Resvg } from '@resvg/resvg-js'
import sharp from 'sharp'

const here = dirname(fileURLToPath(import.meta.url))
const root = join(here, '..') // forge-app/front
const SRC = join(root, 'map-src', 'skyrim_de.svg')
const OUT = join(root, 'public', 'map')
const TILES = join(OUT, 'tiles')
const RENDER_WIDTH = 8192
const TILE = 256

// 1. Masque le groupe de libellés allemands vectorisés.
let svg = readFileSync(SRC, 'utf8')
if (!svg.includes('id="Text"')) {
  console.error('Groupe <g id="Text"> introuvable — structure SVG changée ?')
  process.exit(1)
}
svg = svg.replace('<g id="Text"', '<g id="Text" display="none"')

// 2. Rendu PNG haute résolution (libellés masqués → aucune police requise).
console.log(`Rendu PNG @ ${RENDER_WIDTH}px…`)
const resvg = new Resvg(svg, { fitTo: { mode: 'width', value: RENDER_WIDTH }, background: 'rgb(20,40,55)' })
const png = resvg.render().asPng()
const W = (await sharp(png).metadata()).width
const H = (await sharp(png).metadata()).height
console.log(`PNG ${W} x ${H}`)

// 3. Pyramide de tuiles : z = maxZoom (natif) … 0 (vue d'ensemble).
const maxZoom = Math.ceil(Math.log2(Math.max(W, H) / TILE))
if (existsSync(TILES)) rmSync(TILES, { recursive: true })
let totalTiles = 0
for (let z = 0; z <= maxZoom; z++) {
  const scale = 2 ** (z - maxZoom) // ≤ 1
  const zw = Math.max(1, Math.round(W * scale))
  const zh = Math.max(1, Math.round(H * scale))
  const level = await sharp(png).resize(zw, zh).png().toBuffer()
  const cols = Math.ceil(zw / TILE)
  const rows = Math.ceil(zh / TILE)
  for (let x = 0; x < cols; x++) {
    for (let y = 0; y < rows; y++) {
      const left = x * TILE
      const top = y * TILE
      const w = Math.min(TILE, zw - left)
      const h = Math.min(TILE, zh - top)
      const dir = join(TILES, String(z), String(x))
      mkdirSync(dir, { recursive: true })
      let img = sharp(level).extract({ left, top, width: w, height: h })
      if (w < TILE || h < TILE) {
        img = img.extend({ right: TILE - w, bottom: TILE - h, background: { r: 0, g: 0, b: 0, alpha: 0 } })
      }
      await img.png({ compressionLevel: 9 }).toFile(join(dir, `${y}.png`))
      totalTiles++
    }
  }
  console.log(`z${z}: ${cols}×${rows}`)
}

// 4. Métadonnées consommées par <MapBordeciel>.
mkdirSync(OUT, { recursive: true })
writeFileSync(join(OUT, 'meta.json'), JSON.stringify({ width: W, height: H, tileSize: TILE, maxZoom }, null, 2))
console.log(`OK → ${totalTiles} tuiles dans public/map/tiles + meta.json`)
