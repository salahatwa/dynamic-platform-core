-- Error Code Categories Table
CREATE TABLE error_code_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_code VARCHAR(50) NOT NULL,
    category_name VARCHAR(100) NOT NULL,
    description TEXT,
    display_order INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    corporate_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    UNIQUE KEY uk_category_code_corporate (category_code, corporate_id),
    INDEX idx_corporate (corporate_id),
    INDEX idx_active (is_active)
);

-- Error Codes Table
CREATE TABLE error_code (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    error_code VARCHAR(50) NOT NULL,
    category_id BIGINT,
    app_name VARCHAR(100) NOT NULL,
    module_name VARCHAR(100),
    severity VARCHAR(20) NOT NULL, -- INFO, WARNING, ERROR, CRITICAL
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, DEPRECATED, REMOVED
    http_status_code INT,
    is_public BOOLEAN DEFAULT TRUE,
    is_retryable BOOLEAN DEFAULT FALSE,
    default_message TEXT NOT NULL,
    technical_details TEXT,
    resolution_steps TEXT,
    documentation_url VARCHAR(500),
    corporate_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    UNIQUE KEY uk_error_code_corporate (error_code, corporate_id),
    FOREIGN KEY (category_id) REFERENCES error_code_category(id) ON DELETE SET NULL,
    INDEX idx_app_name (app_name),
    INDEX idx_module (module_name),
    INDEX idx_severity (severity),
    INDEX idx_status (status),
    INDEX idx_corporate (corporate_id),
    INDEX idx_category (category_id)
);

-- Error Code Translations Table
CREATE TABLE error_code_translation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    error_code_id BIGINT NOT NULL,
    language_code VARCHAR(10) NOT NULL,
    message TEXT NOT NULL,
    technical_details TEXT,
    resolution_steps TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (error_code_id) REFERENCES error_code(id) ON DELETE CASCADE,
    UNIQUE KEY uk_error_translation (error_code_id, language_code),
    INDEX idx_language (language_code)
);

-- Error Code Version History Table
CREATE TABLE error_code_version (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    error_code_id BIGINT NOT NULL,
    version_number INT NOT NULL,
    error_code VARCHAR(50) NOT NULL,
    category_id BIGINT,
    app_name VARCHAR(100) NOT NULL,
    module_name VARCHAR(100),
    severity VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    http_status_code INT,
    is_public BOOLEAN,
    is_retryable BOOLEAN,
    default_message TEXT NOT NULL,
    technical_details TEXT,
    resolution_steps TEXT,
    documentation_url VARCHAR(500),
    change_description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    FOREIGN KEY (error_code_id) REFERENCES error_code(id) ON DELETE CASCADE,
    INDEX idx_error_code (error_code_id),
    INDEX idx_version (version_number)
);

-- Error Code Audit Log Table
CREATE TABLE error_code_audit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    error_code_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL, -- CREATE, UPDATE, DELETE, STATUS_CHANGE
    field_name VARCHAR(100),
    old_value TEXT,
    new_value TEXT,
    changed_by VARCHAR(100),
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    FOREIGN KEY (error_code_id) REFERENCES error_code(id) ON DELETE CASCADE,
    INDEX idx_error_code (error_code_id),
    INDEX idx_action (action),
    INDEX idx_changed_at (changed_at)
);

-- Insert default categories
INSERT INTO error_code_category (category_code, category_name, description, display_order, corporate_id) VALUES
('AUTHENTICATION', 'Authentication', 'Authentication and authorization errors', 1, NULL),
('VALIDATION', 'Validation', 'Input validation and data integrity errors', 2, NULL),
('DATABASE', 'Database', 'Database connection and query errors', 3, NULL),
('NETWORK', 'Network', 'Network and connectivity errors', 4, NULL),
('BUSINESS_LOGIC', 'Business Logic', 'Business rule violations', 5, NULL),
('SYSTEM', 'System', 'System and infrastructure errors', 6, NULL),
('INTEGRATION', 'Integration', 'Third-party integration errors', 7, NULL);

-- Insert sample error codes
INSERT INTO error_code (error_code, category_id, app_name, module_name, severity, status, http_status_code, is_public, is_retryable, default_message, technical_details, resolution_steps, corporate_id) VALUES
('AUTH_001', 1, 'PLATFORM', 'Authentication', 'ERROR', 'ACTIVE', 401, TRUE, FALSE, 'Invalid credentials', 'Username or password is incorrect', 'Please check your credentials and try again', NULL),
('AUTH_002', 1, 'PLATFORM', 'Authentication', 'ERROR', 'ACTIVE', 401, TRUE, FALSE, 'Token expired', 'JWT token has expired', 'Please login again to get a new token', NULL),
('VAL_001', 2, 'PLATFORM', 'Validation', 'WARNING', 'ACTIVE', 400, TRUE, FALSE, 'Required field missing', 'One or more required fields are missing', 'Please fill all required fields', NULL),
('DB_001', 3, 'PLATFORM', 'Database', 'CRITICAL', 'ACTIVE', 500, FALSE, TRUE, 'Database connection failed', 'Unable to establish database connection', 'Check database server status and connection settings', NULL),
('SYS_001', 6, 'PLATFORM', 'System', 'CRITICAL', 'ACTIVE', 500, FALSE, TRUE, 'Internal server error', 'Unexpected system error occurred', 'Contact system administrator', NULL);
