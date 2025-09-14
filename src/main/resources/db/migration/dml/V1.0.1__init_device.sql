INSERT INTO device (id, name, keepalive_interval, config_interval, new_state_interval)
VALUES (1, 'Device #1', 20000, 5000, 5000);

INSERT INTO device_vitals (id, state, desired_state, standby_timeout, heartbeat_interval,
                           desired_standby_timeout, desired_heartbeat_interval)
VALUES (1, 'ALIVE', 'DEAD', 80000, 20000, 60000, 30000);

INSERT INTO circuit (id, name, state, desired_state, device_id) VALUES
                                                                    (1, 'Circuit 1', b'0', b'0', 1),
                                                                    (2, 'Circuit 2', b'0', b'0', 1),
                                                                    (3, 'Circuit 3', b'0', b'0', 1),
                                                                    (4, 'Circuit 4', b'0', b'0', 1),
                                                                    (5, 'Circuit 5', b'0', b'0', 1),
                                                                    (6, 'Circuit 6', b'0', b'0', 1),
                                                                    (7, 'Circuit 7', b'0', b'0', 1),
                                                                    (8, 'Circuit 8', b'0', b'0', 1);
