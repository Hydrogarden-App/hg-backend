package com.hydrogarden.business.device.core.entity;

import com.hydrogarden.business.device.core.commands.*;
import com.hydrogarden.business.device.core.event.KeepaliveSentDE;
import com.hydrogarden.business.device.core.event.RegisteredDeviceShutdownDE;
import com.hydrogarden.business.device.core.event.RegisteredDeviceStartDE;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.util.Assert;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Aggregate root representing a Device in the system.
 * Handles device state, command requests, acknowledgements, and heartbeat evaluation.
 */
@Entity
@Table(name = "device")
@NoArgsConstructor
@AllArgsConstructor
public class Device extends HydrogardenAgreggateRoot {

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

    public OutboundDeviceCommand requestChangeCircuitState(CircuitId circuitId, CircuitState newState) {
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
        for (int i = 0; i < this.circuits.size(); i++) {
            circuits.get(i).setState(circuitStates.get(i));
        }
    }

    /**
     * @param ackConfigCommand
     */
    private void acknowledgeAckConfigReceived(AckConfigCommand ackConfigCommand, DeviceContext deviceContext) {
        this.deviceVitals.setDeviceConfig(new DeviceConfig(ackConfigCommand.getStandbyTimeout(), ackConfigCommand.getHeartbeatInterval()));
    }

    private void acknowledgeRequestConfigReceived(RequestConfigCommand requestConfigCommand, DeviceContext deviceContext) {
        this.deviceVitals.setDeviceConfig(new DeviceConfig());
    }

    /**
     * Evaluates current state and optionally returns a DeviceCommand.
     *
     * @return optional DeviceCommand if an action is needed
     */
    public List<OutboundDeviceCommand> evaluateCurrentStateAndCommand(DeviceContext deviceContext) {
        this.checkIfAliveOrDead(deviceContext);
        return evaluateCommands(deviceContext);
    }

    private List<OutboundDeviceCommand> evaluateCommands(DeviceContext deviceContext) {
        List<OutboundDeviceCommand> commands = new ArrayList<>();

        if (this.shouldSendConfig(deviceContext)) {
            OutboundDeviceCommand command = this.requestSendingConfig(deviceContext);
            commands.add(command);
            return commands;
        }

        if (this.shouldSendKeepalive(deviceContext)) {
            OutboundDeviceCommand command = this.requestKeepAlive(deviceContext.now());
            commands.add(command);
        }

        if (this.shouldSendNewState(deviceContext)) {
            OutboundDeviceCommand command = this.requestSendingNewState(deviceContext.now());
            commands.add(command);
        }

        return commands;
    }

    private OutboundDeviceCommand requestSendingConfig(DeviceContext deviceContext) {
        return new ConfigCommand(this.id, this.deviceVitals.getDesiredDeviceConfig().getStandbyTimeout(), this.deviceVitals.getDesiredDeviceConfig().getHeartbeatInterval());
    }

    private boolean shouldSendConfig(DeviceContext deviceContext) {
        boolean timeForConfigCommand = hasIntervalElapsed(
            this.deviceVitals.getLastConfigSendTime(),
            this.configInterval,
            deviceContext.now()
        );

        return this.deviceVitals.getDesiredState() == DeviceState.ALIVE &&
               timeForConfigCommand &&
               hasUnsynchronizedConfig();
    }

    private boolean shouldSendNewState(DeviceContext deviceContext) {
        boolean timeForNewStateCommand = hasIntervalElapsed(
            this.deviceVitals.getLastNewStateSendTime(),
            this.newStateInterval,
            deviceContext.now()
        );
        return timeForNewStateCommand &&
               hasUnsynchronizedCircuits() &&
               isFullyAlive();
    }

    private OutboundDeviceCommand requestSendingNewState(LocalDateTime now) {

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
    public OutboundDeviceCommand enable() {
        return this.updateDesiredState(DeviceState.ALIVE);
    }

    private OutboundDeviceCommand updateDesiredState(DeviceState newDesiredState){
        DeviceState currentDesiredState = this.deviceVitals.getDesiredState();

        if(currentDesiredState != newDesiredState){
            this.deviceVitals.setDesiredState(newDesiredState);
            if(newDesiredState == DeviceState.ALIVE){
                return new ConfigCommand(this.id, this.deviceVitals.getDesiredDeviceConfig().getStandbyTimeout(), this.deviceVitals.getDesiredDeviceConfig().getHeartbeatInterval());

            }
        }

        return null;

    }

    /**
     * Disables the device by setting it's desired state to DEAD.
     *
     * @return
     */
    public OutboundDeviceCommand disable() {
        this.updateDesiredState(DeviceState.DEAD);
        return new NewStateCommand(id, this.circuits.stream().map(c -> new CircuitState(false)).toList());
    }

    /**
     * Checks if the device should send a KeepAlive command.
     *
     * @return true if a keepalive should be sent
     */
    private boolean shouldSendKeepalive(DeviceContext deviceContext) {
        boolean timeForKeepaliveCommand = hasIntervalElapsed(
            this.deviceVitals.getLastKeepAliveSendTime(),
            this.keepaliveInterval,
            deviceContext.now()
        );

        return isFullyAlive() && timeForKeepaliveCommand;
    }

    /**
     * Requests a KeepAlive command for this device.
     *
     * @param now current time
     * @return the KeepAlive DeviceCommand
     */
    private OutboundDeviceCommand requestKeepAlive(LocalDateTime now) {
        this.deviceVitals.setLastKeepAliveSendTime(now);
        this.registerDomainEvent(new KeepaliveSentDE(this.id, now));
        return new KeepAliveCommand(this.id);
    }

    /**
     * Evaluates device state based on last heartbeat and timing rules.
     */
    private void checkIfAliveOrDead(DeviceContext deviceContext) {
        DeviceState targetState = isHeartbeatReceivedOnTime(deviceContext)
            ? DeviceState.ALIVE
            : DeviceState.DEAD;

        updateDeviceState(targetState, deviceContext);
    }

    public List<Circuit> getCircuits() {
        return Collections.unmodifiableList(circuits);
    }

    /**
     * Checks if the specified interval has elapsed since the last send time.
     * Returns true if lastSendTime is null (never sent) or interval has elapsed.
     */
    private boolean hasIntervalElapsed(LocalDateTime lastSendTime, Duration interval, LocalDateTime now) {
        return lastSendTime == null ||
               Duration.between(lastSendTime, now).compareTo(interval) > 0;
    }

    /**
     * Checks if device is fully alive (both actual and desired state are ALIVE).
     */
    private boolean isFullyAlive() {
        return this.deviceVitals.getState() == DeviceState.ALIVE &&
               this.deviceVitals.getDesiredState() == DeviceState.ALIVE;
    }

    /**
     * Checks if device has unsynchronized configuration.
     */
    private boolean hasUnsynchronizedConfig() {
        return !Objects.equals(
            this.deviceVitals.getDeviceConfig(),
            this.deviceVitals.getDesiredDeviceConfig()
        );
    }

    /**
     * Checks if any circuit is unsynchronized.
     */
    private boolean hasUnsynchronizedCircuits() {
        return this.circuits.stream().anyMatch(Circuit::isUnsynchronised);
    }

    /**
     * Determines if heartbeat was received on time based on standby timeout.
     */
    private boolean isHeartbeatReceivedOnTime(DeviceContext deviceContext) {
        if (this.deviceVitals.getLastCommandReceiveTime() == null) {
            return false;
        }

        Duration timeSinceLastCommand = Duration.between(
            this.deviceVitals.getLastCommandReceiveTime(),
            deviceContext.now()
        );

        return timeSinceLastCommand.compareTo(
            this.deviceVitals.getDesiredDeviceConfig().getStandbyTimeout()
        ) < 0;
    }

    /**
     * Updates the device state and raises appropriate domain event if state changes.
     */
    private void updateDeviceState(DeviceState newState, DeviceContext deviceContext) {
        DeviceState previousState = this.deviceVitals.getState();

        if (previousState != newState) {
            if (newState == DeviceState.ALIVE) {
                this.registerDomainEvent(new RegisteredDeviceStartDE(this.id, deviceContext.now()));
            } else {
                this.registerDomainEvent(new RegisteredDeviceShutdownDE(this.id, deviceContext.now()));
            }
        }

        this.deviceVitals.setState(newState);
    }

    public List<OutboundDeviceCommand> handleInboundDeviceCommand(InboundDeviceCommand deviceCommand, DeviceContext deviceContext) {
        this.updateVitalsAfterInboundCommandReception(deviceContext);
        switch (deviceCommand){
            case AckConfigCommand c:
                this.acknowledgeAckConfigReceived(c,deviceContext);
                break;
            case AckStateCommand c:
                this.acknowledgeAckStateReceived(c.getStates(),deviceContext);
                break;
            case HeartbeatCommand c:
                break;
            case RequestConfigCommand c:
                this.acknowledgeRequestConfigReceived(c,deviceContext);
                break;
        }
        return this.evaluateCurrentStateAndCommand(deviceContext);
    }

    private void handleInboundDeviceCommandInternal(InboundDeviceCommand deviceCommand, DeviceContext deviceContext) {

    }


    private IllegalStateException exceptionForUnhandledCommandType(DeviceCommand cmd) {
        return new IllegalStateException("Device does not handle a command of type %s".formatted(cmd.getCommandType()));
    }

    private void updateVitalsAfterInboundCommandReception(DeviceContext deviceContext){
        if(this.deviceVitals.getState() == DeviceState.DEAD) {
            updateDeviceState(DeviceState.ALIVE, deviceContext);
        }

        this.deviceVitals.setLastCommandReceiveTime(deviceContext.now());
    }
}
