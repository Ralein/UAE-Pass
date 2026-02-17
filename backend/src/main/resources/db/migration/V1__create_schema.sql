-- ============================================
-- UAE Digital Identity Platform - Schema V1
-- Production-grade normalized schema
-- ============================================

-- Enable UUID generation
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================
-- USERS TABLE
-- All PII identifiers stored as hashes
-- ============================================
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    emirates_id_hash VARCHAR(128) NOT NULL UNIQUE,
    email_hash       VARCHAR(128) NOT NULL,
    phone_hash       VARCHAR(128) NOT NULL,
    full_name_enc    TEXT,                          -- AES-GCM encrypted
    gender           VARCHAR(10),
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    account_level    VARCHAR(10) NOT NULL DEFAULT 'SOP1',
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_user_status CHECK (status IN ('PENDING', 'OTP_SENT', 'OTP_VERIFIED', 'ACTIVE', 'LOCKED', 'SUSPENDED')),
    CONSTRAINT chk_account_level CHECK (account_level IN ('SOP1', 'SOP2', 'SOP3'))
);

CREATE INDEX idx_users_email_hash ON users (email_hash);
CREATE INDEX idx_users_phone_hash ON users (phone_hash);
CREATE INDEX idx_users_status ON users (status);

-- ============================================
-- CREDENTIALS TABLE
-- PIN stored with Argon2id hash
-- ============================================
CREATE TABLE credentials (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    pin_hash        VARCHAR(512) NOT NULL,           -- Argon2id hash output
    hash_algorithm  VARCHAR(30) NOT NULL DEFAULT 'ARGON2ID',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================
-- OTP CHALLENGES
-- OTP stored hashed, with attempt tracking
-- ============================================
CREATE TABLE otp_challenges (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    otp_hash        VARCHAR(128) NOT NULL,           -- SHA-256 hash of OTP
    channel         VARCHAR(10) NOT NULL,
    expires_at      TIMESTAMPTZ NOT NULL,
    attempts        INT NOT NULL DEFAULT 0,
    max_attempts    INT NOT NULL DEFAULT 5,
    consumed        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_otp_channel CHECK (channel IN ('SMS', 'EMAIL'))
);

CREATE INDEX idx_otp_user_channel ON otp_challenges (user_id, channel, consumed);
CREATE INDEX idx_otp_expires ON otp_challenges (expires_at);

-- ============================================
-- CONSENTS
-- OAuth2 consent grants
-- ============================================
CREATE TABLE consents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    client_id       VARCHAR(256) NOT NULL,
    scopes          TEXT NOT NULL,                    -- comma-separated scopes
    granted_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    revoked_at      TIMESTAMPTZ,

    CONSTRAINT uq_consent_user_client UNIQUE (user_id, client_id)
);

-- ============================================
-- AUDIT LOGS
-- Append-only, immutable security event log
-- ============================================
CREATE TABLE audit_logs (
    id                      BIGSERIAL PRIMARY KEY,
    event_type              VARCHAR(50) NOT NULL,
    user_id                 UUID,
    request_id              VARCHAR(64),
    ip_address              VARCHAR(45),
    device_fingerprint_hash VARCHAR(128),
    details                 JSONB,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_event_type ON audit_logs (event_type);
CREATE INDEX idx_audit_user_id ON audit_logs (user_id);
CREATE INDEX idx_audit_created_at ON audit_logs (created_at DESC);
CREATE INDEX idx_audit_request_id ON audit_logs (request_id);

-- ============================================
-- TOKEN METADATA
-- Track issued tokens for revocation/replay detection
-- ============================================
CREATE TABLE token_metadata (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_jti       VARCHAR(128) NOT NULL UNIQUE,    -- JWT ID for replay detection
    token_type      VARCHAR(20) NOT NULL,
    client_id       VARCHAR(256) NOT NULL,
    issued_at       TIMESTAMPTZ NOT NULL,
    expires_at      TIMESTAMPTZ NOT NULL,
    revoked         BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at      TIMESTAMPTZ,

    CONSTRAINT chk_token_type CHECK (token_type IN ('ACCESS', 'REFRESH'))
);

CREATE INDEX idx_token_jti ON token_metadata (token_jti);
CREATE INDEX idx_token_user ON token_metadata (user_id, revoked);
