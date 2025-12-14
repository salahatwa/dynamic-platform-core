-- V12: Create Apps and Subscriptions Tables for App-Centric Architecture

-- =====================================================
-- 1. Create Apps Table
-- =====================================================
CREATE TABLE apps (
    id BIGSERIAL PRIMARY KEY,
    corporate_id BIGINT NOT NULL REFERENCES corporates(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    app_key VARCHAR(100) UNIQUE NOT NULL,
    icon_url VARCHAR(500),
    status VARCHAR(50) DEFAULT 'ACTIVE' NOT NULL,
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uk_app_corporate_name UNIQUE (corporate_id, name),
    CONSTRAINT chk_app_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED'))
);

CREATE INDEX idx_apps_corporate ON apps(corporate_id);
CREATE INDEX idx_apps_status ON apps(status);
CREATE INDEX idx_apps_key ON apps(app_key);
CREATE INDEX idx_apps_created_at ON apps(created_at DESC);

COMMENT ON TABLE apps IS 'Applications managed by the platform';
COMMENT ON COLUMN apps.app_key IS 'Unique identifier for API access';
COMMENT ON COLUMN apps.status IS 'ACTIVE, INACTIVE, or ARCHIVED';

-- =====================================================
-- 2. Create Subscriptions Table
-- =====================================================
CREATE TABLE subscriptions (
    id BIGSERIAL PRIMARY KEY,
    corporate_id BIGINT NOT NULL REFERENCES corporates(id) ON DELETE CASCADE,
    tier VARCHAR(50) NOT NULL,
    max_apps INT NOT NULL,
    max_users INT NOT NULL,
    max_api_requests_per_month BIGINT NOT NULL,
    features JSONB DEFAULT '{}',
    status VARCHAR(50) DEFAULT 'ACTIVE' NOT NULL,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uk_subscription_corporate UNIQUE (corporate_id),
    CONSTRAINT chk_subscription_tier CHECK (tier IN ('FREE', 'PRO', 'TEAM', 'ENTERPRISE')),
    CONSTRAINT chk_subscription_status CHECK (status IN ('ACTIVE', 'EXPIRED', 'CANCELLED', 'SUSPENDED'))
);

CREATE INDEX idx_subscriptions_corporate ON subscriptions(corporate_id);
CREATE INDEX idx_subscriptions_tier ON subscriptions(tier);
CREATE INDEX idx_subscriptions_status ON subscriptions(status);
CREATE INDEX idx_subscriptions_expires_at ON subscriptions(expires_at);

COMMENT ON TABLE subscriptions IS 'Subscription plans and limits for corporates';
COMMENT ON COLUMN subscriptions.tier IS 'FREE, PRO, TEAM, or ENTERPRISE';
COMMENT ON COLUMN subscriptions.max_apps IS 'Maximum number of apps allowed';
COMMENT ON COLUMN subscriptions.max_users IS 'Maximum number of users allowed (-1 for unlimited)';
COMMENT ON COLUMN subscriptions.max_api_requests_per_month IS 'Maximum API requests per month (-1 for unlimited)';
COMMENT ON COLUMN subscriptions.features IS 'Additional features as JSON';

-- =====================================================
-- 3. Create API Usage Tracking Table
-- =====================================================
CREATE TABLE api_usage (
    id BIGSERIAL PRIMARY KEY,
    corporate_id BIGINT NOT NULL REFERENCES corporates(id) ON DELETE CASCADE,
    app_id BIGINT REFERENCES apps(id) ON DELETE CASCADE,
    year_month VARCHAR(7) NOT NULL, -- Format: YYYY-MM
    request_count BIGINT DEFAULT 0 NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uk_api_usage_corporate_month UNIQUE (corporate_id, year_month)
);

CREATE INDEX idx_api_usage_corporate ON api_usage(corporate_id);
CREATE INDEX idx_api_usage_year_month ON api_usage(year_month);

COMMENT ON TABLE api_usage IS 'Track API usage per corporate per month';

-- =====================================================
-- 4. Create Default App for Each Existing Corporate
-- =====================================================
INSERT INTO apps (corporate_id, name, description, app_key, status, created_at, updated_at)
SELECT 
    c.id,
    c.name || ' App',
    'Default application for ' || c.name,
    'app_' || LOWER(REGEXP_REPLACE(c.name, '[^a-zA-Z0-9]', '_', 'g')) || '_' || c.id,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM corporates c
WHERE c.active = true;

-- =====================================================
-- 5. Create Default FREE Subscription for Each Corporate
-- =====================================================
INSERT INTO subscriptions (
    corporate_id, 
    tier, 
    max_apps, 
    max_users, 
    max_api_requests_per_month,
    features,
    status,
    started_at,
    created_at,
    updated_at
)
SELECT 
    c.id,
    'FREE',
    1,
    2,
    1000,
    '{"basic_translations": true, "basic_templates": true, "community_support": true}'::jsonb,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM corporates c
WHERE c.active = true
ON CONFLICT (corporate_id) DO NOTHING;

-- =====================================================
-- 6. Add app_id Column to Existing Tables
-- =====================================================

-- 6.1 Translation Keys
ALTER TABLE translation_keys ADD COLUMN app_id BIGINT;

-- Migrate existing translation keys to default app
UPDATE translation_keys tk
SET app_id = (
    SELECT a.id 
    FROM apps a 
    WHERE a.corporate_id = (
        SELECT u.corporate_id 
        FROM users u 
        WHERE u.id = tk.created_by
        LIMIT 1
    )
    LIMIT 1
);

-- For any remaining null app_ids, assign to first app of their corporate
UPDATE translation_keys tk
SET app_id = (
    SELECT a.id 
    FROM apps a 
    INNER JOIN translation_apps ta ON ta.id = tk.app_id_old
    WHERE a.corporate_id = (
        SELECT corporate_id FROM translation_apps WHERE id = tk.app_id_old
    )
    LIMIT 1
)
WHERE app_id IS NULL AND app_id_old IS NOT NULL;

-- Set to first available app for any remaining nulls
UPDATE translation_keys tk
SET app_id = (SELECT id FROM apps LIMIT 1)
WHERE app_id IS NULL;

ALTER TABLE translation_keys ALTER COLUMN app_id SET NOT NULL;
ALTER TABLE translation_keys ADD CONSTRAINT fk_translation_keys_app 
    FOREIGN KEY (app_id) REFERENCES apps(id) ON DELETE CASCADE;
CREATE INDEX idx_translation_keys_app ON translation_keys(app_id);

-- 6.2 Templates
ALTER TABLE templates ADD COLUMN app_id BIGINT;

UPDATE templates t
SET app_id = (
    SELECT a.id 
    FROM apps a 
    WHERE a.corporate_id = t.corporate_id
    LIMIT 1
);

ALTER TABLE templates ALTER COLUMN app_id SET NOT NULL;
ALTER TABLE templates ADD CONSTRAINT fk_templates_app 
    FOREIGN KEY (app_id) REFERENCES apps(id) ON DELETE CASCADE;
CREATE INDEX idx_templates_app ON templates(app_id);

-- 6.3 LOVs
ALTER TABLE lovs ADD COLUMN app_id BIGINT;

UPDATE lovs l
SET app_id = (
    SELECT a.id 
    FROM apps a 
    WHERE a.corporate_id = l.corporate_id
    LIMIT 1
);

ALTER TABLE lovs ALTER COLUMN app_id SET NOT NULL;
ALTER TABLE lovs ADD CONSTRAINT fk_lovs_app 
    FOREIGN KEY (app_id) REFERENCES apps(id) ON DELETE CASCADE;
CREATE INDEX idx_lovs_app ON lovs(app_id);

-- 6.4 App Configs
ALTER TABLE app_configs ADD COLUMN app_id BIGINT;

UPDATE app_configs ac
SET app_id = (
    SELECT a.id 
    FROM apps a 
    WHERE a.corporate_id = ac.corporate_id
    LIMIT 1
);

ALTER TABLE app_configs ALTER COLUMN app_id SET NOT NULL;
ALTER TABLE app_configs ADD CONSTRAINT fk_app_configs_app 
    FOREIGN KEY (app_id) REFERENCES apps(id) ON DELETE CASCADE;
CREATE INDEX idx_app_configs_app ON app_configs(app_id);

-- Remove redundant app_name column
ALTER TABLE app_configs DROP COLUMN IF EXISTS app_name;

-- 6.5 Error Codes
ALTER TABLE error_codes ADD COLUMN app_id BIGINT;

UPDATE error_codes ec
SET app_id = (
    SELECT a.id 
    FROM apps a 
    WHERE a.corporate_id = ec.corporate_id
    LIMIT 1
);

ALTER TABLE error_codes ALTER COLUMN app_id SET NOT NULL;
ALTER TABLE error_codes ADD CONSTRAINT fk_error_codes_app 
    FOREIGN KEY (app_id) REFERENCES apps(id) ON DELETE CASCADE;
CREATE INDEX idx_error_codes_app ON error_codes(app_id);

-- Remove redundant app_name column
ALTER TABLE error_codes DROP COLUMN IF EXISTS app_name;

-- =====================================================
-- 7. Create App Audit Table
-- =====================================================
CREATE TABLE app_audit (
    id BIGSERIAL PRIMARY KEY,
    app_id BIGINT NOT NULL REFERENCES apps(id) ON DELETE CASCADE,
    action VARCHAR(50) NOT NULL,
    changed_by BIGINT REFERENCES users(id),
    old_values JSONB,
    new_values JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_app_audit_action CHECK (action IN ('CREATED', 'UPDATED', 'ARCHIVED', 'RESTORED'))
);

CREATE INDEX idx_app_audit_app ON app_audit(app_id);
CREATE INDEX idx_app_audit_created_at ON app_audit(created_at DESC);

COMMENT ON TABLE app_audit IS 'Audit trail for app changes';

-- =====================================================
-- 8. Create Function to Update updated_at Timestamp
-- =====================================================
CREATE OR REPLACE FUNCTION update_apps_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_apps_updated_at
    BEFORE UPDATE ON apps
    FOR EACH ROW
    EXECUTE FUNCTION update_apps_updated_at();

CREATE TRIGGER trigger_subscriptions_updated_at
    BEFORE UPDATE ON subscriptions
    FOR EACH ROW
    EXECUTE FUNCTION update_apps_updated_at();

-- =====================================================
-- 9. Grant Permissions (if using role-based access)
-- =====================================================
-- GRANT SELECT, INSERT, UPDATE, DELETE ON apps TO app_user;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON subscriptions TO app_user;
-- GRANT SELECT, INSERT, UPDATE ON api_usage TO app_user;
-- GRANT SELECT, INSERT ON app_audit TO app_user;

-- =====================================================
-- 10. Insert Sample Data for Testing (Optional)
-- =====================================================
-- Uncomment for development/testing environments
/*
-- Create a test corporate if not exists
INSERT INTO corporates (name, domain, active, created_at, updated_at)
VALUES ('Test Company', 'test.com', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- Create test apps
INSERT INTO apps (corporate_id, name, description, app_key, status, created_at, updated_at)
SELECT 
    c.id,
    'Mobile App',
    'iOS and Android mobile application',
    'app_mobile_' || c.id,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM corporates c
WHERE c.name = 'Test Company'
LIMIT 1;

INSERT INTO apps (corporate_id, name, description, app_key, status, created_at, updated_at)
SELECT 
    c.id,
    'Web Portal',
    'Customer web portal',
    'app_web_' || c.id,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM corporates c
WHERE c.name = 'Test Company'
LIMIT 1;
*/

-- =====================================================
-- Migration Complete
-- =====================================================
-- Summary:
-- ✅ Created apps table with proper constraints
-- ✅ Created subscriptions table with tier limits
-- ✅ Created api_usage tracking table
-- ✅ Created default app for each existing corporate
-- ✅ Created default FREE subscription for each corporate
-- ✅ Added app_id to all module tables
-- ✅ Migrated existing data to default apps
-- ✅ Created audit table for app changes
-- ✅ Added triggers for timestamp updates
