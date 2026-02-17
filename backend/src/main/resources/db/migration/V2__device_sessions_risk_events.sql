-- ============================================
-- UAE Digital Identity Platform - Schema V2
-- Device sessions + Risk events
-- ============================================

-- ============================================
-- DEVICE SESSIONS
-- Track known devices per user for trust scoring
-- ============================================
CREATE TABLE device_sessions (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_fingerprint_hash VARCHAR(128) NOT NULL,
    user_agent              TEXT,
    ip_address              VARCHAR(45),
    last_seen_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    first_seen_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    trust_level             VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
    is_active               BOOLEAN NOT NULL DEFAULT TRUE,
    login_count             INT NOT NULL DEFAULT 0,
    revoked_at              TIMESTAMPTZ,

    CONSTRAINT chk_trust_level CHECK (trust_level IN ('UNKNOWN', 'LOW', 'MEDIUM', 'HIGH', 'BLOCKED'))
);

CREATE INDEX idx_device_user_id ON device_sessions (user_id, is_active);
CREATE INDEX idx_device_fingerprint ON device_sessions (device_fingerprint_hash);
CREATE INDEX idx_device_last_seen ON device_sessions (last_seen_at DESC);
CREATE UNIQUE INDEX idx_device_user_fingerprint ON device_sessions (user_id, device_fingerprint_hash);

-- ============================================
-- RISK EVENTS
-- Security risk scoring and anomaly tracking
-- ============================================
CREATE TABLE risk_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID REFERENCES users(id) ON DELETE SET NULL,
    event_type      VARCHAR(50) NOT NULL,
    risk_score      INT NOT NULL DEFAULT 0,
    source_ip       VARCHAR(45),
    device_fingerprint_hash VARCHAR(128),
    metadata        JSONB,
    resolved        BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_at     TIMESTAMPTZ,
    resolved_by     VARCHAR(100),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_risk_score CHECK (risk_score >= 0 AND risk_score <= 100),
    CONSTRAINT chk_risk_event_type CHECK (event_type IN (
        'BRUTE_FORCE', 'NEW_DEVICE', 'UNUSUAL_IP', 'VELOCITY_ANOMALY',
        'SESSION_HIJACK', 'TOKEN_REPLAY', 'OTP_ABUSE', 'PIN_LOCKOUT',
        'ACCOUNT_TAKEOVER_ATTEMPT', 'GEO_ANOMALY'
    ))
);

CREATE INDEX idx_risk_user_id ON risk_events (user_id);
CREATE INDEX idx_risk_event_type ON risk_events (event_type);
CREATE INDEX idx_risk_created_at ON risk_events (created_at DESC);
CREATE INDEX idx_risk_unresolved ON risk_events (resolved, created_at DESC) WHERE resolved = FALSE;
