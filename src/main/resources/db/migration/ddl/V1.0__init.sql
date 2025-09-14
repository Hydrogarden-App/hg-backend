CREATE TABLE circuit
(
    id            SMALLINT     NOT NULL,
    name          VARCHAR(255) NULL,
    state         BIT(1)       NULL,
    desired_state BIT(1)       NULL,
    device_id     SMALLINT     NULL,
    CONSTRAINT pk_circuit PRIMARY KEY (id)
);

CREATE TABLE device
(
    id                 SMALLINT     NOT NULL,
    name               VARCHAR(255) NOT NULL,
    keepalive_interval DECIMAL(21,0)       NOT NULL,
    config_interval    DECIMAL(21,0)       NOT NULL,
    new_state_interval DECIMAL(21,0)       NOT NULL,
    CONSTRAINT pk_device PRIMARY KEY (id)
);

CREATE TABLE device_vitals
(
    id                          SMALLINT     NOT NULL,
    state                       VARCHAR(255) NULL,
    desired_state               VARCHAR(255) NOT NULL DEFAULT 'DEAD',
    last_keep_alive_send_time   datetime     NULL,
    last_config_send_time       datetime     NULL,
    last_new_state_send_time    datetime     NULL,
    last_command_receive_time   datetime     NULL,
    standby_timeout             DECIMAL(21,0)       NULL,
    heartbeat_interval          DECIMAL(21,0)       NULL,
    desired_standby_timeout     DECIMAL(21,0)       NOT NULL,
    desired_heartbeat_interval  DECIMAL(21,0)       NOT NULL,
    CONSTRAINT pk_device_vitals PRIMARY KEY (id)
);

ALTER TABLE circuit
    ADD CONSTRAINT FK_CIRCUIT_ON_DEVICE FOREIGN KEY (device_id) REFERENCES device (id);

ALTER TABLE device_vitals
    ADD CONSTRAINT FK_DEVICE_VITALS_ON_ID FOREIGN KEY (id) REFERENCES device (id);