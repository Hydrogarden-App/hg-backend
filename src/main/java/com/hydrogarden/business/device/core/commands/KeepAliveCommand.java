package com.hydrogarden.business.device.core.commands;

import com.hydrogarden.business.device.core.entity.DeviceId;

public final class KeepAliveCommand extends OutboundDeviceCommand {
    public KeepAliveCommand(DeviceId deviceId) {
        super(deviceId, DeviceCommandType.KEEP_ALIVE);
    }

    @Override
    public byte[] getPayload() {
        return new byte[0];
    }

    public static KeepAliveCommand fromBytes(byte[] data) {
        short id = (short) (((data[0] & 0xFF) << 8) | (data[1] & 0xFF));
        return new KeepAliveCommand(new DeviceId(id));
    }
}