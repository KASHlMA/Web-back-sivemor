CREATE TABLE verification_centers (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(160) NOT NULL UNIQUE,
    center_key VARCHAR(60) NOT NULL UNIQUE,
    address VARCHAR(255) NOT NULL,
    region_id BIGINT NOT NULL,
    manager VARCHAR(160) NOT NULL,
    email VARCHAR(150) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    alternate_phone VARCHAR(30) NOT NULL,
    schedule VARCHAR(120) NOT NULL,
    archived BIT NOT NULL DEFAULT b'0',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_verification_centers_region FOREIGN KEY (region_id) REFERENCES regions(id)
);
