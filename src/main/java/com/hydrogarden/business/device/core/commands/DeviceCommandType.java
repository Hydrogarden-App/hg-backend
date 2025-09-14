package com.hydrogarden.business.device.core.commands;

public enum DeviceCommandType {
    KEEP_ALIVE((byte) 1),
    START((byte) 2),
    HEARTBEAT((byte) 3),
    NEW_STATE((byte) 4),
    ACK_STATE((byte) 5),
    CONFIG((byte)6),
    ACK_CONFIG((byte)7),
    REQUEST_CONFIG((byte)8);

    private final byte code;
    DeviceCommandType(byte code) { this.code = code; }
    public byte getCode() { return code; }
}
