# Device Domain Model - State-Based Test Plan

## Overview
This test plan uses **state-based testing** to verify all Device entity methods across all valid device states. Each test validates behavior and domain event emission for every method in each state.

## Test State Definitions

Based on `states.md`, the Device entity has **4 valid states** defined by the combination of `state` (actual) and `desiredState` (expected):

### State 1: DEAD/DEAD
- **Actual state:** DEAD
- **Desired state:** DEAD
- **Reason:** `lastCommandReceiveTime` exceeded `standbyTimeout`
- **Description:** Device has timed out and is expected to remain dead

### State 2: DEAD/ALIVE
- **Actual state:** DEAD
- **Desired state:** ALIVE
- **Reason:** Device never sent any ACK back
- **Description:** Device should be alive but hasn't responded yet

### State 3: ALIVE/ALIVE
- **Actual state:** ALIVE
- **Desired state:** ALIVE
- **Reason:** Device confirmed config or other commands within timeout
- **Description:** Device is healthy and operational

### State 4: ALIVE/DEAD
- **Actual state:** ALIVE
- **Desired state:** DEAD
- **Description:** Device is alive but system wants to disable it

---

## Test Structure

```
src/test/java/com/hydrogarden/business/device/core/
├── DeviceTestCases.java          # State setup class
├── DeviceStateBasedTest.java     # Parameterized tests for all methods
└── DeviceDomainEventTest.java    # Event verification tests
```

---

## 1. DeviceTestCases Class

**File:** `DeviceTestCases.java`

This class provides factory methods to create Device instances in each of the 4 valid states.

```java
package com.hydrogarden.business.device.core;

import com.hydrogarden.business.device.core.entity.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

/**
 * Test cases representing the 4 valid device states.
 * Used for parameterized state-based testing.
 */
public class DeviceTestCases {

    // Common test configuration
    public static final Duration DEFAULT_STANDBY_TIMEOUT = Duration.ofSeconds(30);
    public static final Duration DEFAULT_HEARTBEAT_INTERVAL = Duration.ofSeconds(10);
    public static final Duration DEFAULT_KEEPALIVE_INTERVAL = Duration.ofSeconds(15);
    public static final Duration DEFAULT_CONFIG_INTERVAL = Duration.ofSeconds(20);
    public static final Duration DEFAULT_NEW_STATE_INTERVAL = Duration.ofSeconds(5);

    public static final DeviceId TEST_DEVICE_ID = new DeviceId((short) 1);
    public static final LocalDateTime BASE_TIME = LocalDateTime.of(2025, 1, 1, 12, 0, 0);

    /**
     * State 1: DEAD/DEAD
     * Device is dead and expected to be dead (timed out)
     */
    public static Device createState1_DeadDead() {
        Device device = createBaseDevice();
        DeviceVitals vitals = device.getDeviceVitals();

        // Set both states to DEAD
        vitals.setState(DeviceState.DEAD);
        vitals.setDesiredState(DeviceState.DEAD);

        // Last command was beyond standby timeout (60 seconds ago)
        vitals.setLastCommandReceiveTime(BASE_TIME.minus(Duration.ofSeconds(60)));

        // Config is synchronized
        DeviceConfig config = new DeviceConfig(DEFAULT_STANDBY_TIMEOUT, DEFAULT_HEARTBEAT_INTERVAL);
        vitals.setDeviceConfig(config);
        vitals.setDesiredDeviceConfig(config);

        return device;
    }

    /**
     * State 2: DEAD/ALIVE
     * Device is dead but expected to be alive (never sent ACK)
     */
    public static Device createState2_DeadAlive() {
        Device device = createBaseDevice();
        DeviceVitals vitals = device.getDeviceVitals();

        // Actual state is DEAD, desired state is ALIVE
        vitals.setState(DeviceState.DEAD);
        vitals.setDesiredState(DeviceState.ALIVE);

        // Last command was beyond timeout OR never received (null)
        vitals.setLastCommandReceiveTime(BASE_TIME.minus(Duration.ofSeconds(60)));

        // Config may differ (desired wants device alive)
        DeviceConfig currentConfig = new DeviceConfig(DEFAULT_STANDBY_TIMEOUT, DEFAULT_HEARTBEAT_INTERVAL);
        vitals.setDeviceConfig(currentConfig);
        vitals.setDesiredDeviceConfig(currentConfig);

        return device;
    }

    /**
     * State 3: ALIVE/ALIVE
     * Device is alive and expected to be alive (healthy operation)
     */
    public static Device createState3_AliveAlive() {
        Device device = createBaseDevice();
        DeviceVitals vitals = device.getDeviceVitals();

        // Both states are ALIVE
        vitals.setState(DeviceState.ALIVE);
        vitals.setDesiredState(DeviceState.ALIVE);

        // Last command was recent (within timeout)
        vitals.setLastCommandReceiveTime(BASE_TIME.minus(Duration.ofSeconds(5)));

        // Config is synchronized
        DeviceConfig config = new DeviceConfig(DEFAULT_STANDBY_TIMEOUT, DEFAULT_HEARTBEAT_INTERVAL);
        vitals.setDeviceConfig(config);
        vitals.setDesiredDeviceConfig(config);

        // Set recent keepalive and config send times
        vitals.setLastKeepAliveSendTime(BASE_TIME.minus(Duration.ofSeconds(10)));
        vitals.setLastConfigSendTime(BASE_TIME.minus(Duration.ofSeconds(15)));
        vitals.setLastNewStateSendTime(BASE_TIME.minus(Duration.ofSeconds(3)));

        return device;
    }

    /**
     * State 3 Variant: ALIVE/ALIVE with unsynchronized circuits
     */
    public static Device createState3_AliveAlive_UnsynchronizedCircuits() {
        Device device = createState3_AliveAlive();

        // Make circuits unsynchronized (desired != actual)
        List<Circuit> circuits = device.getCircuits();
        circuits.get(0).setDesiredState(new CircuitState(true));  // Want ON
        circuits.get(0).setState(new CircuitState(false));         // Actually OFF

        return device;
    }

    /**
     * State 3 Variant: ALIVE/ALIVE with different desired config
     */
    public static Device createState3_AliveAlive_ConfigDiffers() {
        Device device = createState3_AliveAlive();
        DeviceVitals vitals = device.getDeviceVitals();

        // Current config
        DeviceConfig currentConfig = new DeviceConfig(DEFAULT_STANDBY_TIMEOUT, DEFAULT_HEARTBEAT_INTERVAL);
        vitals.setDeviceConfig(currentConfig);

        // Different desired config
        DeviceConfig desiredConfig = new DeviceConfig(Duration.ofSeconds(45), Duration.ofSeconds(15));
        vitals.setDesiredDeviceConfig(desiredConfig);

        // Clear last config send time to allow immediate send
        vitals.setLastConfigSendTime(null);

        return device;
    }

    /**
     * State 3 Variant: ALIVE/ALIVE needing keepalive
     */
    public static Device createState3_AliveAlive_NeedsKeepalive() {
        Device device = createState3_AliveAlive();
        DeviceVitals vitals = device.getDeviceVitals();

        // Last keepalive was sent beyond keepalive interval
        vitals.setLastKeepAliveSendTime(BASE_TIME.minus(Duration.ofSeconds(20)));

        return device;
    }

    /**
     * State 4: ALIVE/DEAD
     * Device is alive but expected to be dead (being disabled)
     */
    public static Device createState4_AliveDead() {
        Device device = createBaseDevice();
        DeviceVitals vitals = device.getDeviceVitals();

        // Actual state is ALIVE, desired state is DEAD
        vitals.setState(DeviceState.ALIVE);
        vitals.setDesiredState(DeviceState.DEAD);

        // Last command was recent (device is still alive)
        vitals.setLastCommandReceiveTime(BASE_TIME.minus(Duration.ofSeconds(5)));

        // Config is synchronized
        DeviceConfig config = new DeviceConfig(DEFAULT_STANDBY_TIMEOUT, DEFAULT_HEARTBEAT_INTERVAL);
        vitals.setDeviceConfig(config);
        vitals.setDesiredDeviceConfig(config);

        return device;
    }

    /**
     * Create a base device with default setup
     */
    private static Device createBaseDevice() {
        Device device = new Device();
        device.setId(TEST_DEVICE_ID);
        device.setName("Test Device");
        device.setKeepaliveInterval(DEFAULT_KEEPALIVE_INTERVAL);
        device.setConfigInterval(DEFAULT_CONFIG_INTERVAL);
        device.setNewStateInterval(DEFAULT_NEW_STATE_INTERVAL);

        // Create device vitals
        DeviceVitals vitals = new DeviceVitals();
        vitals.setId(TEST_DEVICE_ID);
        device.setDeviceVitals(vitals);

        // Create 8 circuits (common device configuration)
        List<Circuit> circuits = createDefaultCircuits(device);
        device.setCircuits(circuits);

        return device;
    }

    /**
     * Create default circuits for testing
     */
    private static List<Circuit> createDefaultCircuits(Device device) {
        return List.of(
            createCircuit((short) 1, "Circuit 1", false, false, device),
            createCircuit((short) 2, "Circuit 2", false, false, device),
            createCircuit((short) 3, "Circuit 3", false, false, device),
            createCircuit((short) 4, "Circuit 4", false, false, device),
            createCircuit((short) 5, "Circuit 5", false, false, device),
            createCircuit((short) 6, "Circuit 6", false, false, device),
            createCircuit((short) 7, "Circuit 7", false, false, device),
            createCircuit((short) 8, "Circuit 8", false, false, device)
        );
    }

    private static Circuit createCircuit(short id, String name, boolean state, boolean desiredState, Device device) {
        Circuit circuit = new Circuit();
        circuit.setId(new CircuitId(id, TEST_DEVICE_ID));
        circuit.setName(name);
        circuit.setState(new CircuitState(state));
        circuit.setDesiredState(new CircuitState(desiredState));
        circuit.setDevice(device);
        return circuit;
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
```

---

## 2. Device State-Based Tests

**File:** `DeviceStateBasedTest.java`

### Test Matrix Overview

Each Device method is tested in all 4 states. The matrix below shows which tests are expected to **succeed** (✅), **fail/throw** (❌), or have **state-specific behavior** (⚠️).

| Method | State 1<br/>DEAD/DEAD | State 2<br/>DEAD/ALIVE | State 3<br/>ALIVE/ALIVE | State 4<br/>ALIVE/DEAD |
|--------|:---:|:---:|:---:|:---:|
| **evaluateCurrentStateAndCommand()** | ✅ | ✅ | ✅ | ✅ |
| **requestChangeCircuitState()** | ❌ | ❌ | ✅ | ✅ |
| **enable()** | ✅ | ✅ | ✅ | ✅ |
| **disable()** | ✅ | ✅ | ✅ | ✅ |
| **rename()** | ✅ | ✅ | ✅ | ✅ |
| **getCircuits()** | ✅ | ✅ | ✅ | ✅ |
| **handleDeviceCommand(Heartbeat)** | ⚠️ | ⚠️ | ⚠️ | ⚠️ |
| **handleDeviceCommand(AckState)** | ⚠️ | ⚠️ | ⚠️ | ⚠️ |
| **handleDeviceCommand(AckConfig)** | ⚠️ | ⚠️ | ⚠️ | ⚠️ |
| **handleDeviceCommand(RequestConfig)** | ⚠️ | ⚠️ | ⚠️ | ⚠️ |

---

### 2.1 evaluateCurrentStateAndCommand() Tests

#### Test: evaluateCurrentStateAndCommand_State1_DeadDead
- **Given:** Device in State 1 (DEAD/DEAD)
- **When:** evaluateCurrentStateAndCommand() is called
- **Then:**
  - Device remains DEAD
  - No commands are returned (empty list)
  - No domain events are raised
  - State does not transition

#### Test: evaluateCurrentStateAndCommand_State2_DeadAlive
- **Given:** Device in State 2 (DEAD/ALIVE)
- **When:** evaluateCurrentStateAndCommand() is called
- **Then:**
  - Device remains DEAD (waiting for heartbeat)
  - No commands are returned (cannot send commands to dead device)
  - No domain events are raised
  - Device waits for incoming command to transition to ALIVE

#### Test: evaluateCurrentStateAndCommand_State3_AliveAlive
- **Given:** Device in State 3 (ALIVE/ALIVE) with all synchronized
- **When:** evaluateCurrentStateAndCommand() is called
- **Then:**
  - Device remains ALIVE
  - Empty command list returned (nothing to send)
  - No domain events are raised

#### Test: evaluateCurrentStateAndCommand_State3_AliveAlive_NeedsKeepalive
- **Given:** Device in State 3 with keepalive interval elapsed
- **When:** evaluateCurrentStateAndCommand() is called
- **Then:**
  - KeepAliveCommand is returned
  - `KeepaliveSentDE` event is raised
  - lastKeepAliveSendTime is updated

#### Test: evaluateCurrentStateAndCommand_State3_AliveAlive_ConfigDiffers
- **Given:** Device in State 3 with config != desiredConfig
- **When:** evaluateCurrentStateAndCommand() is called
- **Then:**
  - ConfigCommand is returned (first priority)
  - No other commands are returned
  - lastConfigSendTime is NOT updated (command not yet sent)

#### Test: evaluateCurrentStateAndCommand_State3_AliveAlive_UnsynchronizedCircuits
- **Given:** Device in State 3 with unsynchronized circuits
- **When:** evaluateCurrentStateAndCommand() is called
- **Then:**
  - NewStateCommand is returned
  - Command contains all circuit desired states
  - No event raised yet (event raised when ACK received)

#### Test: evaluateCurrentStateAndCommand_State3_AliveAlive_MultipleCommands
- **Given:** Device in State 3 needing keepalive AND new state
- **When:** evaluateCurrentStateAndCommand() is called
- **Then:**
  - Both KeepAliveCommand and NewStateCommand are returned
  - Commands are in correct order: [KeepAlive, NewState]
  - `KeepaliveSentDE` event is raised

#### Test: evaluateCurrentStateAndCommand_State4_AliveDead
- **Given:** Device in State 4 (ALIVE/DEAD)
- **When:** evaluateCurrentStateAndCommand() is called
- **Then:**
  - Device remains ALIVE (still receiving heartbeats)
  - No commands are generated (desiredState is DEAD, so no keepalive/config)
  - No domain events raised
  - Device will transition to DEAD when timeout occurs

---

### 2.2 requestChangeCircuitState() Tests

#### Test: requestChangeCircuitState_State1_DeadDead_ThrowsException
- **Given:** Device in State 1 (DEAD/DEAD)
- **When:** requestChangeCircuitState(circuitId, ON) is called
- **Then:**
  - IllegalStateException is thrown: "Cannot change circuit state of a dead device"
  - Circuit state remains unchanged
  - No command is generated

#### Test: requestChangeCircuitState_State2_DeadAlive_ThrowsException
- **Given:** Device in State 2 (DEAD/ALIVE)
- **When:** requestChangeCircuitState(circuitId, ON) is called
- **Then:**
  - IllegalStateException is thrown
  - Circuit state remains unchanged

#### Test: requestChangeCircuitState_State3_AliveAlive_Success
- **Given:** Device in State 3 (ALIVE/ALIVE)
- **When:** requestChangeCircuitState(circuitId(1), ON) is called
- **Then:**
  - Circuit 1's desiredState is set to ON
  - NewStateCommand is returned with all circuits' desired states
  - No domain event raised yet (raised when ACK received)

#### Test: requestChangeCircuitState_State3_AliveAlive_InvalidCircuitId
- **Given:** Device in State 3 (ALIVE/ALIVE)
- **When:** requestChangeCircuitState(invalidCircuitId(99), ON) is called
- **Then:**
  - IllegalArgumentException is thrown: "Circuit 99 not found"
  - No state changes occur

#### Test: requestChangeCircuitState_State4_AliveDead_Success
- **Given:** Device in State 4 (ALIVE/DEAD)
- **When:** requestChangeCircuitState(circuitId(1), ON) is called
- **Then:**
  - Circuit 1's desiredState is set to ON
  - NewStateCommand is returned
  - Device is still ALIVE so operation succeeds

---

### 2.3 enable() Tests

#### Test: enable_State1_DeadDead
- **Given:** Device in State 1 (DEAD/DEAD)
- **When:** enable() is called
- **Then:**
  - desiredState is set to ALIVE
  - StartCommand is returned
  - Actual state remains DEAD (will transition when heartbeat received)

#### Test: enable_State2_DeadAlive
- **Given:** Device in State 2 (DEAD/ALIVE) - already wants to be alive
- **When:** enable() is called
- **Then:**
  - desiredState remains ALIVE
  - StartCommand is returned
  - No state change (already desired to be ALIVE)

#### Test: enable_State3_AliveAlive
- **Given:** Device in State 3 (ALIVE/ALIVE)
- **When:** enable() is called
- **Then:**
  - desiredState remains ALIVE (idempotent)
  - StartCommand is returned

#### Test: enable_State4_AliveDead
- **Given:** Device in State 4 (ALIVE/DEAD)
- **When:** enable() is called
- **Then:**
  - desiredState changes from DEAD to ALIVE
  - StartCommand is returned
  - Device now wants to stay alive

---

### 2.4 disable() Tests

#### Test: disable_State1_DeadDead
- **Given:** Device in State 1 (DEAD/DEAD)
- **When:** disable() is called
- **Then:**
  - desiredState remains DEAD (idempotent)
  - NewStateCommand is returned with all circuits OFF
  - All circuits' desiredState set to OFF

#### Test: disable_State2_DeadAlive
- **Given:** Device in State 2 (DEAD/ALIVE)
- **When:** disable() is called
- **Then:**
  - desiredState changes from ALIVE to DEAD
  - NewStateCommand is returned with all circuits OFF
  - Device will remain DEAD when it eventually connects

#### Test: disable_State3_AliveAlive
- **Given:** Device in State 3 (ALIVE/ALIVE)
- **When:** disable() is called
- **Then:**
  - desiredState changes from ALIVE to DEAD
  - NewStateCommand is returned with all circuits OFF
  - All 8 circuits' desiredState set to OFF

#### Test: disable_State4_AliveDead
- **Given:** Device in State 4 (ALIVE/DEAD)
- **When:** disable() is called
- **Then:**
  - desiredState remains DEAD (idempotent)
  - NewStateCommand is returned with all circuits OFF

---

### 2.5 rename() Tests

#### Test: rename_State1_DeadDead
- **Given:** Device in State 1 (DEAD/DEAD) named "Old Name"
- **When:** rename("New Name") is called
- **Then:**
  - Device name is updated to "New Name"
  - State remains unchanged
  - No domain events raised

#### Test: rename_State2_DeadAlive
- **Given:** Device in State 2 (DEAD/ALIVE)
- **When:** rename("New Name") is called
- **Then:**
  - Device name is updated to "New Name"

#### Test: rename_State3_AliveAlive
- **Given:** Device in State 3 (ALIVE/ALIVE)
- **When:** rename("New Name") is called
- **Then:**
  - Device name is updated to "New Name"

#### Test: rename_State4_AliveDead
- **Given:** Device in State 4 (ALIVE/DEAD)
- **When:** rename("New Name") is called
- **Then:**
  - Device name is updated to "New Name"

#### Test: rename_AllStates_NullNameThrowsException
- **Given:** Device in any state
- **When:** rename(null) is called
- **Then:**
  - IllegalArgumentException is thrown: "Device name cannot be null"

---

### 2.6 handleDeviceCommand() - HeartbeatCommand Tests

#### Test: handleHeartbeatCommand_State1_DeadDead_TransitionsToAlive
- **Given:** Device in State 1 (DEAD/DEAD), current time = BASE_TIME
- **When:** handleDeviceCommand(HeartbeatCommand, context(BASE_TIME))
- **Then:**
  - Device state transitions from DEAD to ALIVE
  - `RegisteredDeviceStartDE` event is raised
  - lastCommandReceiveTime is updated to BASE_TIME
  - evaluateCurrentStateAndCommand() runs after handling
  - Returns empty command list (device is ALIVE but desiredState is DEAD)

#### Test: handleHeartbeatCommand_State2_DeadAlive_TransitionsToAlive
- **Given:** Device in State 2 (DEAD/ALIVE)
- **When:** handleDeviceCommand(HeartbeatCommand, context(BASE_TIME))
- **Then:**
  - Device state transitions from DEAD to ALIVE
  - `RegisteredDeviceStartDE` event is raised
  - lastCommandReceiveTime is updated
  - Returns commands from evaluation (likely config or keepalive since now ALIVE/ALIVE)

#### Test: handleHeartbeatCommand_State3_AliveAlive_RemainsAlive
- **Given:** Device in State 3 (ALIVE/ALIVE)
- **When:** handleDeviceCommand(HeartbeatCommand, context(BASE_TIME))
- **Then:**
  - Device state remains ALIVE
  - No `RegisteredDeviceStartDE` event (no state change)
  - lastCommandReceiveTime is updated
  - Returns result of evaluateCurrentStateAndCommand()

#### Test: handleHeartbeatCommand_State4_AliveDead_RemainsAlive
- **Given:** Device in State 4 (ALIVE/DEAD)
- **When:** handleDeviceCommand(HeartbeatCommand, context(BASE_TIME))
- **Then:**
  - Device state remains ALIVE
  - lastCommandReceiveTime is updated
  - Returns empty list (desiredState is DEAD, so no commands sent)

---

### 2.7 handleDeviceCommand() - AckStateCommand Tests

#### Test: handleAckStateCommand_State1_DeadDead_TransitionsToAlive
- **Given:** Device in State 1 (DEAD/DEAD)
- **When:** handleDeviceCommand(AckStateCommand[all OFF], context)
- **Then:**
  - Device transitions to ALIVE
  - All circuits synchronized to OFF
  - `RegisteredDeviceStartDE` event is raised
  - lastCommandReceiveTime is updated

#### Test: handleAckStateCommand_State2_DeadAlive_TransitionsAndSynchronizes
- **Given:** Device in State 2 (DEAD/ALIVE) with unsynchronized circuits
- **When:** handleDeviceCommand(AckStateCommand, context)
- **Then:**
  - Device transitions to ALIVE
  - Circuits are synchronized to acknowledged states
  - `RegisteredDeviceStartDE` event is raised
  - Returns commands from evaluation

#### Test: handleAckStateCommand_State3_AliveAlive_SynchronizesCircuits
- **Given:** Device in State 3 (ALIVE/ALIVE) with circuit 1 desired=ON, actual=OFF
- **When:** handleDeviceCommand(AckStateCommand[circuit 1 = ON], context)
- **Then:**
  - Circuit 1's actual state is set to ON (synchronized)
  - Device remains ALIVE
  - lastCommandReceiveTime is updated

#### Test: handleAckStateCommand_State4_AliveDead_Synchronizes
- **Given:** Device in State 4 (ALIVE/DEAD)
- **When:** handleDeviceCommand(AckStateCommand, context)
- **Then:**
  - Circuits are synchronized
  - Device remains ALIVE
  - No commands returned (desiredState is DEAD)

---

### 2.8 handleDeviceCommand() - AckConfigCommand Tests

#### Test: handleAckConfigCommand_State1_DeadDead_TransitionsAndUpdatesConfig
- **Given:** Device in State 1 (DEAD/DEAD)
- **When:** handleDeviceCommand(AckConfigCommand[30s, 10s], context)
- **Then:**
  - Device transitions to ALIVE
  - deviceConfig is updated to acknowledged values
  - `RegisteredDeviceStartDE` event is raised
  - lastCommandReceiveTime is updated

#### Test: handleAckConfigCommand_State2_DeadAlive_TransitionsAndUpdatesConfig
- **Given:** Device in State 2 (DEAD/ALIVE)
- **When:** handleDeviceCommand(AckConfigCommand, context)
- **Then:**
  - Device transitions to ALIVE
  - deviceConfig is updated
  - Returns commands from evaluation

#### Test: handleAckConfigCommand_State3_AliveAlive_UpdatesConfig
- **Given:** Device in State 3 (ALIVE/ALIVE) with different desiredConfig
- **When:** handleDeviceCommand(AckConfigCommand[new values], context)
- **Then:**
  - deviceConfig is updated to new values
  - Device remains ALIVE
  - Config is now synchronized

#### Test: handleAckConfigCommand_State4_AliveDead_UpdatesConfig
- **Given:** Device in State 4 (ALIVE/DEAD)
- **When:** handleDeviceCommand(AckConfigCommand, context)
- **Then:**
  - deviceConfig is updated
  - Device remains ALIVE

---

### 2.9 handleDeviceCommand() - RequestConfigCommand Tests

#### Test: handleRequestConfigCommand_State1_DeadDead_ResetsConfigAndTransitions
- **Given:** Device in State 1 (DEAD/DEAD) with custom config
- **When:** handleDeviceCommand(RequestConfigCommand, context)
- **Then:**
  - Device transitions to ALIVE
  - deviceConfig is reset to default values
  - `RegisteredDeviceStartDE` event is raised

#### Test: handleRequestConfigCommand_State2_DeadAlive_ResetsConfig
- **Given:** Device in State 2 (DEAD/ALIVE)
- **When:** handleDeviceCommand(RequestConfigCommand, context)
- **Then:**
  - Device transitions to ALIVE
  - deviceConfig is reset to defaults
  - Returns commands (likely ConfigCommand with desired config)

#### Test: handleRequestConfigCommand_State3_AliveAlive_ResetsConfig
- **Given:** Device in State 3 (ALIVE/ALIVE) with custom config
- **When:** handleDeviceCommand(RequestConfigCommand, context)
- **Then:**
  - deviceConfig is reset to default DeviceConfig()
  - Device remains ALIVE
  - May trigger ConfigCommand on next evaluation if defaults != desired

#### Test: handleRequestConfigCommand_State4_AliveDead_ResetsConfig
- **Given:** Device in State 4 (ALIVE/DEAD)
- **When:** handleDeviceCommand(RequestConfigCommand, context)
- **Then:**
  - deviceConfig is reset to defaults
  - Device remains ALIVE

---

### 2.10 handleDeviceCommand() - Outbound Commands (Error Cases)

#### Test: handleOutboundCommands_AllStates_ThrowsException
- **Given:** Device in any state
- **When:** handleDeviceCommand(outboundCommand, context) where outboundCommand is:
  - KeepAliveCommand
  - StartCommand
  - NewStateCommand
  - ConfigCommand
- **Then:**
  - IllegalStateException is thrown: "Device does not handle a command of type X"
  - No state changes occur

---

## 3. Domain Event Verification Tests

**File:** `DeviceDomainEventTest.java`

### 3.1 RegisteredDeviceStartDE Event Tests

#### Test: deviceStartEvent_State1ToAlive_RaisedOnHeartbeat
- **Given:** Device in State 1 (DEAD/DEAD)
- **When:** HeartbeatCommand is handled
- **Then:**
  - `RegisteredDeviceStartDE(deviceId, timestamp)` event is raised
  - Event contains correct deviceId and timestamp

#### Test: deviceStartEvent_State2ToAlive_RaisedOnHeartbeat
- **Given:** Device in State 2 (DEAD/ALIVE)
- **When:** HeartbeatCommand is handled
- **Then:**
  - `RegisteredDeviceStartDE` event is raised

#### Test: deviceStartEvent_NotRaisedWhenAlreadyAlive
- **Given:** Device in State 3 (ALIVE/ALIVE)
- **When:** HeartbeatCommand is handled
- **Then:**
  - No `RegisteredDeviceStartDE` event is raised (state unchanged)

---

### 3.2 RegisteredDeviceShutdownDE Event Tests

#### Test: deviceShutdownEvent_State3ToDead_RaisedOnTimeout
- **Given:** Device in State 3 (ALIVE/ALIVE) with lastCommandReceiveTime = 60 seconds ago
- **When:** evaluateCurrentStateAndCommand(now) is called
- **Then:**
  - Device transitions from ALIVE to DEAD
  - `RegisteredDeviceShutdownDE(deviceId, timestamp)` event is raised

#### Test: deviceShutdownEvent_State4ToDead_RaisedOnTimeout
- **Given:** Device in State 4 (ALIVE/DEAD) with lastCommandReceiveTime beyond timeout
- **When:** evaluateCurrentStateAndCommand(now) is called
- **Then:**
  - Device transitions to DEAD
  - `RegisteredDeviceShutdownDE` event is raised

#### Test: deviceShutdownEvent_NotRaisedWhenAlreadyDead
- **Given:** Device in State 1 (DEAD/DEAD)
- **When:** evaluateCurrentStateAndCommand() is called
- **Then:**
  - No `RegisteredDeviceShutdownDE` event is raised

---

### 3.3 KeepaliveSentDE Event Tests

#### Test: keepaliveEvent_State3_RaisedWhenKeepaliveSent
- **Given:** Device in State 3 needing keepalive
- **When:** evaluateCurrentStateAndCommand() generates KeepAliveCommand
- **Then:**
  - `KeepaliveSentDE(deviceId, timestamp)` event is raised
  - lastKeepAliveSendTime is updated

#### Test: keepaliveEvent_NotRaisedInDeadStates
- **Given:** Device in State 1 or State 2
- **When:** evaluateCurrentStateAndCommand() is called
- **Then:**
  - No `KeepaliveSentDE` event is raised (dead devices don't send keepalive)

---

## Test Execution Summary

### Total Test Methods: **52**

**Breakdown by Category:**
- evaluateCurrentStateAndCommand: 8 tests
- requestChangeCircuitState: 5 tests
- enable: 4 tests
- disable: 4 tests
- rename: 5 tests
- handleDeviceCommand (HeartbeatCommand): 4 tests
- handleDeviceCommand (AckStateCommand): 4 tests
- handleDeviceCommand (AckConfigCommand): 4 tests
- handleDeviceCommand (RequestConfigCommand): 4 tests
- handleDeviceCommand (Outbound commands - error cases): 4 tests
- Domain events verification: 6 tests

### Coverage Focus:
- ✅ All Device public methods tested in all 4 valid states
- ✅ State transitions verified (DEAD→ALIVE, ALIVE→DEAD)
- ✅ Domain event emission for state changes
- ✅ Command generation based on state
- ✅ Error cases (dead device operations, invalid circuit IDs)
- ✅ Edge cases (null values, idempotent operations)

---

## Implementation Notes

### Test Dependencies
- **JUnit 5** - @ParameterizedTest, @MethodSource
- **AssertJ** - Fluent assertions
- **Mockito** - Not needed (pure domain logic, no external dependencies to mock)

### Test Execution Strategy
1. Use `DeviceTestCases` factory methods to create consistent test data
2. Use parameterized tests where appropriate to reduce duplication
3. Verify domain events using aggregate root's `getDomainEvents()` method
4. Test state transitions by checking before/after states
5. Validate command generation by inspecting returned command types and payloads

### Example Test Structure
```java
@Test
void evaluateCurrentStateAndCommand_State3_AliveAlive_NeedsKeepalive() {
    // Given
    Device device = DeviceTestCases.createState3_AliveAlive_NeedsKeepalive();
    DeviceContext context = new DeviceContext(BASE_TIME);

    // When
    List<DeviceCommand> commands = device.evaluateCurrentStateAndCommand(context);

    // Then
    assertThat(commands).hasSize(1);
    assertThat(commands.get(0)).isInstanceOf(KeepAliveCommand.class);

    List<HydrogardenDomainEvent> events = device.getDomainEvents();
    assertThat(events).hasSize(1);
    assertThat(events.get(0)).isInstanceOf(KeepaliveSentDE.class);

    KeepaliveSentDE event = (KeepaliveSentDE) events.get(0);
    assertThat(event.getDeviceId()).isEqualTo(TEST_DEVICE_ID);
    assertThat(event.getTimestamp()).isEqualTo(BASE_TIME);
}
```

---

## Next Steps

1. **Review DeviceTestCases class** - Ensure state definitions match domain requirements
2. **Implement DeviceTestCases.java** - Create the test state factory
3. **Implement DeviceStateBasedTest.java** - Write all 46 state-based method tests
4. **Implement DeviceDomainEventTest.java** - Write 6 event verification tests
5. **Run tests and validate coverage** - Ensure all states and transitions are covered
6. **Iterate on edge cases** - Add additional tests as needed based on findings