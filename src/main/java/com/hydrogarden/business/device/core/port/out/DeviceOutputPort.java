package com.hydrogarden.business.device.core.port.out;

import com.hydrogarden.business.device.core.commands.DeviceCommand;

public interface DeviceOutputPort {

    void sendDeviceCommand(DeviceCommand command);
}
