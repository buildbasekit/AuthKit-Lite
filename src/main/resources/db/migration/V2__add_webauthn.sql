CREATE TABLE user_entities (
    id VARCHAR(1000) CHARACTER SET ascii NOT NULL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(200)
);

CREATE TABLE user_credentials (
    credential_id VARCHAR(1000) CHARACTER SET ascii NOT NULL PRIMARY KEY,
    user_entity_user_id VARCHAR(1000) CHARACTER SET ascii NOT NULL,
    public_key BLOB NOT NULL,
    signature_count BIGINT,
    uv_initialized BOOLEAN,
    backup_eligible BOOLEAN NOT NULL,
    authenticator_transports VARCHAR(1000),
    public_key_credential_type VARCHAR(100),
    backup_state BOOLEAN NOT NULL,
    attestation_object BLOB,
    attestation_client_data_json BLOB,
    created TIMESTAMP,
    last_used TIMESTAMP,
    label VARCHAR(1000) NOT NULL,
    CONSTRAINT fk_user_credentials_entity
        FOREIGN KEY (user_entity_user_id) REFERENCES user_entities (id)
);
