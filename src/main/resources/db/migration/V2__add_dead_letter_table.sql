CREATE TABLE dead_letter_queue (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGSERIAL NOT NULL,
    aggregate_type VARCHAR(255),
    aggregate_id BIGINT,
    event_type VARCHAR(255),
    payload JSONB,
    error_message TEXT,
    attempts INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_attempt_at TIMESTAMP
);
