package com.hydrogarden.business.device.core.entity;

import com.hydrogarden.business.device.core.DeviceTestCases;
import com.hydrogarden.business.device.core.commands.*;
import com.hydrogarden.business.device.core.event.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static com.hydrogarden.business.device.core.DeviceTestCases.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for the Device aggregate root entity.
 * Tests cover all valid device states (DEAD/DEAD, DEAD/ALIVE, ALIVE/ALIVE, ALIVE/DEAD)
 * with focus on state transitions, synchronization, and nullability handling.
 */
@DisplayName("Device Entity Tests")
class DeviceTest {

    @Test
    @DisplayName("State 1 (DEAD/DEAD): evaluateCurrentStateAndCommand returns empty list when device is dead and should stay dead")
    void evaluateCurrentStateAndCommand_State1_DeadDead() {
        Device device = DeviceTestCases.createState1_DeadDead();
        DeviceContext context = new DeviceContext(BASE_TIME);

        List<OutboundDeviceCommand> commands = device.evaluateCurrentStateAndCommand(context);

        assertThat(commands)
                .as("Dead device with desired state DEAD should not generate any commands")
                .isEmpty();
        assertThat(device.getDeviceVitals().getState())
                .as("Device actual state should remain DEAD")
                .isEqualTo(DeviceState.DEAD);
        assertThat(device.getDeviceVitals().getDesiredState())
                .as("Device desired state should remain DEAD")
                .isEqualTo(DeviceState.DEAD);
        assertThat(device.getDomainEvents())
                .as("No domain events should be raised when state does not change")
                .isEmpty();
    }

    @Test
    @DisplayName("State 1 (DEAD/DEAD) with config difference: evaluateCurrentStateAndCommand returns empty list because device is dead")
    void evaluateCurrentStateAndCommand_State1_DeadDead_ConfigDiffers() {
        Device device = DeviceTestCases.createState1_DeadDead_ConfigDiffers();
        DeviceContext context = new DeviceContext(BASE_TIME);

        List<OutboundDeviceCommand> commands = device.evaluateCurrentStateAndCommand(context);

        assertThat(commands)
                .as("Dead device should not send config commands even when config differs")
                .isEmpty();
        assertThat(device.getDeviceVitals().getDeviceConfig())
                .as("Current config should remain unchanged")
                .isEqualTo(new DeviceConfig(DEFAULT_STANDBY_TIMEOUT, DEFAULT_HEARTBEAT_INTERVAL));
        assertThat(device.getDeviceVitals().getDesiredDeviceConfig())
                .as("Desired config should remain different")
                .isEqualTo(new DeviceConfig(Duration.ofSeconds(45), Duration.ofSeconds(15)));
    }

    @Test
    @DisplayName("State 2 (DEAD/ALIVE): evaluateCurrentStateAndCommand returns empty list when device is dead but waiting to be alive")
    void evaluateCurrentStateAndCommand_State2_DeadAlive() {
        Device device = DeviceTestCases.createState2_DeadAlive();
        DeviceContext context = new DeviceContext(BASE_TIME);

        List<OutboundDeviceCommand> commands = device.evaluateCurrentStateAndCommand(context);

        assertThat(commands)
                .as("Dead device cannot send commands until it transitions to ALIVE via incoming command")
                .isEmpty();
        assertThat(device.getDeviceVitals().getState())
                .as("Device actual state should remain DEAD until heartbeat received")
                .isEqualTo(DeviceState.DEAD);
        assertThat(device.getDeviceVitals().getDesiredState())
                .as("Device desired state should remain ALIVE")
                .isEqualTo(DeviceState.ALIVE);
        assertThat(device.getDeviceVitals().getLastCommandReceiveTime())
                .as("Last command receive time should be null for device that never responded")
                .isNull();
    }

    @Test
    @DisplayName("State 2 (DEAD/ALIVE) with config difference and interval elapsed: evaluateCurrentStateAndCommand returns ConfigCommand")
    void evaluateCurrentStateAndCommand_State2_DeadAlive_ConfigDiffers_IntervalElapsed() {
        Device device = DeviceTestCases.createState2_DeadAlive_ConfigDiffers();
        device.getDeviceVitals().setLastConfigSendTime(BASE_TIME.minus(Duration.ofSeconds(25)));
        DeviceContext context = new DeviceContext(BASE_TIME);

        List<OutboundDeviceCommand> commands = device.evaluateCurrentStateAndCommand(context);

        assertThat(commands)
                .as("Dead device with desired state ALIVE should send config when interval elapsed (25s > 20s)")
                .hasSize(1);
        assertThat(commands.getFirst())
                .as("Command should be ConfigCommand")
                .isInstanceOf(ConfigCommand.class);

        ConfigCommand configCommand = (ConfigCommand) commands.getFirst();
        assertThat(configCommand.getDeviceId())
                .as("ConfigCommand should contain correct device ID")
                .isEqualTo(TEST_DEVICE_ID);
        assertThat(configCommand.getStandbyTimeout())
                .as("ConfigCommand should contain desired standby timeout")
                .isEqualTo(Duration.ofSeconds(45));
        assertThat(configCommand.getHeartbeatInterval())
                .as("ConfigCommand should contain desired heartbeat interval")
                .isEqualTo(Duration.ofSeconds(15));

        assertThat(device.getDeviceVitals().getState())
                .as("Device actual state should remain DEAD")
                .isEqualTo(DeviceState.DEAD);
        assertThat(device.getDeviceVitals().getDesiredState())
                .as("Device desired state should remain ALIVE")
                .isEqualTo(DeviceState.ALIVE);
        assertThat(device.getDeviceVitals().getDeviceConfig())
                .as("Current config should remain unchanged until ACK received")
                .isEqualTo(new DeviceConfig(DEFAULT_STANDBY_TIMEOUT, DEFAULT_HEARTBEAT_INTERVAL));
    }

    @Test
    @DisplayName("State 2 (DEAD/ALIVE) with config difference but interval not elapsed: evaluateCurrentStateAndCommand returns empty list")
    void evaluateCurrentStateAndCommand_State2_DeadAlive_ConfigDiffers_IntervalNotElapsed() {
        Device device = DeviceTestCases.createState2_DeadAlive_ConfigDiffers();
        device.getDeviceVitals().setLastConfigSendTime(BASE_TIME.minus(Duration.ofSeconds(10)));
        DeviceContext context = new DeviceContext(BASE_TIME);

        List<OutboundDeviceCommand> commands = device.evaluateCurrentStateAndCommand(context);

        assertThat(commands)
                .as("Config command should not be sent when configInterval has not elapsed (10s < 20s)")
                .isEmpty();
        assertThat(device.getDeviceVitals().getState())
                .as("Device should remain DEAD")
                .isEqualTo(DeviceState.DEAD);
        assertThat(device.getDeviceVitals().getDesiredState())
                .as("Desired state should remain ALIVE")
                .isEqualTo(DeviceState.ALIVE);
        assertThat(device.getDeviceVitals().getDesiredDeviceConfig())
                .as("Desired config should be different from current config")
                .isNotEqualTo(device.getDeviceVitals().getDeviceConfig());
    }

    @Test
    @DisplayName("State 3 (ALIVE/ALIVE): evaluateCurrentStateAndCommand returns empty list when device is fully synchronized")
    void evaluateCurrentStateAndCommand_State3_AliveAlive() {
        Device device = DeviceTestCases.createState3_AliveAlive();
        DeviceContext context = new DeviceContext(BASE_TIME);

        List<OutboundDeviceCommand> commands = device.evaluateCurrentStateAndCommand(context);

        assertThat(commands)
                .as("Fully synchronized alive device should not generate commands")
                .isEmpty();
        assertThat(device.getDeviceVitals().getState())
                .as("Device should remain ALIVE")
                .isEqualTo(DeviceState.ALIVE);
        assertThat(device.getDeviceVitals().getDesiredState())
                .as("Desired state should remain ALIVE")
                .isEqualTo(DeviceState.ALIVE);
        assertThat(device.getDeviceVitals().getLastCommandReceiveTime())
                .as("Last command receive time should not be null for alive device")
                .isNotNull();
    }

    @Test
    @DisplayName("State 3 (ALIVE/ALIVE) with config difference but interval not elapsed: evaluateCurrentStateAndCommand returns empty list")
    void evaluateCurrentStateAndCommand_State3_AliveAlive_ConfigDiffers_IntervalNotElapsed() {
        Device device = DeviceTestCases.createState3_AliveAlive_ConfigDiffers();
        DeviceContext context = new DeviceContext(BASE_TIME);

        List<OutboundDeviceCommand> commands = device.evaluateCurrentStateAndCommand(context);

        assertThat(commands)
                .as("Config command should not be sent when configInterval has not elapsed (10s < 20s)")
                .isEmpty();
        assertThat(device.getDeviceVitals().getLastConfigSendTime())
                .as("Last config send time should be 10 seconds ago")
                .isEqualTo(BASE_TIME.minus(Duration.ofSeconds(10)));
        assertThat(device.getDeviceVitals().getDeviceConfig())
                .as("Current config should remain unchanged")
                .isEqualTo(new DeviceConfig(DEFAULT_STANDBY_TIMEOUT, DEFAULT_HEARTBEAT_INTERVAL));
        assertThat(device.getDeviceVitals().getDesiredDeviceConfig())
                .as("Desired config should remain different")
                .isEqualTo(new DeviceConfig(Duration.ofSeconds(45), Duration.ofSeconds(15)));
    }

    @Test
    @DisplayName("State 3 (ALIVE/ALIVE) with config difference and interval elapsed: evaluateCurrentStateAndCommand returns ConfigCommand")
    void evaluateCurrentStateAndCommand_State3_AliveAlive_ConfigDiffers_IntervalElapsed() {
        Device device = DeviceTestCases.createState3_AliveAlive_ConfigDiffers();
        device.getDeviceVitals().setLastConfigSendTime(BASE_TIME.minus(Duration.ofSeconds(25)));
        DeviceContext context = new DeviceContext(BASE_TIME);

        List<OutboundDeviceCommand> commands = device.evaluateCurrentStateAndCommand(context);

        assertThat(commands)
                .as("Config command should be sent when configInterval has elapsed (25s > 20s)")
                .hasSize(1);
        assertThat(commands.getFirst())
                .as("First command should be ConfigCommand")
                .isInstanceOf(ConfigCommand.class);

        ConfigCommand configCommand = (ConfigCommand) commands.getFirst();
        assertThat(configCommand.getDeviceId())
                .as("ConfigCommand should contain correct device ID")
                .isEqualTo(TEST_DEVICE_ID);
        assertThat(configCommand.getStandbyTimeout())
                .as("ConfigCommand should contain desired standby timeout")
                .isEqualTo(Duration.ofSeconds(45));
        assertThat(configCommand.getHeartbeatInterval())
                .as("ConfigCommand should contain desired heartbeat interval")
                .isEqualTo(Duration.ofSeconds(15));
        assertThat(device.getDeviceVitals().getDeviceConfig())
                .as("Current config should remain unchanged until ACK received")
                .isEqualTo(new DeviceConfig(DEFAULT_STANDBY_TIMEOUT, DEFAULT_HEARTBEAT_INTERVAL));
    }

    @Test
    @DisplayName("State 3 (ALIVE/ALIVE) needing keepalive: evaluateCurrentStateAndCommand returns KeepAliveCommand")
    void evaluateCurrentStateAndCommand_State3_AliveAlive_NeedsKeepalive() {
        Device device = DeviceTestCases.createState3_AliveAlive_NeedsKeepalive();
        DeviceContext context = new DeviceContext(BASE_TIME);

        List<OutboundDeviceCommand> commands = device.evaluateCurrentStateAndCommand(context);

        assertThat(commands)
                .as("Device needing keepalive should generate KeepAliveCommand")
                .hasSize(1);
        assertThat(commands.getFirst())
                .as("Command should be KeepAliveCommand")
                .isInstanceOf(KeepAliveCommand.class);

        assertThat(device.getDeviceVitals().getLastKeepAliveSendTime())
                .as("Last keepalive send time should be updated to current time")
                .isEqualTo(BASE_TIME);

        List<HydrogardenDomainEvent> events = device.getDomainEvents();
        assertThat(events)
                .as("KeepaliveSentDE domain event should be raised")
                .hasSize(1);
        assertThat(events.getFirst())
                .as("Event should be KeepaliveSentDE")
                .isInstanceOf(KeepaliveSentDE.class);

        KeepaliveSentDE event = (KeepaliveSentDE) events.getFirst();
        assertThat(event.getDeviceId())
                .as("Event should contain correct device ID")
                .isEqualTo(TEST_DEVICE_ID);
        assertThat(event.getTimestamp())
                .as("Event timestamp should match current time")
                .isEqualTo(BASE_TIME);
    }

    @Test
    @DisplayName("State 3 (ALIVE/ALIVE) with unsynchronized circuits but interval not elapsed: evaluateCurrentStateAndCommand returns empty list")
    void evaluateCurrentStateAndCommand_State3_AliveAlive_UnsynchronizedCircuits_IntervalNotElapsed() {
        Device device = DeviceTestCases.createState3_AliveAlive_UnsynchronizedCircuits();
        DeviceContext context = new DeviceContext(BASE_TIME);

        List<OutboundDeviceCommand> commands = device.evaluateCurrentStateAndCommand(context);

        assertThat(commands)
                .as("NewState command should not be sent when newStateInterval has not elapsed (3s < 5s)")
                .isEmpty();
        assertThat(device.getDeviceVitals().getLastNewStateSendTime())
                .as("Last new state send time should be 3 seconds ago")
                .isEqualTo(BASE_TIME.minus(Duration.ofSeconds(3)));
        assertThat(device.getCircuits().getFirst().isUnsynchronised())
                .as("First circuit should remain unsynchronized")
                .isTrue();
        assertThat(device.getCircuits().getFirst().getState())
                .as("First circuit actual state should be OFF")
                .isEqualTo(new CircuitState(false));
        assertThat(device.getCircuits().getFirst().getDesiredState())
                .as("First circuit desired state should be ON")
                .isEqualTo(new CircuitState(true));
    }

    @Test
    @DisplayName("State 3 (ALIVE/ALIVE) with unsynchronized circuits and interval elapsed: evaluateCurrentStateAndCommand returns NewStateCommand")
    void evaluateCurrentStateAndCommand_State3_AliveAlive_UnsynchronizedCircuits_IntervalElapsed() {
        Device device = DeviceTestCases.createState3_AliveAlive_UnsynchronizedCircuits();
        device.getDeviceVitals().setLastNewStateSendTime(BASE_TIME.minus(Duration.ofSeconds(6)));
        DeviceContext context = new DeviceContext(BASE_TIME);

        List<OutboundDeviceCommand> commands = device.evaluateCurrentStateAndCommand(context);

        assertThat(commands)
                .as("Device with unsynchronized circuits should generate NewStateCommand when interval elapsed (6s > 5s)")
                .hasSize(1);
        assertThat(commands.getFirst())
                .as("Command should be NewStateCommand")
                .isInstanceOf(NewStateCommand.class);

        NewStateCommand newStateCommand = (NewStateCommand) commands.getFirst();
        assertThat(newStateCommand.getDeviceId())
                .as("NewStateCommand should contain correct device ID")
                .isEqualTo(TEST_DEVICE_ID);
        assertThat(newStateCommand.getStates())
                .as("NewStateCommand should contain all 8 circuit desired states")
                .hasSize(8);
        assertThat(newStateCommand.getStates().get(0))
                .as("First circuit desired state should be ON (true)")
                .isEqualTo(new CircuitState(true));
        assertThat(newStateCommand.getStates().get(1))
                .as("Second circuit desired state should be OFF (false)")
                .isEqualTo(new CircuitState(false));
    }

    @Test
    @DisplayName("State 4 (ALIVE/DEAD): evaluateCurrentStateAndCommand returns empty list when device is alive but should be dead")
    void evaluateCurrentStateAndCommand_State4_AliveDead() {
        Device device = DeviceTestCases.createState4_AliveDead();
        DeviceContext context = new DeviceContext(BASE_TIME);

        List<OutboundDeviceCommand> commands = device.evaluateCurrentStateAndCommand(context);

        assertThat(commands)
                .as("Device with desired state DEAD should not send keepalive or config commands")
                .isEmpty();
        assertThat(device.getDeviceVitals().getState())
                .as("Device should remain ALIVE until timeout occurs")
                .isEqualTo(DeviceState.ALIVE);
        assertThat(device.getDeviceVitals().getDesiredState())
                .as("Desired state should remain DEAD")
                .isEqualTo(DeviceState.DEAD);
    }

    @Test
    @DisplayName("State 4 (ALIVE/DEAD) with config difference: evaluateCurrentStateAndCommand returns empty list")
    void evaluateCurrentStateAndCommand_State4_AliveDead_ConfigDiffers() {
        Device device = DeviceTestCases.createState4_AliveDead_ConfigDiffers();
        DeviceContext context = new DeviceContext(BASE_TIME);

        List<OutboundDeviceCommand> commands = device.evaluateCurrentStateAndCommand(context);

        assertThat(commands)
                .as("Device being disabled should not send config commands even when config differs")
                .isEmpty();
        assertThat(device.getDeviceVitals().getDesiredState())
                .as("Desired state should be DEAD")
                .isEqualTo(DeviceState.DEAD);
        assertThat(device.getDeviceVitals().getDesiredDeviceConfig())
                .as("Desired config should differ from current config")
                .isNotEqualTo(device.getDeviceVitals().getDeviceConfig());
    }

    @Test
    @DisplayName("HeartbeatCommand on State 1 (DEAD/DEAD): device transitions to ALIVE and raises RegisteredDeviceStartDE")
    void handleInboundDeviceCommand_HeartbeatCommand_State1_DeadDead_TransitionsToAlive() {
        Device device = DeviceTestCases.createState1_DeadDead();
        HeartbeatCommand heartbeatCommand = new HeartbeatCommand(TEST_DEVICE_ID);
        DeviceContext context = new DeviceContext(BASE_TIME);

        List<OutboundDeviceCommand> commands = device.handleInboundDeviceCommand(heartbeatCommand, context);

        assertThat(device.getDeviceVitals().getState())
                .as("Device should transition from DEAD to ALIVE upon receiving heartbeat")
                .isEqualTo(DeviceState.ALIVE);
        assertThat(device.getDeviceVitals().getLastCommandReceiveTime())
                .as("Last command receive time should be updated to current time")
                .isEqualTo(BASE_TIME);

        List<HydrogardenDomainEvent> events = device.getDomainEvents();
        assertThat(events)
                .as("RegisteredDeviceStartDE event should be raised when device transitions to ALIVE")
                .hasSize(1);
        assertThat(events.getFirst())
                .as("Event should be RegisteredDeviceStartDE")
                .isInstanceOf(RegisteredDeviceStartDE.class);

        RegisteredDeviceStartDE event = (RegisteredDeviceStartDE) events.getFirst();
        assertThat(event.getDeviceId())
                .as("Event should contain correct device ID")
                .isEqualTo(TEST_DEVICE_ID);
        assertThat(event.getTimestamp())
                .as("Event timestamp should match current time")
                .isEqualTo(BASE_TIME);

        assertThat(commands)
                .as("No commands should be returned for DEAD/DEAD device after heartbeat (desired state is DEAD)")
                .isEmpty();
    }

    @Test
    @DisplayName("HeartbeatCommand on State 2 (DEAD/ALIVE): device transitions to ALIVE and may return commands")
    void handleInboundDeviceCommand_HeartbeatCommand_State2_DeadAlive_TransitionsToAlive() {
        Device device = DeviceTestCases.createState2_DeadAlive();
        HeartbeatCommand heartbeatCommand = new HeartbeatCommand(TEST_DEVICE_ID);
        DeviceContext context = new DeviceContext(BASE_TIME);

        List<OutboundDeviceCommand> commands = device.handleInboundDeviceCommand(heartbeatCommand, context);

        assertThat(device.getDeviceVitals().getState())
                .as("Device should transition from DEAD to ALIVE")
                .isEqualTo(DeviceState.ALIVE);
        assertThat(device.getDeviceVitals().getDesiredState())
                .as("Desired state should remain ALIVE")
                .isEqualTo(DeviceState.ALIVE);
        assertThat(device.getDeviceVitals().getLastCommandReceiveTime())
                .as("Last command receive time should be set to current time")
                .isEqualTo(BASE_TIME);

        List<HydrogardenDomainEvent> events = device.getDomainEvents();
        assertThat(events)
                .as("RegisteredDeviceStartDE should be raised on transition to ALIVE")
                .isNotEmpty();
        assertThat(events.stream().anyMatch(e -> e instanceof RegisteredDeviceStartDE))
                .as("Events should contain RegisteredDeviceStartDE")
                .isTrue();
    }

    @Test
    @DisplayName("HeartbeatCommand on State 3 (ALIVE/ALIVE): device remains ALIVE and no state change event")
    void handleInboundDeviceCommand_HeartbeatCommand_State3_AliveAlive_RemainsAlive() {
        Device device = DeviceTestCases.createState3_AliveAlive();
        HeartbeatCommand heartbeatCommand = new HeartbeatCommand(TEST_DEVICE_ID);
        DeviceContext context = new DeviceContext(BASE_TIME);

        LocalDateTime previousLastCommandTime = device.getDeviceVitals().getLastCommandReceiveTime();

        List<OutboundDeviceCommand> commands = device.handleInboundDeviceCommand(heartbeatCommand, context);

        assertThat(device.getDeviceVitals().getState())
                .as("Device should remain ALIVE")
                .isEqualTo(DeviceState.ALIVE);
        assertThat(device.getDeviceVitals().getLastCommandReceiveTime())
                .as("Last command receive time should be updated to current time")
                .isEqualTo(BASE_TIME)
                .isNotEqualTo(previousLastCommandTime);

        List<HydrogardenDomainEvent> events = device.getDomainEvents();
        assertThat(events.stream().noneMatch(e -> e instanceof RegisteredDeviceStartDE))
                .as("No RegisteredDeviceStartDE event should be raised when state does not change")
                .isTrue();
    }

    @Test
    @DisplayName("HeartbeatCommand on State 4 (ALIVE/DEAD): device remains ALIVE and updates last command time")
    void handleInboundDeviceCommand_HeartbeatCommand_State4_AliveDead_RemainsAlive() {
        Device device = DeviceTestCases.createState4_AliveDead();
        HeartbeatCommand heartbeatCommand = new HeartbeatCommand(TEST_DEVICE_ID);
        DeviceContext context = new DeviceContext(BASE_TIME);

        List<OutboundDeviceCommand> commands = device.handleInboundDeviceCommand(heartbeatCommand, context);

        assertThat(device.getDeviceVitals().getState())
                .as("Device should remain ALIVE despite desired state being DEAD")
                .isEqualTo(DeviceState.ALIVE);
        assertThat(device.getDeviceVitals().getDesiredState())
                .as("Desired state should remain DEAD")
                .isEqualTo(DeviceState.DEAD);
        assertThat(device.getDeviceVitals().getLastCommandReceiveTime())
                .as("Last command receive time should be updated")
                .isEqualTo(BASE_TIME);
        assertThat(commands)
                .as("No commands should be returned when desired state is DEAD")
                .isEmpty();
    }

    @Test
    @DisplayName("AckStateCommand on State 1 (DEAD/DEAD): device transitions to ALIVE and synchronizes circuits")
    void handleInboundDeviceCommand_AckStateCommand_State1_DeadDead_TransitionsAndSynchronizes() {
        Device device = DeviceTestCases.createState1_DeadDead();
        List<CircuitState> acknowledgedStates = List.of(
                new CircuitState(false), new CircuitState(false), new CircuitState(false), new CircuitState(false),
                new CircuitState(false), new CircuitState(false), new CircuitState(false), new CircuitState(false)
        );
        AckStateCommand ackStateCommand = new AckStateCommand(TEST_DEVICE_ID, acknowledgedStates);
        DeviceContext context = new DeviceContext(BASE_TIME);

        List<OutboundDeviceCommand> commands = device.handleInboundDeviceCommand(ackStateCommand, context);

        assertThat(device.getDeviceVitals().getState())
                .as("Device should transition to ALIVE when receiving AckStateCommand")
                .isEqualTo(DeviceState.ALIVE);
        assertThat(device.getDeviceVitals().getLastCommandReceiveTime())
                .as("Last command receive time should be updated")
                .isEqualTo(BASE_TIME);

        List<Circuit> circuits = device.getCircuits();
        for (int i = 0; i < circuits.size(); i++) {
            assertThat(circuits.get(i).getState())
                    .as("Circuit %d actual state should be synchronized to acknowledged state", i + 1)
                    .isEqualTo(acknowledgedStates.get(i));
        }

        List<HydrogardenDomainEvent> events = device.getDomainEvents();
        assertThat(events.stream().anyMatch(e -> e instanceof RegisteredDeviceStartDE))
                .as("RegisteredDeviceStartDE event should be raised on DEAD to ALIVE transition")
                .isTrue();
    }

    @Test
    @DisplayName("AckStateCommand on State 3 (ALIVE/ALIVE) with unsynchronized circuits: synchronizes circuit states")
    void handleInboundDeviceCommand_AckStateCommand_State3_AliveAlive_SynchronizesCircuits() {
        Device device = DeviceTestCases.createState3_AliveAlive_UnsynchronizedCircuits();
        List<CircuitState> acknowledgedStates = List.of(
                new CircuitState(true), new CircuitState(false), new CircuitState(false), new CircuitState(false),
                new CircuitState(false), new CircuitState(false), new CircuitState(false), new CircuitState(false)
        );
        AckStateCommand ackStateCommand = new AckStateCommand(TEST_DEVICE_ID, acknowledgedStates);
        DeviceContext context = new DeviceContext(BASE_TIME);

        device.handleInboundDeviceCommand(ackStateCommand, context);

        assertThat(device.getDeviceVitals().getState())
                .as("Device should remain ALIVE")
                .isEqualTo(DeviceState.ALIVE);

        Circuit firstCircuit = device.getCircuits().getFirst();
        assertThat(firstCircuit.getState())
                .as("First circuit actual state should be synchronized to ON")
                .isEqualTo(new CircuitState(true));
        assertThat(firstCircuit.getDesiredState())
                .as("First circuit desired state should remain ON")
                .isEqualTo(new CircuitState(true));
        assertThat(firstCircuit.isUnsynchronised())
                .as("First circuit should now be synchronized")
                .isFalse();
    }

    @Test
    @DisplayName("AckConfigCommand on State 1 (DEAD/DEAD): device transitions to ALIVE and updates config")
    void handleInboundDeviceCommand_AckConfigCommand_State1_DeadDead_TransitionsAndUpdatesConfig() {
        Device device = DeviceTestCases.createState1_DeadDead_ConfigDiffers();
        AckConfigCommand ackConfigCommand = new AckConfigCommand(
                TEST_DEVICE_ID,
                Duration.ofSeconds(45),
                Duration.ofSeconds(15)
        );
        DeviceContext context = new DeviceContext(BASE_TIME);

        device.handleInboundDeviceCommand(ackConfigCommand, context);

        assertThat(device.getDeviceVitals().getState())
                .as("Device should transition to ALIVE")
                .isEqualTo(DeviceState.ALIVE);
        assertThat(device.getDeviceVitals().getDeviceConfig())
                .as("Device config should be updated to acknowledged values")
                .isEqualTo(new DeviceConfig(Duration.ofSeconds(45), Duration.ofSeconds(15)));
        assertThat(device.getDeviceVitals().getLastCommandReceiveTime())
                .as("Last command receive time should be updated")
                .isEqualTo(BASE_TIME);

        List<HydrogardenDomainEvent> events = device.getDomainEvents();
        assertThat(events.stream().anyMatch(e -> e instanceof RegisteredDeviceStartDE))
                .as("RegisteredDeviceStartDE should be raised on transition")
                .isTrue();
    }

    @Test
    @DisplayName("AckConfigCommand on State 3 (ALIVE/ALIVE) with config difference: synchronizes config")
    void handleInboundDeviceCommand_AckConfigCommand_State3_AliveAlive_UpdatesConfig() {
        Device device = DeviceTestCases.createState3_AliveAlive_ConfigDiffers();
        DeviceConfig previousConfig = device.getDeviceVitals().getDeviceConfig();
        AckConfigCommand ackConfigCommand = new AckConfigCommand(
                TEST_DEVICE_ID,
                Duration.ofSeconds(45),
                Duration.ofSeconds(15)
        );
        DeviceContext context = new DeviceContext(BASE_TIME);

        device.handleInboundDeviceCommand(ackConfigCommand, context);

        assertThat(device.getDeviceVitals().getState())
                .as("Device should remain ALIVE")
                .isEqualTo(DeviceState.ALIVE);
        assertThat(device.getDeviceVitals().getDeviceConfig())
                .as("Device config should be updated to new values")
                .isEqualTo(new DeviceConfig(Duration.ofSeconds(45), Duration.ofSeconds(15)))
                .isNotEqualTo(previousConfig);
    }

    @Test
    @DisplayName("RequestConfigCommand on State 1 (DEAD/DEAD): device transitions to ALIVE and resets config to defaults")
    void handleInboundDeviceCommand_RequestConfigCommand_State1_DeadDead_ResetsConfig() {
        Device device = DeviceTestCases.createState1_DeadDead_ConfigDiffers();
        RequestConfigCommand requestConfigCommand = new RequestConfigCommand(TEST_DEVICE_ID);
        DeviceContext context = new DeviceContext(BASE_TIME);

        device.handleInboundDeviceCommand(requestConfigCommand, context);

        assertThat(device.getDeviceVitals().getState())
                .as("Device should transition to ALIVE")
                .isEqualTo(DeviceState.ALIVE);
        assertThat(device.getDeviceVitals().getDeviceConfig())
                .as("Device config should be reset to default values")
                .isEqualTo(new DeviceConfig());
        assertThat(device.getDeviceVitals().getLastCommandReceiveTime())
                .as("Last command receive time should be updated")
                .isEqualTo(BASE_TIME);
    }

    @Test
    @DisplayName("RequestConfigCommand on State 3 (ALIVE/ALIVE): resets config to defaults without state change")
    void handleInboundDeviceCommand_RequestConfigCommand_State3_AliveAlive_ResetsConfig() {
        Device device = DeviceTestCases.createState3_AliveAlive_ConfigDiffers();
        RequestConfigCommand requestConfigCommand = new RequestConfigCommand(TEST_DEVICE_ID);
        DeviceContext context = new DeviceContext(BASE_TIME);

        device.handleInboundDeviceCommand(requestConfigCommand, context);

        assertThat(device.getDeviceVitals().getState())
                .as("Device should remain ALIVE")
                .isEqualTo(DeviceState.ALIVE);
        assertThat(device.getDeviceVitals().getDeviceConfig())
                .as("Device config should be reset to default DeviceConfig()")
                .isEqualTo(new DeviceConfig());

        List<HydrogardenDomainEvent> events = device.getDomainEvents();
        assertThat(events.stream().noneMatch(e -> e instanceof RegisteredDeviceStartDE))
                .as("No state change event should be raised when device was already ALIVE")
                .isTrue();
    }

    @Test
    @DisplayName("enable() on State 1 (DEAD/DEAD): sets desired state to ALIVE and returns KeepAliveCommand")
    void enable_State1_DeadDead() {
        Device device = DeviceTestCases.createState1_DeadDead();

        OutboundDeviceCommand command = device.enable();

        assertThat(device.getDeviceVitals().getDesiredState())
                .as("Desired state should be set to ALIVE")
                .isEqualTo(DeviceState.ALIVE);
        assertThat(device.getDeviceVitals().getState())
                .as("Actual state should remain DEAD until heartbeat received")
                .isEqualTo(DeviceState.DEAD);
        assertThat(command)
                .as("enable() should return KeepAliveCommand")
                .isInstanceOf(ConfigCommand.class);
        assertThat((command).getDeviceId())
                .as("ConfigCommand should contain correct device ID")
                .isEqualTo(TEST_DEVICE_ID);
    }

    @Test
    @DisplayName("enable() on State 2 (DEAD/ALIVE): desired state remains ALIVE, idempotent operation")
    void enable_State2_DeadAlive() {
        Device device = DeviceTestCases.createState2_DeadAlive();

        OutboundDeviceCommand command = device.enable();

        assertThat(device.getDeviceVitals().getDesiredState())
                .as("Desired state should remain ALIVE (idempotent)")
                .isEqualTo(DeviceState.ALIVE);
        assertThat(command)
                .as("No commands should be returned")
                .isNull();
    }

    @Test
    @DisplayName("enable() on State 3 (ALIVE/ALIVE): desired state remains ALIVE, idempotent operation")
    void enable_State3_AliveAlive() {
        Device device = DeviceTestCases.createState3_AliveAlive();

        OutboundDeviceCommand command = device.enable();

        assertThat(device.getDeviceVitals().getDesiredState())
                .as("Desired state should remain ALIVE (idempotent)")
                .isEqualTo(DeviceState.ALIVE);
        assertThat(device.getDeviceVitals().getState())
                .as("Actual state should remain ALIVE")
                .isEqualTo(DeviceState.ALIVE);
    }

    @Test
    @DisplayName("enable() on State 4 (ALIVE/DEAD): changes desired state from DEAD to ALIVE")
    void enable_State4_AliveDead() {
        Device device = DeviceTestCases.createState4_AliveDead();

        OutboundDeviceCommand command = device.enable();

        assertThat(device.getDeviceVitals().getDesiredState())
                .as("Desired state should change from DEAD to ALIVE")
                .isEqualTo(DeviceState.ALIVE);
        assertThat(device.getDeviceVitals().getState())
                .as("Actual state should remain ALIVE")
                .isEqualTo(DeviceState.ALIVE);
        assertThat(command)
                .as("ConfigCommand should be returned")
                .isInstanceOf(ConfigCommand.class);
    }

    @Test
    @DisplayName("disable() on State 1 (DEAD/DEAD): desired state remains DEAD and all circuits set to OFF")
    void disable_State1_DeadDead() {
        Device device = DeviceTestCases.createState1_DeadDead();

        OutboundDeviceCommand command = device.disable();

        assertThat(device.getDeviceVitals().getDesiredState())
                .as("Desired state should remain DEAD (idempotent)")
                .isEqualTo(DeviceState.DEAD);
        assertThat(command)
                .as("disable() should return NewStateCommand")
                .isInstanceOf(NewStateCommand.class);

        NewStateCommand newStateCommand = (NewStateCommand) command;
        assertThat(newStateCommand.getStates())
                .as("All circuit states in command should be OFF")
                .allMatch(state -> state.equals(new CircuitState(false)));
    }

    @Test
    @DisplayName("disable() on State 2 (DEAD/ALIVE): changes desired state from ALIVE to DEAD")
    void disable_State2_DeadAlive() {
        Device device = DeviceTestCases.createState2_DeadAlive();

        OutboundDeviceCommand command = device.disable();

        assertThat(device.getDeviceVitals().getDesiredState())
                .as("Desired state should change from ALIVE to DEAD")
                .isEqualTo(DeviceState.DEAD);
        assertThat(device.getDeviceVitals().getState())
                .as("Actual state should remain DEAD")
                .isEqualTo(DeviceState.DEAD);
        assertThat(command)
                .as("NewStateCommand with all circuits OFF should be returned")
                .isInstanceOf(NewStateCommand.class);
    }

    @Test
    @DisplayName("disable() on State 3 (ALIVE/ALIVE): changes desired state to DEAD and sets all circuits to OFF")
    void disable_State3_AliveAlive() {
        Device device = DeviceTestCases.createState3_AliveAlive();

        OutboundDeviceCommand command = device.disable();

        assertThat(device.getDeviceVitals().getDesiredState())
                .as("Desired state should change from ALIVE to DEAD")
                .isEqualTo(DeviceState.DEAD);
        assertThat(device.getDeviceVitals().getState())
                .as("Actual state should remain ALIVE until timeout")
                .isEqualTo(DeviceState.ALIVE);

        NewStateCommand newStateCommand = (NewStateCommand) command;
        assertThat(newStateCommand.getStates())
                .as("All 8 circuits should be set to OFF in the command")
                .hasSize(8)
                .allMatch(state -> state.equals(new CircuitState(false)));
    }

    @Test
    @DisplayName("disable() on State 4 (ALIVE/DEAD): desired state remains DEAD, idempotent operation")
    void disable_State4_AliveDead() {
        Device device = DeviceTestCases.createState4_AliveDead();

        OutboundDeviceCommand command = device.disable();

        assertThat(device.getDeviceVitals().getDesiredState())
                .as("Desired state should remain DEAD (idempotent)")
                .isEqualTo(DeviceState.DEAD);
        assertThat(command)
                .as("NewStateCommand should still be returned")
                .isInstanceOf(NewStateCommand.class);
    }

    @Test
    @DisplayName("requestChangeCircuitState() on State 1 (DEAD/DEAD): throws IllegalStateException")
    void requestChangeCircuitState_State1_DeadDead_ThrowsException() {
        Device device = DeviceTestCases.createState1_DeadDead();
        CircuitId circuitId = new CircuitId((short) 1);

        assertThatThrownBy(() -> device.requestChangeCircuitState(circuitId, new CircuitState(true)))
                .as("Cannot change circuit state of dead device")
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot change circuit state of a dead device");
    }

    @Test
    @DisplayName("requestChangeCircuitState() on State 2 (DEAD/ALIVE): throws IllegalStateException")
    void requestChangeCircuitState_State2_DeadAlive_ThrowsException() {
        Device device = DeviceTestCases.createState2_DeadAlive();
        CircuitId circuitId = new CircuitId((short) 1);

        assertThatThrownBy(() -> device.requestChangeCircuitState(circuitId, new CircuitState(true)))
                .as("Cannot change circuit state when actual state is DEAD even if desired is ALIVE")
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot change circuit state of a dead device");
    }

    @Test
    @DisplayName("requestChangeCircuitState() on State 3 (ALIVE/ALIVE): updates circuit desired state and returns NewStateCommand")
    void requestChangeCircuitState_State3_AliveAlive_Success() {
        Device device = DeviceTestCases.createState3_AliveAlive();
        CircuitId circuitId = new CircuitId((short) 1);
        CircuitState previousDesiredState = device.getCircuits().getFirst().getDesiredState();

        OutboundDeviceCommand command = device.requestChangeCircuitState(circuitId, new CircuitState(true));

        assertThat(device.getCircuits().getFirst().getDesiredState())
                .as("First circuit desired state should be updated to ON")
                .isEqualTo(new CircuitState(true))
                .isNotEqualTo(previousDesiredState);
        assertThat(device.getCircuits().getFirst().getState())
                .as("First circuit actual state should remain unchanged until ACK")
                .isEqualTo(new CircuitState(false));
        assertThat(device.getCircuits().getFirst().isUnsynchronised())
                .as("First circuit should now be unsynchronized")
                .isTrue();

        assertThat(command)
                .as("requestChangeCircuitState should return NewStateCommand")
                .isInstanceOf(NewStateCommand.class);

        NewStateCommand newStateCommand = (NewStateCommand) command;
        assertThat(newStateCommand.getStates())
                .as("NewStateCommand should contain all 8 circuit desired states")
                .hasSize(8);
        assertThat(newStateCommand.getStates().getFirst())
                .as("First circuit in command should be ON")
                .isEqualTo(new CircuitState(true));
    }

    @Test
    @DisplayName("requestChangeCircuitState() on State 3 (ALIVE/ALIVE) with invalid circuit ID: throws IllegalArgumentException")
    void requestChangeCircuitState_State3_AliveAlive_InvalidCircuitId() {
        Device device = DeviceTestCases.createState3_AliveAlive();
        CircuitId invalidCircuitId = new CircuitId((short) 99);

        assertThatThrownBy(() -> device.requestChangeCircuitState(invalidCircuitId, new CircuitState(true)))
                .as("Requesting change for non-existent circuit should throw exception")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Circuit 99 not found");
    }

    @Test
    @DisplayName("requestChangeCircuitState() on State 4 (ALIVE/DEAD): succeeds because actual state is ALIVE")
    void requestChangeCircuitState_State4_AliveDead_Success() {
        Device device = DeviceTestCases.createState4_AliveDead();
        CircuitId circuitId = new CircuitId((short) 1);

        OutboundDeviceCommand command = device.requestChangeCircuitState(circuitId, new CircuitState(true));

        assertThat(device.getCircuits().getFirst().getDesiredState())
                .as("Circuit desired state should be updated even though device desired state is DEAD")
                .isEqualTo(new CircuitState(true));
        assertThat(command)
                .as("NewStateCommand should be returned")
                .isInstanceOf(NewStateCommand.class);
    }

    @Test
    @DisplayName("rename() on State 1 (DEAD/DEAD): updates device name without affecting state")
    void rename_State1_DeadDead() {
        Device device = DeviceTestCases.createState1_DeadDead();
        String newName = "New Device Name";

        device.rename(newName);

        assertThat(device.getName())
                .as("Device name should be updated")
                .isEqualTo(newName);
        assertThat(device.getDeviceVitals().getState())
                .as("Device state should remain DEAD")
                .isEqualTo(DeviceState.DEAD);
        assertThat(device.getDomainEvents())
                .as("No domain events should be raised for rename operation")
                .isEmpty();
    }

    @Test
    @DisplayName("rename() with null name: throws IllegalArgumentException")
    void rename_NullName_ThrowsException() {
        Device device = DeviceTestCases.createState3_AliveAlive();

        assertThatThrownBy(() -> device.rename(null))
                .as("Renaming device to null should throw exception")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Device name cannot be null");
    }

    @Test
    @DisplayName("Device transitions from ALIVE to DEAD when lastCommandReceiveTime exceeds standbyTimeout")
    void checkIfAliveOrDead_AliveDevice_BecomesDeadOnTimeout() {
        Device device = DeviceTestCases.createState3_AliveAlive();
        LocalDateTime futureTime = BASE_TIME.plus(Duration.ofSeconds(60));
        DeviceContext context = new DeviceContext(futureTime);

        device.evaluateCurrentStateAndCommand(context);

        assertThat(device.getDeviceVitals().getState())
                .as("Device should transition to DEAD when last command time exceeds standby timeout")
                .isEqualTo(DeviceState.DEAD);

        List<HydrogardenDomainEvent> events = device.getDomainEvents();
        assertThat(events)
                .as("RegisteredDeviceShutdownDE event should be raised")
                .hasSize(1);
        assertThat(events.getFirst())
                .as("Event should be RegisteredDeviceShutdownDE")
                .isInstanceOf(RegisteredDeviceShutdownDE.class);

        RegisteredDeviceShutdownDE event = (RegisteredDeviceShutdownDE) events.getFirst();
        assertThat(event.getDeviceId())
                .as("Event should contain correct device ID")
                .isEqualTo(TEST_DEVICE_ID);
        assertThat(event.getTimestamp())
                .as("Event timestamp should match evaluation time")
                .isEqualTo(futureTime);
    }

    @Test
    @DisplayName("Device with null lastCommandReceiveTime does not transition state")
    void checkIfAliveOrDead_NullLastCommandTime_NoStateChange() {
        Device device = DeviceTestCases.createState2_DeadAlive();
        DeviceContext context = new DeviceContext(BASE_TIME);

        device.evaluateCurrentStateAndCommand(context);

        assertThat(device.getDeviceVitals().getLastCommandReceiveTime())
                .as("Last command receive time should remain null")
                .isNull();
        assertThat(device.getDeviceVitals().getState())
                .as("Device state should remain DEAD when lastCommandReceiveTime is null")
                .isEqualTo(DeviceState.DEAD);
    }

    @Test
    @DisplayName("getCircuits() returns unmodifiable list to protect aggregate consistency")
    void getCircuits_ReturnsUnmodifiableList() {
        Device device = DeviceTestCases.createState3_AliveAlive();

        List<Circuit> circuits = device.getCircuits();

        assertThat(circuits)
                .as("getCircuits should return list with 8 circuits")
                .hasSize(8);

        assertThatThrownBy(() -> circuits.add(new Circuit()))
                .as("Returned list should be unmodifiable to protect aggregate")
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
