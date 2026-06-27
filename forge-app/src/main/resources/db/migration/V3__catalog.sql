-- Contexte Catalogue (global) : items et graphe de recettes.
CREATE TABLE item (
    id            UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    type          VARCHAR(20)  NOT NULL,
    hand_required VARCHAR(10),
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    is_system     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Une arête du graphe de recettes : output requiert `quantity` × component.
-- output -> CASCADE (supprimer un item retire sa recette) ; component -> RESTRICT
-- (un item utilisé comme composant ne peut pas être supprimé).
CREATE TABLE recipe_component (
    id                UUID    NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    output_item_id    UUID    NOT NULL REFERENCES item(id) ON DELETE CASCADE,
    component_item_id UUID    NOT NULL REFERENCES item(id) ON DELETE RESTRICT,
    quantity          INTEGER NOT NULL CHECK (quantity > 0),
    UNIQUE (output_item_id, component_item_id)
);

CREATE INDEX idx_recipe_output ON recipe_component(output_item_id);
CREATE INDEX idx_recipe_component ON recipe_component(component_item_id);

-- Le septime : monnaie, item système non supprimable (valeur 1 dans chaque business).
INSERT INTO item (name, type, is_system) VALUES ('Septime', 'MONNAIE', TRUE);
