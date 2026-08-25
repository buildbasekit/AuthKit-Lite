CREATE TABLE webauthn_user_entity (
    id BINARY(32) PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE webauthn_credentials (
    id BINARY(32) PRIMARY KEY,
    user_entity_id BINARY(32) NOT NULL,
    public_key BLOB NOT NULL,
    signature_count BIGINT NOT NULL,
    uv_initialized BOOLEAN NOT NULL,
    transports VARCHAR(255),
    backup_eligible BOOLEAN NOT NULL,
    backup_state BOOLEAN NOT NULL,
    attestation_object BLOB,
    client_data_json BLOB,
    label VARCHAR(255),
    created TIMESTAMP NOT NULL,
    last_used TIMESTAMP,
    FOREIGN KEY (user_entity_id) REFERENCES webauthn_user_entity(id)
);
