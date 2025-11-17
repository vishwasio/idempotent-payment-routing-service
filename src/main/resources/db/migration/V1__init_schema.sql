CREATE TABLE idempotency_keys (
    id BIGSERIAL PRIMARY KEY,
    client_id VARCHAR(100) NOT NULL,
    idempotency_key UUID NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('IN_PROGRESS','COMPLETED','FAILED')),
    response_code INT,
    response_body OID,
    transaction_id BIGINT,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now(),
    UNIQUE (client_id, idempotency_key)
);

CREATE TABLE payment_transactions (
    id BIGSERIAL PRIMARY KEY,
    client_id VARCHAR(100) NOT NULL,
    amount NUMERIC(18,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING','PROCESSING','SUCCESS','FAILED')),
    gateway_transaction_id VARCHAR(100),
    retry_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now()
);

CREATE TABLE outbox (
    id BIGSERIAL PRIMARY KEY,
    aggregate_type VARCHAR(50),
    aggregate_id BIGINT,
    event_type VARCHAR(50),
    payload JSONB NOT NULL,
    processed BOOLEAN DEFAULT FALSE,
    attempts INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT now(),
    processed_at TIMESTAMP
);

CREATE INDEX idx_outbox_unprocessed ON outbox (processed) WHERE processed = false;
