package com.hydrogarden.business.device.core.commands;

import com.hydrogarden.business.common.vo.DeviceId;

public abstract sealed class InboundDeviceCommand extends DeviceCommand permits AckConfigCommand, AckStateCommand, HeartbeatCommand, RequestConfigCommand {
    public InboundDeviceCommand(DeviceId deviceId, DeviceCommandType deviceCommandType) {
        super(deviceId, deviceCommandType);
    }
}
