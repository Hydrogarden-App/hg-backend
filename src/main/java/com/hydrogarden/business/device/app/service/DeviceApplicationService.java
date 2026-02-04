package com.hydrogarden.business.device.app.service;

import com.hydrogarden.business.device.core.commands.DeviceCommand;
import com.hydrogarden.business.device.core.commands.InboundDeviceCommand;
import com.hydrogarden.business.device.core.entity.CircuitId;
import com.hydrogarden.business.device.core.entity.CircuitState;
import com.hydrogarden.business.device.core.entity.Device;
import com.hydrogarden.business.device.core.entity.DeviceId;

/**
 * Application Service interface for orchestrating Device use cases.
 */
public interface DeviceApplicationService {

    /**
     * Rename a device.
     *
     * @param deviceId device ID
     * @param newName  new device name
     */
    Device renameDevice(DeviceId deviceId, String newName);

    /**
     * Get a device by ID.
     *
     * @param deviceId device ID
     * @return the device
     */
    Device getDevice(DeviceId deviceId);

    /**
     * Enable a device.
     *
     * @param deviceId device ID
     * @return the updated device
     */
    Device enableDevice(DeviceId deviceId);

    /**
     * Disable a device.
     *
     * @param deviceId device ID
     * @return the updated device
     */
    Device disableDevice(DeviceId deviceId);

    /**
     * Request to change circuit status.
     *
     * @param deviceId     device ID
     * @param circuitId    circuit ID
     * @param circuitState new circuit state
     * @return the updated device
     */
    Device requestChangeCircuitStatus(DeviceId deviceId, CircuitId circuitId, CircuitState circuitState);

    /**
     * Handle a device command.
     *
     * @param deviceCommand the device command to handle
     */
    void handleDeviceCommand(InboundDeviceCommand deviceCommand);
}
