package com.hydrogarden.business.device.app.service;

import com.hydrogarden.business.device.core.commands.DeviceCommand;
import com.hydrogarden.business.device.core.commands.DeviceContext;
import com.hydrogarden.business.device.core.commands.InboundDeviceCommand;
import com.hydrogarden.business.device.core.commands.OutboundDeviceCommand;
import com.hydrogarden.business.device.core.entity.*;
import com.hydrogarden.business.device.core.port.out.DeviceOutputPort;
import com.hydrogarden.business.device.infra.repository.DeviceRepository;
import com.hydrogarden.common.AuthorizedForDevice;
import com.hydrogarden.common.HydrogardenEventPublisher;
import com.hydrogarden.common.HydrogardenTimeProvider;
import com.hydrogarden.common.ServiceUtils;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Application Service for orchestrating Device use cases.
 * Handles command evaluation, sending commands, and acknowledging execution.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DeviceApplicationServiceImpl implements DeviceApplicationService {

    private final DeviceRepository deviceRepository;
    private final DeviceOutputPort deviceOutputPort;
    private final HydrogardenEventPublisher hydrogardenEventPublisher;
    private final HydrogardenTimeProvider hydrogardenTimeProvider;

    /**
     * Rename a device.
     *
     * @param deviceId device ID
     * @param newName  new device name
     */
    @Override
    @Transactional
    @AuthorizedForDevice
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

    @Override
    @Transactional
    @AuthorizedForDevice
    public Device getDevice(DeviceId deviceId) {
        return deviceRepository.findById(deviceId).orElseThrow(() -> new IllegalArgumentException("Device not found: " + deviceId));
    }

    @Override
    @Transactional
    @AuthorizedForDevice
    public Device enableDevice(DeviceId deviceId) {
        Device device = deviceRepository.findById(deviceId).orElseThrow(() -> new IllegalArgumentException("Device not found: " + deviceId));
        OutboundDeviceCommand enableCommand = device.enable();

        ServiceUtils.runAfterCommit(() -> {
            deviceOutputPort.sendDeviceCommand(enableCommand);
        });

        device = deviceRepository.save(device);
        return device;
    }

    @Override
    @Transactional
    @AuthorizedForDevice
    public Device disableDevice(DeviceId deviceId) {
        Device device = deviceRepository.findById(deviceId).orElseThrow(() -> new IllegalArgumentException("Device not found: " + deviceId));
        OutboundDeviceCommand newStateCommand = device.disable();

        ServiceUtils.runAfterCommit(() -> {
            deviceOutputPort.sendDeviceCommand(newStateCommand);
        });

        device = deviceRepository.save(device);
        return device;
    }

    @Override
    @Transactional
    @AuthorizedForDevice
    public Device requestChangeCircuitStatus(DeviceId deviceId, CircuitId circuitId, CircuitState circuitState) {
        Device device = deviceRepository.findById(deviceId).orElseThrow();
        OutboundDeviceCommand newStatusCommand = device.requestChangeCircuitState(circuitId, circuitState);

        ServiceUtils.runAfterCommit(() -> {
            deviceOutputPort.sendDeviceCommand(newStatusCommand);
        });

        device = deviceRepository.save(device);
        return device;
    }

    @Override
    @Transactional
    public void handleDeviceCommand(InboundDeviceCommand deviceCommand) {
        Device device = deviceRepository.findById(deviceCommand.getDeviceId()).orElseThrow();


        List<OutboundDeviceCommand> commands = device.handleInboundDeviceCommand(deviceCommand, this.getDeviceContext());
        sendDeviceCommands(commands, deviceCommand.getDeviceId());
    }

    private void sendDeviceCommands(List<OutboundDeviceCommand> commands, DeviceId deviceId) {
        for (OutboundDeviceCommand command : commands) {
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
