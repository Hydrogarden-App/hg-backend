package com.hydrogarden.business.device.core;

import com.hydrogarden.business.device.core.entity.*;
import org.junit.jupiter.params.provider.Arguments;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Test cases representing the 4 valid device states.
 * Used for parameterized state-based testing.
 */
public class DeviceTestCases {

    public static final Duration DEFAULT_STANDBY_TIMEOUT = Duration.ofSeconds(30);
    public static final Duration DEFAULT_HEARTBEAT_INTERVAL = Duration.ofSeconds(10);
    public static final Duration DEFAULT_KEEPALIVE_INTERVAL = Duration.ofSeconds(15);
    public static final Duration DEFAULT_CONFIG_INTERVAL = Duration.ofSeconds(20);
    public static final Duration DEFAULT_NEW_STATE_INTERVAL = Duration.ofSeconds(5);

    public static final Duration ALTERNATIVE_STANDBY_TIMEOUT = Duration.ofSeconds(45);
    public static final Duration ALTERNATIVE_HEARTBEAT_INTERVAL = Duration.ofSeconds(15);

    public static final DeviceId TEST_DEVICE_ID = new DeviceId((short) 1);
    public static final LocalDateTime BASE_TIME = LocalDateTime.of(2025, 1, 1, 12, 0, 0);

    /**
     * State 1: DEAD/DEAD
     * Device is dead and expected to be dead (timed out)
     */
    public static Device createState1_DeadDead() {
        DeviceConfig config = createDefaultConfig();
        DeviceVitals vitals = createVitals(
                DeviceState.DEAD,
                DeviceState.DEAD,
                null,
                BASE_TIME.minus(Duration.ofSeconds(60)),
                config,
                config,
                null,
                null
        );
        return createDevice(vitals, null);
    }

    /**
     * State 2: DEAD/ALIVE
     * Device is dead but expected to be alive (never sent ACK)
     */
    public static Device createState2_DeadAlive() {
        DeviceConfig config = createDefaultConfig();
        DeviceVitals vitals = createVitals(
                DeviceState.DEAD,
                DeviceState.ALIVE,
                null,
                null,
                config,
                config,
                null,
                null
        );
        return createDevice(vitals, null);
    }

    /**
     * State 3: ALIVE/ALIVE
     * Device is alive and expected to be alive (healthy operation)
     */
    public static Device createState3_AliveAlive() {
        DeviceConfig config = createDefaultConfig();
        DeviceVitals vitals = createVitals(
                DeviceState.ALIVE,
                DeviceState.ALIVE,
                BASE_TIME.minus(Duration.ofSeconds(10)),
                BASE_TIME.minus(Duration.ofSeconds(5)),
                config,
                config,
                BASE_TIME.minus(Duration.ofSeconds(15)),
                BASE_TIME.minus(Duration.ofSeconds(3))
        );
        return createDevice(vitals, null);
    }

    /**
     * State 3 Variant: ALIVE/ALIVE with unsynchronized circuits
     */
    public static Device createState3_AliveAlive_UnsynchronizedCircuits() {
        DeviceConfig config = createDefaultConfig();
        DeviceVitals vitals = createVitals(
                DeviceState.ALIVE,
                DeviceState.ALIVE,
                BASE_TIME.minus(Duration.ofSeconds(10)),
                BASE_TIME.minus(Duration.ofSeconds(5)),
                config,
                config,
                BASE_TIME.minus(Duration.ofSeconds(15)),
                BASE_TIME.minus(Duration.ofSeconds(3))
        );

        List<Circuit> circuits = createDefaultCircuits();
        circuits.set(0, new Circuit(
                new CircuitId((short) 1),
                new CircuitState(false),
                "Circuit 1",
                new CircuitState(true),
                null
        ));

        return createDevice(vitals, circuits);
    }

    /**
     * State 1 Variant: DEAD/DEAD with different desired config
     */
    public static Device createState1_DeadDead_ConfigDiffers() {
        DeviceVitals vitals = createVitals(
                DeviceState.DEAD,
                DeviceState.DEAD,
                null,
                BASE_TIME.minus(Duration.ofSeconds(60)),
                createDefaultConfig(),
                createAlternativeConfig(),
                null,
                null
        );
        return createDevice(vitals, null);
    }

    /**
     * State 2 Variant: DEAD/ALIVE with different desired config
     */
    public static Device createState2_DeadAlive_ConfigDiffers() {
        DeviceVitals vitals = createVitals(
                DeviceState.DEAD,
                DeviceState.ALIVE,
                null,
                null,
                createDefaultConfig(),
                createAlternativeConfig(),
                null,
                null
        );
        return createDevice(vitals, null);
    }

    /**
     * State 3 Variant: ALIVE/ALIVE with different desired config
     */
    public static Device createState3_AliveAlive_ConfigDiffers() {
        DeviceVitals vitals = createVitals(
                DeviceState.ALIVE,
                DeviceState.ALIVE,
                BASE_TIME.minus(Duration.ofSeconds(10)),
                BASE_TIME.minus(Duration.ofSeconds(5)),
                createDefaultConfig(),
                createAlternativeConfig(),
                BASE_TIME.minus(Duration.ofSeconds(10)),
                BASE_TIME.minus(Duration.ofSeconds(3))
        );
        return createDevice(vitals, null);
    }

    /**
     * State 4 Variant: ALIVE/DEAD with different desired config
     */
    public static Device createState4_AliveDead_ConfigDiffers() {
        DeviceVitals vitals = createVitals(
                DeviceState.ALIVE,
                DeviceState.DEAD,
                null,
                BASE_TIME.minus(Duration.ofSeconds(5)),
                createDefaultConfig(),
                createAlternativeConfig(),
                null,
                null
        );
        return createDevice(vitals, null);
    }

    /**
     * State 3 Variant: ALIVE/ALIVE needing keepalive
     */
    public static Device createState3_AliveAlive_NeedsKeepalive() {
        DeviceConfig config = createDefaultConfig();
        DeviceVitals vitals = createVitals(
                DeviceState.ALIVE,
                DeviceState.ALIVE,
                BASE_TIME.minus(Duration.ofSeconds(20)),
                BASE_TIME.minus(Duration.ofSeconds(5)),
                config,
                config,
                BASE_TIME.minus(Duration.ofSeconds(15)),
                BASE_TIME.minus(Duration.ofSeconds(3))
        );
        return createDevice(vitals, null);
    }

    /**
     * State 4: ALIVE/DEAD
     * Device is alive but expected to be dead (being disabled)
     */
    public static Device createState4_AliveDead() {
        DeviceConfig config = createDefaultConfig();
        DeviceVitals vitals = createVitals(
                DeviceState.ALIVE,
                DeviceState.DEAD,
                null,
                BASE_TIME.minus(Duration.ofSeconds(5)),
                config,
                config,
                null,
                null
        );
        return createDevice(vitals, null);
    }

    /**
     * Creates a device with specified vitals and optional custom circuits.
     * Handles bidirectional relationship setup automatically.
     */
    private static Device createDevice(DeviceVitals vitals, List<Circuit> circuits) {
        if (circuits == null) {
            circuits = createDefaultCircuits();
        }

        Device device = new Device(
                TEST_DEVICE_ID,
                "Test Device",
                circuits,
                DEFAULT_KEEPALIVE_INTERVAL,
                DEFAULT_CONFIG_INTERVAL,
                DEFAULT_NEW_STATE_INTERVAL,
                vitals,
                Collections.emptySet()
        );

        vitals.setDevice(device);
        setCircuitDeviceUsingReflection(circuits, device);

        return device;
    }

    /**
     * Creates DeviceVitals with the specified parameters.
     */
    private static DeviceVitals createVitals(
            DeviceState state,
            DeviceState desiredState,
            LocalDateTime lastKeepAliveSendTime,
            LocalDateTime lastCommandReceiveTime,
            DeviceConfig currentConfig,
            DeviceConfig desiredConfig,
            LocalDateTime lastConfigSendTime,
            LocalDateTime lastNewStateSendTime
    ) {
        return new DeviceVitals(
                TEST_DEVICE_ID,
                state,
                desiredState,
                lastKeepAliveSendTime,
                lastCommandReceiveTime,
                currentConfig,
                desiredConfig,
                lastConfigSendTime,
                lastNewStateSendTime,
                null
        );
    }

    /**
     * Creates default synchronized config (current == desired).
     */
    private static DeviceConfig createDefaultConfig() {
        return new DeviceConfig(DEFAULT_STANDBY_TIMEOUT, DEFAULT_HEARTBEAT_INTERVAL);
    }

    /**
     * Creates alternative config with different timeout values.
     */
    private static DeviceConfig createAlternativeConfig() {
        return new DeviceConfig(ALTERNATIVE_STANDBY_TIMEOUT, ALTERNATIVE_HEARTBEAT_INTERVAL);
    }

    /**
     * Creates default circuits for testing (all OFF and synchronized).
     */
    private static List<Circuit> createDefaultCircuits() {
        List<Circuit> circuits = new ArrayList<>();
        for (short i = 1; i <= 8; i++) {
            circuits.add(new Circuit(
                    new CircuitId(i),
                    new CircuitState(false),
                    "Circuit " + i,
                    new CircuitState(false),
                    null
            ));
        }
        return circuits;
    }

    /**
     * Sets the device field on circuits using reflection to avoid circular dependency issues.
     */
    private static void setCircuitDeviceUsingReflection(List<Circuit> circuits, Device device) {
        try {
            Field deviceField = Circuit.class.getDeclaredField("device");
            deviceField.setAccessible(true);
            for (Circuit circuit : circuits) {
                deviceField.set(circuit, device);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set device field on circuits", e);
        }
    }

    /**
     * Provides all test states for parameterized tests
     */
    public static Stream<Arguments> allDeviceStates() {
        return Stream.of(
                Arguments.of("State1_DEAD_DEAD", createState1_DeadDead()),
                Arguments.of("State2_DEAD_ALIVE", createState2_DeadAlive()),
                Arguments.of("State3_ALIVE_ALIVE", createState3_AliveAlive()),
                Arguments.of("State4_ALIVE_DEAD", createState4_AliveDead())
        );
    }
}
