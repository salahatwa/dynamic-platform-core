-- Translation Management System Database Schema for PostgreSQL
-- Run this script in your PostgreSQL database

-- 1. Translation Apps Table
CREATE TABLE IF NOT EXISTS translation_apps (
    id BIGSERIAL PRIMARY KEY,
    corporate_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    api_key VARCHAR(255) UNIQUE NOT NULL,
    default_language VARCHAR(10) NOT NULL DEFAULT 'en',
    supported_languages JSONB,
    active BOOLEAN DEFAULT TRUE,
    keys_count INTEGER DEFAULT 0,
    translations_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (corporate_id) REFERENCES corporates(id) ON DELETE CASCADE,
    UNIQUE (corporate_id, name)
);

CREATE INDEX IF NOT EXISTS idx_translation_apps_corporate_id ON translation_apps(corporate_id);
CREATE INDEX IF NOT EXISTS idx_translation_apps_api_key ON translation_apps(api_key);
CREATE INDEX IF NOT EXISTS idx_translation_apps_active ON translation_apps(active);

-- 2. Translation Keys Table
CREATE TABLE IF NOT EXISTS translation_keys (
    id BIGSERIAL PRIMARY KEY,
    app_id BIGINT NOT NULL,
    key_name VARCHAR(500) NOT NULL,
    description TEXT,
    context TEXT,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (app_id) REFERENCES translation_apps(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    UNIQUE (app_id, key_name)
);

CREATE INDEX IF NOT EXISTS idx_translation_keys_app_id ON translation_keys(app_id);
CREATE INDEX IF NOT EXISTS idx_translation_keys_key_name ON translation_keys(key_name);
CREATE INDEX IF NOT EXISTS idx_translation_keys_created_by ON translation_keys(created_by);

-- 3. Translations Table
CREATE TABLE IF NOT EXISTS translations (
    id BIGSERIAL PRIMARY KEY,
    key_id BIGINT NOT NULL,
    language VARCHAR(10) NOT NULL,
    value TEXT NOT NULL,
    status VARCHAR(20) DEFAULT 'published',
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (key_id) REFERENCES translation_keys(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL,
    UNIQUE (key_id, language)
);

CREATE INDEX IF NOT EXISTS idx_translations_key_id ON translations(key_id);
CREATE INDEX IF NOT EXISTS idx_translations_language ON translations(language);
CREATE INDEX IF NOT EXISTS idx_translations_status ON translations(status);

-- 4. Translation Versions Table
CREATE TABLE IF NOT EXISTS translation_versions (
    id BIGSERIAL PRIMARY KEY,
    app_id BIGINT NOT NULL,
    version INTEGER NOT NULL,
    snapshot JSONB NOT NULL,
    changelog TEXT,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (app_id) REFERENCES translation_apps(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    UNIQUE (app_id, version)
);

CREATE INDEX IF NOT EXISTS idx_translation_versions_app_id ON translation_versions(app_id);
CREATE INDEX IF NOT EXISTS idx_translation_versions_created_at ON translation_versions(created_at);

-- 5. Translation Audit Logs Table
CREATE TABLE IF NOT EXISTS translation_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    app_id BIGINT NOT NULL,
    key_id BIGINT,
    translation_id BIGINT,
    action VARCHAR(50) NOT NULL,
    old_value TEXT,
    new_value TEXT,
    changed_by BIGINT,
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent TEXT,
    FOREIGN KEY (app_id) REFERENCES translation_apps(id) ON DELETE CASCADE,
    FOREIGN KEY (key_id) REFERENCES translation_keys(id) ON DELETE SET NULL,
    FOREIGN KEY (translation_id) REFERENCES translations(id) ON DELETE SET NULL,
    FOREIGN KEY (changed_by) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_translation_audit_logs_app_id ON translation_audit_logs(app_id);
CREATE INDEX IF NOT EXISTS idx_translation_audit_logs_key_id ON translation_audit_logs(key_id);
CREATE INDEX IF NOT EXISTS idx_translation_audit_logs_changed_at ON translation_audit_logs(changed_at);
CREATE INDEX IF NOT EXISTS idx_translation_audit_logs_changed_by ON translation_audit_logs(changed_by);

-- Create trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_translation_apps_updated_at BEFORE UPDATE ON translation_apps
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_translation_keys_updated_at BEFORE UPDATE ON translation_keys
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_translations_updated_at BEFORE UPDATE ON translations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
