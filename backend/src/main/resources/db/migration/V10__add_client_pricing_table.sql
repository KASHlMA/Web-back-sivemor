CREATE TABLE client_pricing (
    id BIGINT NOT NULL AUTO_INCREMENT,
    client_company_id BIGINT NOT NULL,
    materia VARCHAR(20) NOT NULL,
    price DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_client_pricing_client_materia (client_company_id, materia),
    CONSTRAINT fk_client_pricing_client FOREIGN KEY (client_company_id) REFERENCES client_companies (id)
);
