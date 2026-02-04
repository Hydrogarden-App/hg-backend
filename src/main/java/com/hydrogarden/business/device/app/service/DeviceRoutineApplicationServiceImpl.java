package com.hydrogarden.business.device.app.service;

import com.hydrogarden.business.device.core.commands.DeviceCommand;
import com.hydrogarden.business.device.core.commands.DeviceContext;
import com.hydrogarden.business.device.core.commands.OutboundDeviceCommand;
import com.hydrogarden.business.device.core.entity.Device;
import com.hydrogarden.business.device.core.entity.DeviceId;
import com.hydrogarden.business.device.core.port.out.DeviceOutputPort;
import com.hydrogarden.business.device.infra.repository.DeviceRepository;
import com.hydrogarden.common.HydrogardenTimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * Application Service for running device routine tasks.
 * Handles periodic device evaluation and command execution.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DeviceRoutineApplicationServiceImpl implements DeviceRoutineApplicationService {

    private final DeviceRepository deviceRepository;
    private final DeviceOutputPort deviceOutputPort;
    private final TransactionTemplate transactionTemplate;
    private final HydrogardenTimeProvider hydrogardenTimeProvider;

    /**
     * Periodically evaluate all devices, send commands if necessary,
     * and acknowledge execution.
     */
    @Override
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

        List<OutboundDeviceCommand> commands = transactionTemplate.execute(status -> {
            Device device = deviceRepository.findById(deviceId).orElseThrow();
            device = deviceRepository.save(device);
            return device.evaluateCurrentStateAndCommand(this.getDeviceContext());
        });

        sendDeviceCommands(commands, deviceId);

    }

    private void sendDeviceCommands(List<OutboundDeviceCommand> commands, DeviceId deviceId) {
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
