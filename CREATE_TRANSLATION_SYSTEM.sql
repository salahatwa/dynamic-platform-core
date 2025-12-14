-- Translation Management System Database Schema
-- Phase 1: Core Tables

-- 1. Translation Apps Table
CREATE TABLE IF NOT EXISTS translation_apps (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    corporate_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    api_key VARCHAR(255) UNIQUE NOT NULL,
    default_language VARCHAR(10) NOT NULL DEFAULT 'en',
    supported_languages JSON,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (corporate_id) REFERENCES corporates(id) ON DELETE CASCADE,
    UNIQUE KEY unique_app_per_corporate (corporate_id, name),
    INDEX idx_corporate_id (corporate_id),
    INDEX idx_api_key (api_key),
    INDEX idx_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Translation Keys Table
CREATE TABLE IF NOT EXISTS translation_keys (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    app_id BIGINT NOT NULL,
    key_name VARCHAR(500) NOT NULL,
    description TEXT,
    context TEXT,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (app_id) REFERENCES translation_apps(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    UNIQUE KEY unique_key_per_app (app_id, key_name),
    INDEX idx_app_id (app_id),
    INDEX idx_key_name (key_name(255)),
    INDEX idx_created_by (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Translations Table
CREATE TABLE IF NOT EXISTS translations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    key_id BIGINT NOT NULL,
    language VARCHAR(10) NOT NULL,
    value TEXT NOT NULL,
    status ENUM('draft', 'pending', 'approved', 'published') DEFAULT 'published',
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (key_id) REFERENCES translation_keys(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL,
    UNIQUE KEY unique_translation (key_id, language),
    INDEX idx_key_id (key_id),
    INDEX idx_language (language),
    INDEX idx_status (status),
    INDEX idx_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Translation Versions Table
CREATE TABLE IF NOT EXISTS translation_versions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    app_id BIGINT NOT NULL,
    version INT NOT NULL,
    changelog TEXT,
    snapshot LONGTEXT,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (app_id) REFERENCES translation_apps(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    UNIQUE KEY unique_version_per_app (app_id, version),
    INDEX idx_app_id (app_id),
    INDEX idx_version (version),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Translation Audit Logs Table
CREATE TABLE IF NOT EXISTS translation_audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    app_id BIGINT NOT NULL,
    key_id BIGINT,
    language VARCHAR(10),
    action ENUM('create', 'update', 'delete', 'import', 'export', 'rollback') NOT NULL,
    old_value TEXT,
    new_value TEXT,
    user_id BIGINT,
    user_email VARCHAR(255),
    ip_address VARCHAR(45),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (app_id) REFERENCES translation_apps(id) ON DELETE CASCADE,
    FOREIGN KEY (key_id) REFERENCES translation_keys(id) ON DELETE SET NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_app_id (app_id),
    INDEX idx_key_id (key_id),
    INDEX idx_user_id (user_id),
    INDEX idx_timestamp (timestamp),
    INDEX idx_action (action),
    INDEX idx_app_timestamp (app_id, timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Sample Data (Optional - for testing)
-- INSERT INTO translation_apps (corporate_id, name, description, api_key, default_language, supported_languages, active)
-- VALUES (1, 'Demo App', 'Demo application for testing', 'tms_demo_key_12345', 'en', '["en", "ar", "fr"]', TRUE);

-- Verify tables created
SELECT 
    TABLE_NAME, 
    TABLE_ROWS, 
    CREATE_TIME 
FROM 
    information_schema.TABLES 
WHERE 
    TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME LIKE 'translation%'
ORDER BY 
    TABLE_NAME;
