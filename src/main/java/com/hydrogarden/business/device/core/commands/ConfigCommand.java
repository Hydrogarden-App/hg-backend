package com.hydrogarden.business.device.core.commands;

import com.hydrogarden.business.device.core.entity.DeviceId;

import java.time.Duration;

public class ConfigCommand extends BaseConfigCommand {
    public ConfigCommand(DeviceId deviceId, Duration standbyTimeout, Duration heartbeatInterval) {
        super(deviceId, DeviceCommandType.CONFIG, standbyTimeout, heartbeatInterval);
    }

    @Override
    public void accept(DeviceCommandVisitor visitor, DeviceContext deviceContext) {
        visitor.visit(this, deviceContext);
    }

    public static ConfigCommand fromBytes(byte[] data) {

        int standbyTimeout;
        int heartbeatInterval;

        standbyTimeout = (data[0] & 0xFF) << 24;
        standbyTimeout = standbyTimeout | (data[1] & 0xFF) << 16;
        standbyTimeout = standbyTimeout | (data[2] & 0xFF) << 8;
        standbyTimeout = standbyTimeout | (data[3] & 0xFF);

        heartbeatInterval = (data[4] & 0xFF) << 24;
        heartbeatInterval = heartbeatInterval | (data[5] & 0xFF) << 16;
        heartbeatInterval = heartbeatInterval | (data[6] & 0xFF) << 8;
        heartbeatInterval = heartbeatInterval | (data[7] & 0xFF);

        Duration standbyTimeoutDuration = Duration.ofSeconds(standbyTimeout);
        Duration heartbeatIntervalDuration = Duration.ofSeconds(heartbeatInterval);

        short id = (short) (((data[0] & 0xFF) << 8) | (data[1] & 0xFF));
        return new ConfigCommand(new DeviceId(id), standbyTimeoutDuration, heartbeatIntervalDuration);
    }
}
