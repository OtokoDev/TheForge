-- Farmeur = nom libre (plus de lien vers un membre du site).
ALTER TABLE creance_entry ADD COLUMN farmer_name VARCHAR(120);
UPDATE creance_entry ce SET farmer_name = COALESCE(u.in_game_name, u.username)
  FROM app_user u WHERE ce.farmer_user_id = u.id;
UPDATE creance_entry SET farmer_name = 'Inconnu' WHERE farmer_name IS NULL;
ALTER TABLE creance_entry ALTER COLUMN farmer_name SET NOT NULL;
ALTER TABLE creance_entry DROP COLUMN farmer_user_id;
CREATE INDEX idx_creance_business_farmer_name ON creance_entry (business_id, farmer_name);
