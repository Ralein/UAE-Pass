# UAE Digital Identity Platform — Operations Runbooks

## 1. JWT Key Rotation

### Scheduled (Proactive)
The `JwkRotationScheduler` runs daily at 02:00 and logs warnings 30 days before key expiry.

### Manual Rotation Steps
```bash
# 1. Generate new key pair in existing keystore
keytool -genkeypair \
  -alias identity-signing-key-$(date +%Y%m%d) \
  -keyalg RSA -keysize 2048 \
  -sigalg SHA256withRSA \
  -storetype PKCS12 \
  -keystore jwt-keystore.p12 \
  -storepass "$JWT_KEYSTORE_PASSWORD" \
  -validity 730 \
  -dname "CN=UAE Identity Server,O=UAE Government,C=AE"

# 2. Update application config to use new alias
export JWT_KEY_ALIAS=identity-signing-key-$(date +%Y%m%d)

# 3. Rolling restart (zero downtime)
# Old key remains in JWK set for verification
# New key used for signing
# Remove old key after token TTL expires (24h for refresh tokens)

# 4. Verify
curl -s http://localhost:8080/oauth2/jwks | jq '.keys | length'
# Should show multiple keys during rotation window
```

### Emergency Rotation (Key Compromise)
1. Generate new keystore entirely
2. Stop application
3. Replace keystore file
4. Update `JWT_KEY_ALIAS` env var
5. Clear Redis sessions: `redis-cli -a $REDIS_PASSWORD FLUSHDB`
6. Restart application
7. All existing tokens are invalidated

---

## 2. Incident Response

### Account Lockout
```bash
# Check lockout status in Redis
redis-cli -a $REDIS_PASSWORD GET "security:lockout:$USER_ID"

# Clear lockout manually
redis-cli -a $REDIS_PASSWORD DEL "security:lockout:$USER_ID"

# Check risk events
psql -U $DB_USER -d uae_identity -c \
  "SELECT * FROM risk_events WHERE user_id='$USER_ID' AND resolved=false ORDER BY created_at DESC LIMIT 10;"
```

### Token Replay Detected
1. Check audit logs: `SELECT * FROM audit_logs WHERE event_type='TOKEN_REPLAYED' ORDER BY created_at DESC;`
2. Identify affected user
3. Revoke all user sessions
4. Force password/PIN reset
5. Notify user via registered contact

### Brute Force Attack
1. Monitor metrics: `identity.lockout.count` counter
2. Check `risk_events` table for `BRUTE_FORCE` events
3. Consider temporary IP block at load balancer level
4. Verify rate limits are operational: `GET /actuator/metrics/identity.auth.failure`

---

## 3. Backup & Restore

### PostgreSQL Backup
```bash
# Daily backup (schedule via cron)
pg_dump -Fc -U $DB_USER -h localhost uae_identity > "backup_$(date +%Y%m%d_%H%M%S).dump"

# Restore
pg_restore -U $DB_USER -h localhost -d uae_identity_restored backup_YYYYMMDD.dump
```

### Redis Backup
Redis uses AOF persistence (configured in docker-compose.yml):
- Backup file: `/data/appendonly.aof`
- RDB snapshots also available: `/data/dump.rdb`

```bash
# Trigger manual RDB save
redis-cli -a $REDIS_PASSWORD BGSAVE

# Copy backup
docker cp uae-redis:/data/dump.rdb ./redis-backup-$(date +%Y%m%d).rdb
```

---

## 4. Audit Log Retention

### Policy
- **Hot storage**: 90 days in PostgreSQL (indexed, queryable)
- **Cold storage**: Export to file/S3 after 90 days
- **Retention**: 7 years minimum (regulatory compliance)

### Export Script
```bash
# Export old audit logs to compressed JSON
psql -U $DB_USER -d uae_identity -c \
  "COPY (SELECT * FROM audit_logs WHERE created_at < NOW() - INTERVAL '90 days')
   TO STDOUT WITH (FORMAT csv, HEADER true)" | gzip > "audit_export_$(date +%Y%m%d).csv.gz"

# Verify HMAC chain integrity before archiving
# TODO: Implement chain verification script

# Delete exported records
psql -U $DB_USER -d uae_identity -c \
  "DELETE FROM audit_logs WHERE created_at < NOW() - INTERVAL '90 days';"
```

---

## 5. Redis Eviction Policy

### Current Configuration
- Max memory: Set in `docker-compose.yml` or Redis config
- Policy: `noeviction` for security data (sessions, lockouts, replay detection)

### Recommendations
```redis
# Verify policy
CONFIG GET maxmemory-policy

# Security data MUST NOT be evictable
# If Redis approaches memory limit:
# 1. Increase maxmemory
# 2. Review TTLs on security keys
# 3. Purge expired device sessions
```

---

## 6. Database Encryption at Rest

### PostgreSQL
- Enable TDE (Transparent Data Encryption) at the storage level
- For cloud deployments: Use managed database encryption (AWS RDS, Azure Database)
- For Docker: Mount encrypted volume

### Application-Level Encryption
Already implemented:
- Full name: AES-256-GCM encrypted (`full_name_enc` column)
- Identifiers: SHA-256 hashed with application salt
- PINs: Argon2id hashed with server pepper
- OTPs: SHA-256 hashed (ephemeral, no salt needed)

### Key Management
- AES key: `$AES_ENCRYPTION_KEY` env var → SecretsProvider
- Server pepper: `$SERVER_PEPPER` env var → SecretsProvider
- For production: Migrate to Vault/KMS via `SecretsProvider` interface
