-- Écrans masqués (front) pour un business, pilotés par SYSTEM. CSV de clés de route (ex. "/commandes,/carte").
ALTER TABLE business ADD COLUMN hidden_screens TEXT NOT NULL DEFAULT '';
