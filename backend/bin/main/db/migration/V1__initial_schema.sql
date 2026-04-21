CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(150) NOT NULL UNIQUE,
    full_name VARCHAR(160) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    active BIT NOT NULL DEFAULT b'1',
    archived BIT NOT NULL DEFAULT b'0',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE TABLE refresh_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP(6) NOT NULL,
    revoked_at TIMESTAMP(6) NULL,
    archived BIT NOT NULL DEFAULT b'0',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE regions (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE,
    archived BIT NOT NULL DEFAULT b'0',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE TABLE client_companies (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    region_id BIGINT NOT NULL,
    name VARCHAR(160) NOT NULL UNIQUE,
    tax_id VARCHAR(30) NOT NULL UNIQUE,
    archived BIT NOT NULL DEFAULT b'0',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_client_companies_region FOREIGN KEY (region_id) REFERENCES regions(id)
);

CREATE TABLE vehicle_units (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    client_company_id BIGINT NOT NULL,
    plate VARCHAR(20) NOT NULL UNIQUE,
    vin VARCHAR(30) NOT NULL UNIQUE,
    category VARCHAR(10) NOT NULL,
    brand VARCHAR(80) NOT NULL,
    model VARCHAR(80) NOT NULL,
    archived BIT NOT NULL DEFAULT b'0',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_vehicle_units_client FOREIGN KEY (client_company_id) REFERENCES client_companies(id)
);

CREATE TABLE verification_orders (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    client_company_id BIGINT NOT NULL,
    region_id BIGINT NOT NULL,
    assigned_technician_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    scheduled_at TIMESTAMP(6) NOT NULL,
    notes TEXT NULL,
    archived BIT NOT NULL DEFAULT b'0',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_verification_orders_client FOREIGN KEY (client_company_id) REFERENCES client_companies(id),
    CONSTRAINT fk_verification_orders_region FOREIGN KEY (region_id) REFERENCES regions(id),
    CONSTRAINT fk_verification_orders_technician FOREIGN KEY (assigned_technician_id) REFERENCES users(id)
);

CREATE TABLE order_units (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    verification_order_id BIGINT NOT NULL,
    vehicle_unit_id BIGINT NOT NULL,
    archived BIT NOT NULL DEFAULT b'0',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_order_units_order_vehicle (verification_order_id, vehicle_unit_id),
    CONSTRAINT fk_order_units_order FOREIGN KEY (verification_order_id) REFERENCES verification_orders(id),
    CONSTRAINT fk_order_units_vehicle FOREIGN KEY (vehicle_unit_id) REFERENCES vehicle_units(id)
);

CREATE TABLE checklist_templates (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    version INT NOT NULL,
    current BIT NOT NULL DEFAULT b'1',
    archived BIT NOT NULL DEFAULT b'0',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE TABLE checklist_sections (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT NOT NULL,
    title VARCHAR(160) NOT NULL,
    description TEXT NULL,
    display_order INT NOT NULL,
    archived BIT NOT NULL DEFAULT b'0',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_checklist_sections_template FOREIGN KEY (template_id) REFERENCES checklist_templates(id)
);

CREATE TABLE checklist_questions (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    section_id BIGINT NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    prompt TEXT NOT NULL,
    display_order INT NOT NULL,
    required BIT NOT NULL DEFAULT b'1',
    archived BIT NOT NULL DEFAULT b'0',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_checklist_questions_section FOREIGN KEY (section_id) REFERENCES checklist_sections(id)
);

CREATE TABLE inspections (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    verification_order_id BIGINT NOT NULL,
    order_unit_id BIGINT NOT NULL UNIQUE,
    technician_id BIGINT NOT NULL,
    template_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    overall_result VARCHAR(20) NULL,
    overall_comment TEXT NULL,
    last_section_index INT NOT NULL DEFAULT 0,
    started_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    submitted_at TIMESTAMP(6) NULL,
    archived BIT NOT NULL DEFAULT b'0',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_inspections_order FOREIGN KEY (verification_order_id) REFERENCES verification_orders(id),
    CONSTRAINT fk_inspections_order_unit FOREIGN KEY (order_unit_id) REFERENCES order_units(id),
    CONSTRAINT fk_inspections_technician FOREIGN KEY (technician_id) REFERENCES users(id),
    CONSTRAINT fk_inspections_template FOREIGN KEY (template_id) REFERENCES checklist_templates(id)
);

CREATE TABLE inspection_answers (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    inspection_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    answer_value VARCHAR(10) NOT NULL,
    comment TEXT NULL,
    archived BIT NOT NULL DEFAULT b'0',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_inspection_answers_inspection_question (inspection_id, question_id),
    CONSTRAINT fk_inspection_answers_inspection FOREIGN KEY (inspection_id) REFERENCES inspections(id),
    CONSTRAINT fk_inspection_answers_question FOREIGN KEY (question_id) REFERENCES checklist_questions(id)
);

CREATE TABLE inspection_section_notes (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    inspection_id BIGINT NOT NULL,
    section_id BIGINT NOT NULL,
    comment TEXT NULL,
    archived BIT NOT NULL DEFAULT b'0',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_inspection_section_notes_inspection_section (inspection_id, section_id),
    CONSTRAINT fk_inspection_section_notes_inspection FOREIGN KEY (inspection_id) REFERENCES inspections(id),
    CONSTRAINT fk_inspection_section_notes_section FOREIGN KEY (section_id) REFERENCES checklist_sections(id)
);

CREATE TABLE inspection_evidences (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    inspection_id BIGINT NOT NULL,
    section_id BIGINT NULL,
    filename VARCHAR(255) NOT NULL,
    mime_type VARCHAR(120) NOT NULL,
    checksum VARCHAR(64) NOT NULL,
    captured_at TIMESTAMP(6) NOT NULL,
    comment TEXT NULL,
    content LONGBLOB NOT NULL,
    archived BIT NOT NULL DEFAULT b'0',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_inspection_evidences_inspection FOREIGN KEY (inspection_id) REFERENCES inspections(id),
    CONSTRAINT fk_inspection_evidences_section FOREIGN KEY (section_id) REFERENCES checklist_sections(id)
);

CREATE TABLE payments (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    verification_order_id BIGINT NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    reference VARCHAR(120) NULL,
    notes TEXT NULL,
    paid_at TIMESTAMP(6) NULL,
    archived BIT NOT NULL DEFAULT b'0',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_payments_order FOREIGN KEY (verification_order_id) REFERENCES verification_orders(id)
);

CREATE TABLE audit_log (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    actor_user_id BIGINT NULL,
    action VARCHAR(80) NOT NULL,
    entity_name VARCHAR(80) NOT NULL,
    entity_id VARCHAR(80) NOT NULL,
    details_json TEXT NULL,
    archived BIT NOT NULL DEFAULT b'0',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_audit_log_actor FOREIGN KEY (actor_user_id) REFERENCES users(id)
);
