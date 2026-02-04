package com.hydrogarden.business.device.core.commands;

import com.hydrogarden.business.device.core.entity.DeviceId;

public final class HeartbeatCommand extends InboundDeviceCommand {
    public HeartbeatCommand(DeviceId deviceId) {
        super(deviceId, DeviceCommandType.HEARTBEAT);
    }

    @Override
    public byte[] getPayload() {
        return new byte[0];
    }

    public static HeartbeatCommand fromBytes(byte[] data) {
        short id = (short) (((data[0] & 0xFF) << 8) | (data[1] & 0xFF));
        return new HeartbeatCommand(new DeviceId(id));
    }
}