package com.hydrogarden.business.device.core.commands;

import com.hydrogarden.business.device.core.entity.DeviceId;

public abstract class AbstractDeviceCommand implements DeviceCommand {
    protected final DeviceId deviceId;
    protected final DeviceCommandType type;

    protected AbstractDeviceCommand(DeviceId deviceId, DeviceCommandType type) {
        this.deviceId = deviceId;
        this.type = type;
    }

    @Override
    public DeviceId getDeviceId() { return deviceId; }

    @Override
    public DeviceCommandType getCommandType() { return type; }

    @Override
    public byte getPayloadLength() {
        byte[] payload = getPayload();
        return (byte) (payload != null ? payload.length : 0);
    }

    @Override
    public byte[] toBytes() {
        byte[] payload = getPayload();
        int length = 2 + 1 + 1 + getPayloadLength();
        byte[] result = new byte[length];
        result[0] = (byte) ((deviceId.getId() >> 8) & 0xFF);
        result[1] = (byte) (deviceId.getId() & 0xFF);
        result[2] = type.getCode();
        result[3] = getPayloadLength();
        if (payload != null) System.arraycopy(payload, 0, result, 4, payload.length);
        return result;
    }

}
