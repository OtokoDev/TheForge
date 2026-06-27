/** @type {import('next').NextConfig} */
const nextConfig = {
  // Export statique : le build produit `out/` (HTML/CSS/JS pur), embarqué dans le jar
  // Micronaut et servi en ressources statiques. Aucun Node au runtime.
  output: "export",
  // Pas d'optimisation d'image serveur (incompatible export statique).
  images: { unoptimized: true },
  // Chaque route est exportée en dossier/index.html → sert bien derrière un serveur statique.
  trailingSlash: true,
  // Le lint tourne séparément ; on ne bloque pas le build/export dessus.
  eslint: { ignoreDuringBuilds: true },
}

module.exports = nextConfig
