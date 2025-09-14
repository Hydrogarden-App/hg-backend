package com.hydrogarden.business.device.core.commands;

import com.hydrogarden.business.device.core.entity.DeviceId;

public class StartCommand extends AbstractDeviceCommand {
    public StartCommand(DeviceId deviceId) {
        super(deviceId, DeviceCommandType.START);
    }

    @Override
    public byte[] getPayload() {
        return new byte[0];
    }

    public static StartCommand fromBytes(byte[] data) {
        short id = (short) (((data[0] & 0xFF) << 8) | (data[1] & 0xFF));
        return new StartCommand(new DeviceId(id));
    }

    @Override
    public void accept(DeviceCommandVisitor visitor, DeviceContext deviceContext) {
        visitor.visit(this, deviceContext);
    }
}
