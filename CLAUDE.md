@AGENTS.md
# CLAUDE.md

Guidelines pour réduire erreurs LLM. Fusionner avec instructions projet.

**Tradeoff:** biais prudence > vitesse. Tâches triviales : juge.

## 1. Think Before Coding

**Pas d'hypothèse cachée. Surface tradeoffs.**

Avant de coder :
- Énonce tes hypothèses. Incertain → demande.
- Plusieurs interprétations → présente-les, ne choisis pas en silence.
- Approche plus simple existe → dis-le, pushback.
- Pas clair → stop, nomme le flou, demande.

## 2. Simplicity First

**Code minimum qui résout. Rien de spéculatif.**

- Pas de feature non demandée.
- Pas d'abstraction pour usage unique.
- Pas de "flexibilité"/"configurabilité" non demandée.
- Pas de gestion d'erreur pour cas impossibles.
- 200 lignes possibles en 50 → réécris.

Test : "un senior dirait-il que c'est sur-compliqué ?" Oui → simplifie.

## 3. Surgical Changes

**Touche que le nécessaire. Nettoie que ton propre bazar.**

Édition de code existant :
- N'"améliore" pas code/commentaires/format adjacents.
- Ne refactore pas ce qui marche.
- Respecte le style existant.
- Code mort non lié → mentionne, ne supprime pas.

Orphelins créés par tes changements :
- Retire imports/vars/fonctions que TES changements rendent inutiles.
- Ne supprime pas le code mort préexistant sauf demande.

Test : chaque ligne changée trace à la demande user.

## 4. Goal-Driven Execution

**Définis critères de succès. Boucle jusqu'à vérifié.**

Tâches → buts vérifiables :
- "Add validation" → "tests inputs invalides, puis les faire passer"
- "Fix the bug" → "test qui reproduit, puis le faire passer"
- "Refactor X" → "tests verts avant et après"

Multi-étapes → plan bref :
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Critères forts = boucle autonome. Critères faibles ("make it work") = clarifications constantes.

---

**Ça marche si :** moins de changements inutiles dans diffs, moins de réécritures pour sur-complication, questions de clarification avant l'implémentation et non après l'erreur.
