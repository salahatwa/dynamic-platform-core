-- ============================================
-- User Invitation System - Database Schema
-- ============================================

-- 1. Create invitations table
CREATE TABLE IF NOT EXISTS invitations (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(64) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL,
    corporate_id BIGINT NOT NULL REFERENCES corporates(id),
    invited_by_id BIGINT NOT NULL REFERENCES users(id),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    expires_at TIMESTAMP NOT NULL,
    accepted_at TIMESTAMP,
    accepted_by_id BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_invitation_status CHECK (status IN ('PENDING', 'ACCEPTED', 'EXPIRED', 'CANCELLED'))
);

-- Create indexes for invitations table
CREATE INDEX IF NOT EXISTS idx_invitations_token ON invitations(token);
CREATE INDEX IF NOT EXISTS idx_invitations_email ON invitations(email);
CREATE INDEX IF NOT EXISTS idx_invitations_corporate_id ON invitations(corporate_id);
CREATE INDEX IF NOT EXISTS idx_invitations_status ON invitations(status);
CREATE INDEX IF NOT EXISTS idx_invitations_expires_at ON invitations(expires_at);

-- 2. Create invitation_roles junction table
CREATE TABLE IF NOT EXISTS invitation_roles (
    invitation_id BIGINT NOT NULL REFERENCES invitations(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id),
    PRIMARY KEY (invitation_id, role_id)
);

-- Create indexes for invitation_roles table
CREATE INDEX IF NOT EXISTS idx_invitation_roles_invitation_id ON invitation_roles(invitation_id);
CREATE INDEX IF NOT EXISTS idx_invitation_roles_role_id ON invitation_roles(role_id);

-- 3. Enhance users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS invitation_accepted_at TIMESTAMP;

-- Create indexes for users table
CREATE INDEX IF NOT EXISTS idx_users_invited_by_id ON users(invited_by_id);
CREATE INDEX IF NOT EXISTS idx_users_corporate_id ON users(corporate_id);

-- ============================================
-- Verification Queries
-- ============================================

-- Verify tables created
SELECT table_name 
FROM information_schema.tables 
WHERE table_schema = 'public' 
AND table_name IN ('invitations', 'invitation_roles');

-- Verify indexes created
SELECT indexname 
FROM pg_indexes 
WHERE schemaname = 'public' 
AND tablename IN ('invitations', 'invitation_roles', 'users')
AND indexname LIKE 'idx_invitation%';

-- Verify columns added to users table
SELECT column_name, data_type 
FROM information_schema.columns 
WHERE table_name = 'users' 
AND column_name IN ('invited_by_id', 'invitation_accepted_at');
