package com.hydrogarden.business.device.core.commands;

import com.hydrogarden.business.device.core.entity.DeviceId;
import lombok.Getter;

import java.time.Duration;

@Getter
public abstract class BaseConfigCommand extends AbstractDeviceCommand {
    private final Duration standbyTimeout;
    private final Duration heartbeatInterval;

    protected BaseConfigCommand(DeviceId deviceId, DeviceCommandType type, Duration standbyTimeout, Duration heartbeatInterval) {
        super(deviceId, type);
        this.standbyTimeout = standbyTimeout;
        this.heartbeatInterval = heartbeatInterval;
    }

    @Override
    public byte[] getPayload() {
        byte[] payload = new byte[8];

        int standbyTimeout = Math.toIntExact(getStandbyTimeout().getSeconds());
        int heartbeatInterval = Math.toIntExact(getHeartbeatInterval().getSeconds());


        payload[0] = (byte) (standbyTimeout >> 24);
        payload[1] = (byte) (standbyTimeout >> 16);
        payload[2] = (byte) (standbyTimeout >> 8);
        payload[3] = (byte) (standbyTimeout);

        payload[4] = (byte) (heartbeatInterval >> 24);
        payload[5] = (byte) (heartbeatInterval >> 16);
        payload[6] = (byte) (heartbeatInterval >> 8);
        payload[7] = (byte) (heartbeatInterval);
        return payload;
    }
}
