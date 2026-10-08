CREATE TABLE app_users (
 id VARCHAR(36) PRIMARY KEY, email VARCHAR(254) NOT NULL UNIQUE,
 name VARCHAR(80) NOT NULL, password_hash VARCHAR(100) NOT NULL,
 created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE wallets (
 id VARCHAR(36) PRIMARY KEY, owner_id VARCHAR(36) UNIQUE REFERENCES app_users(id),
 label VARCHAR(100) NOT NULL, balance_cents BIGINT NOT NULL CHECK(balance_cents >= 0),
 currency VARCHAR(3) NOT NULL DEFAULT 'INR' CHECK(currency = 'INR')
);
CREATE TABLE transfers (
 id VARCHAR(36) PRIMARY KEY, source_id VARCHAR(36) NOT NULL REFERENCES wallets(id),
 target_id VARCHAR(36) NOT NULL REFERENCES wallets(id),
 amount_cents BIGINT NOT NULL CHECK(amount_cents > 0),
 note VARCHAR(200) NOT NULL, category VARCHAR(30) NOT NULL,
 idempotency_key VARCHAR(100) NOT NULL,
 created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(source_id, idempotency_key), CHECK(source_id <> target_id)
);
CREATE TABLE ledger_entries (
 id VARCHAR(36) PRIMARY KEY, transfer_id VARCHAR(36) NOT NULL REFERENCES transfers(id),
 wallet_id VARCHAR(36) NOT NULL REFERENCES wallets(id), delta_cents BIGINT NOT NULL CHECK(delta_cents <> 0),
 UNIQUE(transfer_id,wallet_id)
);
CREATE INDEX transfers_source_time ON transfers(source_id,created_at);
CREATE INDEX transfers_target_time ON transfers(target_id,created_at);
CREATE INDEX ledger_wallet ON ledger_entries(wallet_id);
CREATE TABLE audit_events (
 id VARCHAR(36) PRIMARY KEY, actor_id VARCHAR(36), event_type VARCHAR(40) NOT NULL,
 reference_id VARCHAR(36), created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO wallets(id,owner_id,label,balance_cents) VALUES('00000000-0000-0000-0000-000000000000',NULL,'Simulation treasury',1000000000000);
