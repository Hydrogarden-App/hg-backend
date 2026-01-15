package com.hydrogarden.business.device.core.commands;

import com.hydrogarden.business.device.core.entity.DeviceId;

import java.time.Duration;

public abstract sealed class InboundDeviceCommand extends DeviceCommand permits AckConfigCommand, AckStateCommand, HeartbeatCommand, RequestConfigCommand {
    public InboundDeviceCommand(DeviceId deviceId, DeviceCommandType deviceCommandType) {
        super(deviceId, deviceCommandType);
    }
}
