package com.hydrogarden.business.device.core.commands;

import com.hydrogarden.business.device.core.entity.DeviceId;

public final class RequestConfigCommand extends InboundDeviceCommand {
    public RequestConfigCommand(DeviceId deviceId) {
        super(deviceId, DeviceCommandType.REQUEST_CONFIG);
    }

    @Override
    public byte[] getPayload() {
        return new byte[0];
    }

    public static RequestConfigCommand fromBytes(byte[] data) {
        short id = (short) (((data[0] & 0xFF) << 8) | (data[1] & 0xFF));
        return new RequestConfigCommand(new DeviceId(id));
    }
}
