ALTER TABLE payments
    ADD COLUMN payment_type VARCHAR(20) NOT NULL DEFAULT 'CASH' AFTER verification_order_id,
    ADD COLUMN deposit_account VARCHAR(160) NULL AFTER amount,
    ADD COLUMN invoice_number VARCHAR(120) NULL AFTER deposit_account;

UPDATE payments
SET status = 'APPROVED'
WHERE status = 'PAID';

ALTER TABLE payments
    DROP COLUMN currency,
    DROP COLUMN reference,
    DROP COLUMN notes;
