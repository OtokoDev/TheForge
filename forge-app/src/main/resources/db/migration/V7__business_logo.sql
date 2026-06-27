-- Logo du business, stocké en data-URL base64 (image légère ; pas de volume requis).
ALTER TABLE business ADD COLUMN logo_data_url TEXT;
