-- V32 a changé le sens de tax_rate.rate (part entreprise → part forgeron sur le CA).
-- On clôture les réglages existants pour forcer une reconfiguration explicite (Config → Taxe),
-- plutôt que de réinterpréter silencieusement les anciens taux à l'envers.
UPDATE tax_rate SET valid_to = NOW() WHERE valid_to IS NULL;
