package com.hydrogarden.business.device.core.commands;

import com.hydrogarden.business.device.core.entity.DeviceId;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class AckConfigCommandTest {

    private byte[] createMessage(short deviceId, int standbyTimeout, int heartbeatInterval) {
        byte[] data = new byte[12];
        // Device ID (2 bytes)
        data[0] = (byte) (deviceId >> 8);
        data[1] = (byte) deviceId;
        // Command type (ACK_CONFIG = 7)
        data[2] = DeviceCommandType.ACK_CONFIG.getCode();
        // Payload length (8 bytes)
        data[3] = 8;
        // Standby timeout (4 bytes, big-endian)
        data[4] = (byte) (standbyTimeout >> 24);
        data[5] = (byte) (standbyTimeout >> 16);
        data[6] = (byte) (standbyTimeout >> 8);
        data[7] = (byte) standbyTimeout;
        // Heartbeat interval (4 bytes, big-endian)
        data[8] = (byte) (heartbeatInterval >> 24);
        data[9] = (byte) (heartbeatInterval >> 16);
        data[10] = (byte) (heartbeatInterval >> 8);
        data[11] = (byte) heartbeatInterval;
        return data;
    }

    @Test
    void fromBytes_shouldParseConfigData() {
        // Given: ACK_CONFIG message with standbyTimeout=300s, heartbeatInterval=60s
        byte[] data = createMessage((short) 1, 300, 60);

        // When
        AckConfigCommand command = AckConfigCommand.fromBytes(data);

        // Then
        assertNotNull(command);
        assertEquals(Duration.ofSeconds(300), command.getStandbyTimeout());
        assertEquals(Duration.ofSeconds(60), command.getHeartbeatInterval());
    }

    @Test
    void fromBytes_shouldParseDeviceId() {
        // Given: Message with device ID = 1
        byte[] data = createMessage((short) 1, 0, 0);

        // When
        AckConfigCommand command = AckConfigCommand.fromBytes(data);

        // Then
        assertNotNull(command);
        assertEquals(new DeviceId((short) 1), command.getDeviceId());
    }

    @Test
    void fromBytes_shouldParseLargeDeviceId() {
        // Given: Message with device ID = 1000
        byte[] data = createMessage((short) 1000, 0, 0);

        // When
        AckConfigCommand command = AckConfigCommand.fromBytes(data);

        // Then
        assertNotNull(command);
        assertEquals(new DeviceId((short) 1000), command.getDeviceId());
    }

    @Test
    void fromBytes_shouldHandleLargeTimeoutValues() {
        // Given: Large timeout values (24 hours = 86400 seconds, 1 hour = 3600 seconds)
        byte[] data = createMessage((short) 1, 86400, 3600);

        // When
        AckConfigCommand command = AckConfigCommand.fromBytes(data);

        // Then
        assertNotNull(command);
        assertEquals(Duration.ofSeconds(86400), command.getStandbyTimeout());
        assertEquals(Duration.ofSeconds(3600), command.getHeartbeatInterval());
    }

    @Test
    void fromBytes_shouldHandleZeroValues() {
        // Given: Zero timeout values
        byte[] data = createMessage((short) 1, 0, 0);

        // When
        AckConfigCommand command = AckConfigCommand.fromBytes(data);

        // Then
        assertNotNull(command);
        assertEquals(Duration.ZERO, command.getStandbyTimeout());
        assertEquals(Duration.ZERO, command.getHeartbeatInterval());
    }

    @Test
    void fromBytes_shouldHandleMaxIntValues() {
        // Given: Maximum integer values
        byte[] data = createMessage((short) 1, Integer.MAX_VALUE, Integer.MAX_VALUE);

        // When
        AckConfigCommand command = AckConfigCommand.fromBytes(data);

        // Then
        assertNotNull(command);
        assertEquals(Duration.ofSeconds(Integer.MAX_VALUE), command.getStandbyTimeout());
        assertEquals(Duration.ofSeconds(Integer.MAX_VALUE), command.getHeartbeatInterval());
    }

    @Test
    void toBytes_shouldCreateValidMessage() {
        // Given
        DeviceId deviceId = new DeviceId((short) 1);
        Duration standbyTimeout = Duration.ofSeconds(300);
        Duration heartbeatInterval = Duration.ofSeconds(60);
        AckConfigCommand command = new AckConfigCommand(deviceId, standbyTimeout, heartbeatInterval);

        // When
        byte[] result = command.toBytes();

        // Then
        assertNotNull(result);
        assertEquals(12, result.length); // 2 (deviceId) + 1 (type) + 1 (length) + 8 (payload)

        // Verify header
        assertEquals((byte) 0x00, result[0]); // Device ID high byte
        assertEquals((byte) 0x01, result[1]); // Device ID low byte
        assertEquals(DeviceCommandType.ACK_CONFIG.getCode(), result[2]); // Command type
        assertEquals((byte) 8, result[3]); // Payload length

        // Verify payload (standbyTimeout = 300 = 0x0000012C)
        assertEquals((byte) 0x00, result[4]);
        assertEquals((byte) 0x00, result[5]);
        assertEquals((byte) 0x01, result[6]);
        assertEquals((byte) 0x2C, result[7]);

        // Verify payload (heartbeatInterval = 60 = 0x0000003C)
        assertEquals((byte) 0x00, result[8]);
        assertEquals((byte) 0x00, result[9]);
        assertEquals((byte) 0x00, result[10]);
        assertEquals((byte) 0x3C, result[11]);
    }

    @Test
    void roundTrip_shouldWork() {
        // Given: Create a command and convert to bytes
        DeviceId deviceId = new DeviceId((short) 1);
        Duration standbyTimeout = Duration.ofSeconds(300);
        Duration heartbeatInterval = Duration.ofSeconds(60);
        AckConfigCommand original = new AckConfigCommand(deviceId, standbyTimeout, heartbeatInterval);

        byte[] bytes = original.toBytes();

        // When: Parse the bytes back
        AckConfigCommand parsed = AckConfigCommand.fromBytes(bytes);

        // Then: The parsed command should match the original
        assertNotNull(parsed);
        assertEquals(original.getDeviceId(), parsed.getDeviceId());
        assertEquals(original.getStandbyTimeout(), parsed.getStandbyTimeout());
        assertEquals(original.getHeartbeatInterval(), parsed.getHeartbeatInterval());
    }

    @Test
    void roundTrip_shouldWorkWithVariousValues() {
        // Given: Various test cases
        short[] deviceIds = {1, 100, 1000, Short.MAX_VALUE};
        int[] timeouts = {0, 1, 60, 300, 3600, 86400, Integer.MAX_VALUE};

        for (short id : deviceIds) {
            for (int timeout : timeouts) {
                for (int interval : timeouts) {
                    DeviceId deviceId = new DeviceId(id);
                    Duration standbyTimeout = Duration.ofSeconds(timeout);
                    Duration heartbeatInterval = Duration.ofSeconds(interval);
                    AckConfigCommand original = new AckConfigCommand(deviceId, standbyTimeout, heartbeatInterval);

                    // When
                    byte[] bytes = original.toBytes();
                    AckConfigCommand parsed = AckConfigCommand.fromBytes(bytes);

                    // Then
                    assertEquals(original.getDeviceId(), parsed.getDeviceId(),
                            String.format("DeviceId mismatch for id=%d, timeout=%d, interval=%d", id, timeout, interval));
                    assertEquals(original.getStandbyTimeout(), parsed.getStandbyTimeout(),
                            String.format("StandbyTimeout mismatch for id=%d, timeout=%d, interval=%d", id, timeout, interval));
                    assertEquals(original.getHeartbeatInterval(), parsed.getHeartbeatInterval(),
                            String.format("HeartbeatInterval mismatch for id=%d, timeout=%d, interval=%d", id, timeout, interval));
                }
            }
        }
    }
}
