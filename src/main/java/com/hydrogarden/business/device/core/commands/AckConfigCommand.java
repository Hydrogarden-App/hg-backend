package com.hydrogarden.business.device.core.commands;

import com.hydrogarden.business.device.core.entity.DeviceId;
import lombok.Getter;

import java.time.Duration;

@Getter
public final class AckConfigCommand extends InboundDeviceCommand {
    private final Duration standbyTimeout;
    private final Duration heartbeatInterval;

    public AckConfigCommand(DeviceId deviceId, Duration standbyTimeout, Duration heartbeatInterval) {
        super(deviceId, DeviceCommandType.ACK_CONFIG);
        this.standbyTimeout = standbyTimeout;
        this.heartbeatInterval = heartbeatInterval;
    }

    public static AckConfigCommand fromBytes(byte[] data) {

        int standbyTimeout;
        int heartbeatInterval;

        standbyTimeout = (data[4] & 0xFF) << 24;
        standbyTimeout = standbyTimeout | (data[5] & 0xFF) << 16;
        standbyTimeout = standbyTimeout | (data[6] & 0xFF) << 8;
        standbyTimeout = standbyTimeout | (data[7] & 0xFF);

        heartbeatInterval = (data[8] & 0xFF) << 24;
        heartbeatInterval = heartbeatInterval | (data[9] & 0xFF) << 16;
        heartbeatInterval = heartbeatInterval | (data[10] & 0xFF) << 8;
        heartbeatInterval = heartbeatInterval | (data[11] & 0xFF);

        Duration standbyTimeoutDuration = Duration.ofSeconds(standbyTimeout);
        Duration heartbeatIntervalDuration = Duration.ofSeconds(heartbeatInterval);

        short id = (short) (((data[0] & 0xFF) << 8) | (data[1] & 0xFF));
        return new AckConfigCommand(new DeviceId(id), standbyTimeoutDuration, heartbeatIntervalDuration);
    }

    public byte[] getPayload() {
        byte[] payload = new byte[8];

        int standbyTimeout = Math.toIntExact(this.standbyTimeout.getSeconds());
        int heartbeatInterval = Math.toIntExact(this.heartbeatInterval.getSeconds());


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
