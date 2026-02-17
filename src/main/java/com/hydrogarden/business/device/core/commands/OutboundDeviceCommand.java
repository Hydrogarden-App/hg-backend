package com.hydrogarden.business.device.core.commands;

import com.hydrogarden.business.common.vo.DeviceId;

public abstract sealed class OutboundDeviceCommand extends DeviceCommand permits ConfigCommand, KeepAliveCommand, NewStateCommand {
    public OutboundDeviceCommand(DeviceId deviceId, DeviceCommandType deviceCommandType) {
        super(deviceId, deviceCommandType);
    }
}
