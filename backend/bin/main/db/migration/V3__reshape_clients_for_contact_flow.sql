ALTER TABLE client_companies
    MODIFY COLUMN region_id BIGINT NULL,
    MODIFY COLUMN tax_id VARCHAR(30) NULL;

ALTER TABLE client_companies
    ADD COLUMN business_name VARCHAR(160) NULL AFTER name,
    ADD COLUMN email VARCHAR(150) NULL AFTER business_name,
    ADD COLUMN phone VARCHAR(30) NULL AFTER email,
    ADD COLUMN alternate_phone VARCHAR(30) NULL AFTER phone,
    ADD COLUMN manager VARCHAR(160) NULL AFTER alternate_phone;

UPDATE client_companies
SET
    business_name = COALESCE(NULLIF(TRIM(business_name), ''), name),
    email = COALESCE(NULLIF(TRIM(email), ''), CONCAT('cliente', id, '@sivemor.local')),
    phone = COALESCE(NULLIF(TRIM(phone), ''), '0000000000'),
    alternate_phone = COALESCE(NULLIF(TRIM(alternate_phone), ''), '0000000000'),
    manager = COALESCE(NULLIF(TRIM(manager), ''), 'Sin asignar');

ALTER TABLE client_companies
    MODIFY COLUMN business_name VARCHAR(160) NOT NULL,
    MODIFY COLUMN email VARCHAR(150) NOT NULL,
    MODIFY COLUMN phone VARCHAR(30) NOT NULL,
    MODIFY COLUMN alternate_phone VARCHAR(30) NOT NULL,
    MODIFY COLUMN manager VARCHAR(160) NOT NULL;
