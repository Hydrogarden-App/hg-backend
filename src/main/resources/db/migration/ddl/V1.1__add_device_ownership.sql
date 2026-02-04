CREATE TABLE device_ownership
(
    owner_id  VARCHAR(255) NOT NULL,
    device_id SMALLINT     NOT NULL,
    CONSTRAINT pk_device_ownership PRIMARY KEY (owner_id, device_id)
);

ALTER TABLE device_ownership
    ADD CONSTRAINT FK_DEVICE_OWNERSHIP_ON_DEVICE FOREIGN KEY (device_id) REFERENCES device (id);

CREATE INDEX idx_device_ownership_device_id ON device_ownership (device_id);
