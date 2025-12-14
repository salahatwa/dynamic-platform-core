-- V10: Create App Configuration Tables

-- App Configuration Groups
CREATE TABLE app_config_group (
    id BIGSERIAL PRIMARY KEY,
    group_key VARCHAR(100) NOT NULL,
    group_name VARCHAR(200) NOT NULL,
    description TEXT,
    app_name VARCHAR(100) NOT NULL,
    display_order INTEGER DEFAULT 0,
    active BOOLEAN DEFAULT true,
    corporate_id BIGINT NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100) NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_config_group UNIQUE (group_key, app_name, corporate_id)
);

-- App Configuration Items
CREATE TABLE app_config (
    id BIGSERIAL PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL,
    config_name VARCHAR(200) NOT NULL,
    description TEXT,
    config_type VARCHAR(50) NOT NULL, -- TEXT, NUMBER, BOOLEAN, ENUM, JSON, TEMPLATE, LIST
    config_value TEXT,
    default_value TEXT,
    enum_values TEXT, -- JSON array for ENUM type
    validation_rules TEXT, -- JSON object for validation
    is_public BOOLEAN DEFAULT false, -- Can be exposed to frontend
    is_required BOOLEAN DEFAULT false,
    display_order INTEGER DEFAULT 0,
    group_id BIGINT,
    app_name VARCHAR(100) NOT NULL,
    active BOOLEAN DEFAULT true,
    corporate_id BIGINT NOT NULL,
    version INTEGER DEFAULT 1,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100) NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_config_group FOREIGN KEY (group_id) REFERENCES app_config_group(id) ON DELETE SET NULL,
    CONSTRAINT uk_config_key UNIQUE (config_key, app_name, corporate_id)
);

-- App Configuration Version History
CREATE TABLE app_config_version (
    id BIGSERIAL PRIMARY KEY,
    config_id BIGINT NOT NULL,
    version INTEGER NOT NULL,
    config_value TEXT,
    change_type VARCHAR(50) NOT NULL, -- CREATE, UPDATE, DELETE
    change_description TEXT,
    changed_by VARCHAR(100) NOT NULL,
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    metadata TEXT, -- JSON for additional info
    CONSTRAINT fk_config_version FOREIGN KEY (config_id) REFERENCES app_config(id) ON DELETE CASCADE
);

-- App Configuration Audit Log
CREATE TABLE app_config_audit (
    id BIGSERIAL PRIMARY KEY,
    config_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL, -- VIEW, CREATE, UPDATE, DELETE, RESTORE
    old_value TEXT,
    new_value TEXT,
    user_email VARCHAR(255),
    ip_address VARCHAR(50),
    user_agent TEXT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_config_audit FOREIGN KEY (config_id) REFERENCES app_config(id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX idx_config_group_app ON app_config_group(app_name, corporate_id);
CREATE INDEX idx_config_group_active ON app_config_group(active);
CREATE INDEX idx_config_app ON app_config(app_name, corporate_id);
CREATE INDEX idx_config_group_id ON app_config(group_id);
CREATE INDEX idx_config_active ON app_config(active);
CREATE INDEX idx_config_public ON app_config(is_public);
CREATE INDEX idx_config_version_config ON app_config_version(config_id);
CREATE INDEX idx_config_audit_config ON app_config_audit(config_id);
CREATE INDEX idx_config_audit_timestamp ON app_config_audit(timestamp);

-- Comments
COMMENT ON TABLE app_config_group IS 'Configuration groups for organizing related settings';
COMMENT ON TABLE app_config IS 'Dynamic application configuration items';
COMMENT ON TABLE app_config_version IS 'Version history for configuration changes';
COMMENT ON TABLE app_config_audit IS 'Audit log for configuration access and modifications';

COMMENT ON COLUMN app_config.config_type IS 'TEXT, NUMBER, BOOLEAN, ENUM, JSON, TEMPLATE, LIST';
COMMENT ON COLUMN app_config.is_public IS 'If true, can be exposed to frontend clients';
COMMENT ON COLUMN app_config.validation_rules IS 'JSON object with validation rules (min, max, pattern, etc.)';
COMMENT ON COLUMN app_config.enum_values IS 'JSON array of allowed values for ENUM type';
