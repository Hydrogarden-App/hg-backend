package com.hydrogarden.business.device.app.service;

import com.hydrogarden.business.device.core.commands.DeviceCommand;
import com.hydrogarden.business.device.core.commands.DeviceContext;
import com.hydrogarden.business.device.core.entity.*;
import com.hydrogarden.business.device.core.port.out.DeviceOutputPort;
import com.hydrogarden.business.device.infra.repository.DeviceRepository;
import com.hydrogarden.common.HydrogardenEventPublisher;
import com.hydrogarden.common.HydrogardenTimeProvider;
import com.hydrogarden.common.ServiceUtils;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * Application Service for orchestrating Device use cases.
 * Handles command evaluation, sending commands, and acknowledging execution.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DeviceApplicationService {

    private final DeviceRepository deviceRepository;
    private final DeviceOutputPort deviceOutputPort;
    private final HydrogardenEventPublisher hydrogardenEventPublisher;
    private final TransactionTemplate transactionTemplate;
    private final HydrogardenTimeProvider hydrogardenTimeProvider;
    private final EntityManager em;

    /**
     * Periodically evaluate all devices, send commands if necessary,
     * and acknowledge execution.
     */
    public void runDeviceRoutine() {
        log.debug("Device routine started");
        List<Device> devices = deviceRepository.findAll();

        for (Device device : devices) {
            log.debug("Running device routine for device: id={}", device.getId());
            evaluateAndExecute(device.getId());
        }
        log.debug("Device routine finished.");
    }

    /**
     * Evaluates a device, sends the command if required, and acknowledges it.
     *
     * @param deviceId the device to evaluate
     */
    private void evaluateAndExecute(DeviceId deviceId) {

        List<DeviceCommand> commands = transactionTemplate.execute(status -> {
            Device device = deviceRepository.findById(deviceId).orElseThrow();
            device = deviceRepository.save(device);
            return device.evaluateCurrentStateAndCommand(this.getDeviceContext());
        });

        sendDeviceCommands(commands, deviceId);

    }

    /**
     * Rename a device.
     *
     * @param deviceId device ID
     * @param newName  new device name
     */
    @Transactional
    public Device renameDevice(DeviceId deviceId, String newName) {
        Device device = deviceRepository.findById(deviceId).orElseThrow(() -> new IllegalArgumentException("Device not found: " + deviceId));
        device.rename(newName);
        deviceRepository.save(device);

        List<HydrogardenDomainEvent> domainEvents = device.getDomainEvents(); //DeviceRenamedDomainEvent

        ServiceUtils.runAfterCommit(() -> {
            hydrogardenEventPublisher.publish(domainEvents);
            device.clearDomainEvents();
        });

        return device;

    }

    @Transactional
    public Device getDevice(DeviceId deviceId) {
        return deviceRepository.findById(deviceId).orElseThrow(() -> new IllegalArgumentException("Device not found: " + deviceId));
    }

    @Transactional
    public Device enableDevice(DeviceId deviceId) {
        Device device = deviceRepository.findById(deviceId).orElseThrow(() -> new IllegalArgumentException("Device not found: " + deviceId));
        DeviceCommand enableCommand = device.enable();

        ServiceUtils.runAfterCommit(() -> {
            deviceOutputPort.sendDeviceCommand(enableCommand);
        });

        device = deviceRepository.save(device);
        return device;
    }

    @Transactional
    public Device disableDevice(DeviceId deviceId) {
        Device device = deviceRepository.findById(deviceId).orElseThrow(() -> new IllegalArgumentException("Device not found: " + deviceId));
        DeviceCommand newStateCommand = device.disable();

        ServiceUtils.runAfterCommit(() -> {
            deviceOutputPort.sendDeviceCommand(newStateCommand);
        });

        device = deviceRepository.save(device);
        return device;
    }

    @Transactional
    public Device requestChangeCircuitStatus(DeviceId deviceId, CircuitId circuitId, CircuitState circuitState) {
        Device device = deviceRepository.findById(deviceId).orElseThrow();
        DeviceCommand newStatusCommand = device.requestChangeCircuitState(circuitId, circuitState);

        ServiceUtils.runAfterCommit(() -> {
            deviceOutputPort.sendDeviceCommand(newStatusCommand);
        });

        device = deviceRepository.save(device);
        return device;
    }

    @Transactional
    public void handleDeviceCommand(DeviceCommand deviceCommand) {
        Device device = deviceRepository.findById(deviceCommand.getDeviceId()).orElseThrow();


        List<DeviceCommand> commands = device.handleDeviceCommand(deviceCommand, this.getDeviceContext());
        sendDeviceCommands(commands, deviceCommand.getDeviceId());
    }

    private void sendDeviceCommands(List<DeviceCommand> commands, DeviceId deviceId) {
        for (DeviceCommand command : commands) {
            try {
                deviceOutputPort.sendDeviceCommand(command);
            } catch (Exception e) {
                log.error("Failed to send command {} to device {}", command, deviceId, e);
            }
        }
    }

    private DeviceContext getDeviceContext() {
        return new DeviceContext(hydrogardenTimeProvider.getCurrentTime());
    }
}
