-- LOV Main Table
CREATE TABLE lov (
    id BIGSERIAL PRIMARY KEY,
    lov_code VARCHAR(100) NOT NULL,
    lov_type VARCHAR(50) NOT NULL,
    value VARCHAR(500) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    parent_lov_id BIGINT,
    translation_key VARCHAR(200),
    metadata TEXT,
    corporate_id BIGINT,
    version INTEGER NOT NULL DEFAULT 1,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_parent_lov FOREIGN KEY (parent_lov_id) REFERENCES lov(id) ON DELETE SET NULL,
    CONSTRAINT fk_corporate FOREIGN KEY (corporate_id) REFERENCES corporate(id) ON DELETE CASCADE,
    CONSTRAINT uk_lov_code_corporate UNIQUE (lov_code, corporate_id)
);

-- Indexes for performance
CREATE INDEX idx_lov_code ON lov(lov_code);
CREATE INDEX idx_lov_type ON lov(lov_type);
CREATE INDEX idx_corporate_id ON lov(corporate_id);
CREATE INDEX idx_lov_active ON lov(active);
CREATE INDEX idx_lov_type_corporate ON lov(lov_type, corporate_id);

-- LOV Version Table
CREATE TABLE lov_version (
    id BIGSERIAL PRIMARY KEY,
    lov_id BIGINT NOT NULL,
    version INTEGER NOT NULL,
    lov_code VARCHAR(100) NOT NULL,
    lov_type VARCHAR(50) NOT NULL,
    value VARCHAR(500) NOT NULL,
    display_order INTEGER NOT NULL,
    active BOOLEAN NOT NULL,
    translation_key VARCHAR(200),
    metadata TEXT,
    changed_by VARCHAR(100) NOT NULL,
    changed_at TIMESTAMP NOT NULL,
    change_type VARCHAR(20) NOT NULL CHECK (change_type IN ('CREATE', 'UPDATE', 'DELETE')),
    change_description VARCHAR(500),
    CONSTRAINT fk_lov_version FOREIGN KEY (lov_id) REFERENCES lov(id) ON DELETE CASCADE
);

CREATE INDEX idx_lov_version_lov_id ON lov_version(lov_id);
CREATE INDEX idx_lov_version_version ON lov_version(version);

-- LOV Audit Table
CREATE TABLE lov_audit (
    id BIGSERIAL PRIMARY KEY,
    lov_id BIGINT NOT NULL,
    action VARCHAR(20) NOT NULL CHECK (action IN ('CREATE', 'UPDATE', 'DELETE', 'VIEW')),
    user_id BIGINT NOT NULL,
    user_name VARCHAR(100) NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    changes TEXT,
    CONSTRAINT fk_lov_audit FOREIGN KEY (lov_id) REFERENCES lov(id) ON DELETE CASCADE
);

CREATE INDEX idx_lov_audit_lov_id ON lov_audit(lov_id);
CREATE INDEX idx_lov_audit_timestamp ON lov_audit(timestamp);
CREATE INDEX idx_lov_audit_user_id ON lov_audit(user_id);

-- Insert sample LOV types
INSERT INTO lov (lov_code, lov_type, value, display_order, active, translation_key, corporate_id, created_by, updated_by) VALUES
('COUNTRY_US', 'COUNTRY', 'United States', 10, TRUE, 'country.us', 1, 'system', 'system'),
('COUNTRY_UK', 'COUNTRY', 'United Kingdom', 20, TRUE, 'country.uk', 1, 'system', 'system'),
('COUNTRY_SA', 'COUNTRY', 'Saudi Arabia', 30, TRUE, 'country.sa', 1, 'system', 'system'),
('MARKET_OPEN', 'MARKET_STATUS', 'Open', 10, TRUE, 'market.status.open', 1, 'system', 'system'),
('MARKET_CLOSED', 'MARKET_STATUS', 'Closed', 20, TRUE, 'market.status.closed', 1, 'system', 'system'),
('MARKET_PENDING', 'MARKET_STATUS', 'Pending', 30, TRUE, 'market.status.pending', 1, 'system', 'system'),
('ROLE_ADMIN', 'USER_ROLE', 'Administrator', 10, TRUE, 'role.admin', 1, 'system', 'system'),
('ROLE_USER', 'USER_ROLE', 'User', 20, TRUE, 'role.user', 1, 'system', 'system'),
('ROLE_VIEWER', 'USER_ROLE', 'Viewer', 30, TRUE, 'role.viewer', 1, 'system', 'system');

-- Comments
COMMENT ON TABLE lov IS 'List of Values - Reference data and lookup values';
COMMENT ON TABLE lov_version IS 'Version history for LOV entries';
COMMENT ON TABLE lov_audit IS 'Audit trail for LOV operations';
COMMENT ON COLUMN lov.lov_code IS 'Unique code for the LOV entry';
COMMENT ON COLUMN lov.lov_type IS 'Type/category of the LOV';
COMMENT ON COLUMN lov.metadata IS 'Additional JSON metadata';
COMMENT ON COLUMN lov.corporate_id IS 'Corporate isolation - each corporate has separate LOVs';
