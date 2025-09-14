package com.hydrogarden.business.device.core.commands;

import com.hydrogarden.business.device.core.entity.DeviceId;

public class RequestConfigCommand extends AbstractDeviceCommand{
    protected RequestConfigCommand(DeviceId deviceId) {
        super(deviceId, DeviceCommandType.REQUEST_CONFIG);
    }

    @Override
    public byte[] getPayload() {
        return new byte[0];
    }

    @Override
    public void accept(DeviceCommandVisitor visitor, DeviceContext deviceContext) {
        visitor.visit(this,deviceContext);
    }

    public static RequestConfigCommand fromBytes(byte[] data) {
        short id = (short) (((data[0] & 0xFF) << 8) | (data[1] & 0xFF));
        return new RequestConfigCommand(new DeviceId(id));
    }
}
