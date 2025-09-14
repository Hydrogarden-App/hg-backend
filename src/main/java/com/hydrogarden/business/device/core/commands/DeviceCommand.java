package com.hydrogarden.business.device.core.commands;

import com.hydrogarden.business.device.core.entity.DeviceId;

public interface DeviceCommand {
    DeviceId getDeviceId();       // 2 bytes
    DeviceCommandType getCommandType();     // 1 byte
    byte getPayloadLength();   // 1 byte
    byte[] getPayload();       // variable

    byte[] toBytes();          // serialize
    void accept(DeviceCommandVisitor visitor, DeviceContext deviceContext); // <-- new

}
