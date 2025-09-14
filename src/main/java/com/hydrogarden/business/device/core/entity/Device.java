package com.hydrogarden.business.device.core.entity;

import com.hydrogarden.business.device.core.commands.*;
import com.hydrogarden.business.device.core.event.KeepaliveSentDE;
import com.hydrogarden.business.device.core.event.RegisteredDeviceStartDE;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.util.Assert;

import java.io.File;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Aggregate root representing a Device in the system.
 * Handles device state, command requests, acknowledgements, and heartbeat evaluation.
 */
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "device")
@NoArgsConstructor
@AllArgsConstructor
public class Device extends HydrogardenAgreggateRoot implements DeviceCommandVisitor {

    // ------------------- Getters -------------------
    /**
     * Unique identifier of the device (aggregate ID)
     */
    @Getter
    @EmbeddedId
    private DeviceId id;

    /**
     * Human-readable device name
     */
    @Getter
    @Column(nullable = false)
    private String name;

    /**
     * Circuits associated with this device.
     */
    @OneToMany(mappedBy = "device", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Circuit> circuits;

    @Column(nullable = false)
    @Getter
    @Setter
    private Duration keepaliveInterval;

    @Column(nullable = false)
    @Getter
    @Setter
    private Duration configInterval;

    @Getter
    @Column
    private Duration newStateInterval;

    @Getter
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "id", referencedColumnName = "id")
    private DeviceVitals deviceVitals;


    // ------------------- Domain Behavior -------------------

    public DeviceCommand requestChangeCircuitState(CircuitId circuitId, CircuitState newState) {
        if (this.deviceVitals.getState() != DeviceState.ALIVE) {
            throw new IllegalStateException("Cannot change circuit state of a dead device");
        }
        Optional<Circuit> circuitOptional = this.circuits.stream().filter(c -> c.getId().equals(circuitId)).findFirst();

        if (circuitOptional.isEmpty()) {
            throw new IllegalArgumentException("Circuit %s not found".formatted(circuitId.getId()));
        }
        Circuit circuit = circuitOptional.get();

        circuit.setDesiredState(newState);

        return new NewStateCommand(id, this.circuits.stream().map(Circuit::getDesiredState).toList());
    }

    private void acknowledgeAckStateReceived(List<CircuitState> circuitStates, DeviceContext now) {
        checkIfDeviceStarted(now.now());

        for (int i = 0; i < this.circuits.size(); i++) {
            circuits.get(i).setState(circuitStates.get(i));
        }
    }

    private void checkIfDeviceStarted(LocalDateTime now) {

        if(this.deviceVitals.getState() == DeviceState.DEAD) {
            this.deviceVitals.setState(DeviceState.ALIVE);
            this.registerDomainEvent(new RegisteredDeviceStartDE(this.id, now));
        }

        this.deviceVitals.setLastCommandReceiveTime(now);

    }

    /**
     * Acknowledge that a heartbeat signal has been received.
     *
     * @param now current time
     */
    public void acknowledgeHeartbeatReceived(LocalDateTime now) {
        checkIfDeviceStarted(now);
    }

    /**
     * @param ackConfigCommand
     */
    private void acknowledgeAckConfigReceived(AckConfigCommand ackConfigCommand, DeviceContext deviceContext) {
        checkIfDeviceStarted(deviceContext.now());
        this.deviceVitals.setDeviceConfig(new DeviceConfig(ackConfigCommand.getStandbyTimeout(), ackConfigCommand.getHeartbeatInterval()));
    }

    private void acknowledgeRequestConfigReceived(RequestConfigCommand requestConfigCommand, DeviceContext deviceContext) {
        checkIfDeviceStarted(deviceContext.now());
        this.deviceVitals.setDeviceConfig(new DeviceConfig());
    }

    /**
     * Evaluates current state and optionally returns a DeviceCommand.
     *
     * @return optional DeviceCommand if an action is needed
     */
    public List<DeviceCommand> evaluateCurrentStateAndCommand(DeviceContext deviceContext) {
        this.evaluateState(deviceContext);
        List<DeviceCommand> commands = evaluateCommands(deviceContext);

        return commands;
    }

    private List<DeviceCommand> evaluateCommands(DeviceContext deviceContext) {
        List<DeviceCommand> commands = new ArrayList<>();

        if (this.shouldSendConfig(deviceContext)) {
            DeviceCommand command = this.requestSendingConfig(deviceContext);
            commands.add(command);
            return commands;
        }

        if (this.shouldSendKeepalive(deviceContext)) {
            DeviceCommand e = this.requestKeepAlive(deviceContext.now());
            commands.add(e);
        }

        if (this.shouldSendNewState(deviceContext)) {
            DeviceCommand command = this.requestSendingNewState(deviceContext.now());
            commands.add(command);
        }

        return commands;
    }

    private DeviceCommand requestSendingConfig(DeviceContext deviceContext) {
        return new ConfigCommand(this.id, this.deviceVitals.getDesiredDeviceConfig().getStandbyTimeout(), this.deviceVitals.getDesiredDeviceConfig().getHeartbeatInterval());
    }

    private boolean shouldSendConfig(DeviceContext deviceContext) {
        boolean timeForConfigCommand = this.deviceVitals.getLastConfigSendTime() == null || Duration.between(this.deviceVitals.getLastConfigSendTime(), deviceContext.now()).compareTo(this.configInterval) > 0;

        return this.deviceVitals.getDesiredState() == DeviceState.ALIVE && timeForConfigCommand && !Objects.equals(this.deviceVitals.getDeviceConfig(), this.deviceVitals.getDesiredDeviceConfig());
    }

    private boolean shouldSendNewState(DeviceContext deviceContext) {
        boolean timeForNewStateCommand = this.deviceVitals.getLastNewStateSendTime() == null || Duration.between(this.deviceVitals.getLastNewStateSendTime(), deviceContext.now()).compareTo(this.newStateInterval) > 0;
        return timeForNewStateCommand && this.circuits.stream().anyMatch(Circuit::isUnsynchronised) && this.deviceVitals.getState() == DeviceState.ALIVE && this.deviceVitals.getDesiredState() == DeviceState.ALIVE;
    }

    private DeviceCommand requestSendingNewState(LocalDateTime now) {

        return new NewStateCommand(id, this.circuits.stream().map(Circuit::getDesiredState).toList());
    }

    /**
     * Rename the device.
     *
     * @param name new name for the device
     */
    public void rename(String name) {
        Assert.notNull(name, "Device name cannot be null");
        this.name = name;
    }

    /**
     * Enables the device by setting it's desired state to ALIVE.
     *
     * @return Enable command
     */
    public DeviceCommand enable() {

        this.deviceVitals.setDesiredState(DeviceState.ALIVE);

        return new StartCommand(this.id);
    }

    /**
     * Disables the device by setting it's desired state to DEAD.
     *
     * @return
     */
    public DeviceCommand disable() {
        this.deviceVitals.setDesiredState(DeviceState.DEAD);
        return new NewStateCommand(id, this.circuits.stream().map(c -> new CircuitState(false)).toList());
    }

    /**
     * Checks if the device should send a KeepAlive command.
     *
     * @return true if a keepalive should be sent
     */
    private boolean shouldSendKeepalive(DeviceContext deviceContext) {
        boolean timeForKeepaliveCommand = this.deviceVitals.getLastKeepAliveSendTime() == null || Duration.between(this.deviceVitals.getLastKeepAliveSendTime(), deviceContext.now()).compareTo(this.keepaliveInterval) > 0;

        return this.deviceVitals.getState() == DeviceState.ALIVE && this.deviceVitals.getDesiredState() == DeviceState.ALIVE && timeForKeepaliveCommand;
    }

    /**
     * Requests a KeepAlive command for this device.
     *
     * @param now current time
     * @return the KeepAlive DeviceCommand
     */
    private DeviceCommand requestKeepAlive(LocalDateTime now) {
        this.deviceVitals.setLastKeepAliveSendTime(now);
        this.registerDomainEvent(new KeepaliveSentDE(this.id, now));
        return new KeepAliveCommand(this.id);
    }

    /**
     * Evaluates device state based on last heartbeat and timing rules.
     */
    private void evaluateState(DeviceContext deviceContext) {
        if (this.getDeviceVitals().getLastCommandReceiveTime() == null) return;

        Duration timeSinceLastHeartbeat = Duration.between(this.getDeviceVitals().getLastCommandReceiveTime(), deviceContext.now());
        boolean heartbeatReceivedOnTime = timeSinceLastHeartbeat.compareTo(this.deviceVitals.getDeviceConfig().getStandbyTimeout()) < 0;

        if (!heartbeatReceivedOnTime) {
            this.deviceVitals.setState(DeviceState.DEAD);
        } else {
            this.deviceVitals.setState(DeviceState.ALIVE);
        }

    }

    public List<Circuit> getCircuits() {
        return Collections.unmodifiableList(circuits);
    }

    public List<DeviceCommand> handleDeviceCommand(DeviceCommand deviceCommand, DeviceContext deviceContext) {
        deviceCommand.accept(this, deviceContext);
        return this.evaluateCurrentStateAndCommand(deviceContext);
    }


    private IllegalStateException exceptionForUnhandledCommandType(DeviceCommand cmd) {
        return new IllegalStateException("Device does not handle a command of type %s".formatted(cmd.getCommandType()));
    }

    @Override
    public void visit(HeartbeatCommand cmd, DeviceContext now) {
        this.acknowledgeHeartbeatReceived(now.now());
    }

    @Override
    public void visit(AckStateCommand cmd, DeviceContext now) {
        this.acknowledgeAckStateReceived(cmd.getStates(),now);
    }

    @Override
    public void visit(ConfigCommand configCommand, DeviceContext deviceContext) {
        throw this.exceptionForUnhandledCommandType(configCommand);
    }

    @Override
    public void visit(AckConfigCommand ackConfigCommand, DeviceContext deviceContext) {
        this.acknowledgeAckConfigReceived(ackConfigCommand, deviceContext);
    }

    @Override
    public void visit(RequestConfigCommand requestConfigCommand, DeviceContext deviceContext) {
        this.acknowledgeRequestConfigReceived(requestConfigCommand, deviceContext);
    }



    @Override
    public void visit(KeepAliveCommand cmd, DeviceContext now) {
        throw exceptionForUnhandledCommandType(cmd);
    }

    @Override
    public void visit(StartCommand cmd, DeviceContext now) {
        throw exceptionForUnhandledCommandType(cmd);
    }

    @Override
    public void visit(NewStateCommand cmd, DeviceContext now) {
        throw exceptionForUnhandledCommandType(cmd);
    }
}
