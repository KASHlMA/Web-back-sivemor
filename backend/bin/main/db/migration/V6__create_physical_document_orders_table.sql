CREATE TABLE physical_document_orders (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    verification_order_id BIGINT NOT NULL,
    shipped_at TIMESTAMP(6) NOT NULL,
    tracking_number VARCHAR(120) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ORDERED',
    received_by VARCHAR(160) NULL,
    photo_data LONGTEXT NULL,
    comment TEXT NULL,
    archived BIT NOT NULL DEFAULT b'0',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_physical_document_order_order FOREIGN KEY (verification_order_id) REFERENCES verification_orders(id)
);
