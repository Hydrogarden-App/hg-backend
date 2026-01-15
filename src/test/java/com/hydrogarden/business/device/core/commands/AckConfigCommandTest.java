package com.hydrogarden.business.device.core.commands;

import com.hydrogarden.business.device.core.entity.DeviceId;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class AckConfigCommandTest {

    @Test
    void fromBytes_shouldParseConfigData() {
        // Given: ACK_CONFIG message with standbyTimeout=300s, heartbeatInterval=60s
        // Message format: [standbyTimeout(4 bytes)][heartbeatInterval(4 bytes)]
        byte[] data = new byte[8];

        // Standby timeout = 300 seconds
        int standbyTimeout = 300;
        data[0] = (byte) (standbyTimeout >> 24);
        data[1] = (byte) (standbyTimeout >> 16);
        data[2] = (byte) (standbyTimeout >> 8);
        data[3] = (byte) standbyTimeout;

        // Heartbeat interval = 60 seconds
        int heartbeatInterval = 60;
        data[4] = (byte) (heartbeatInterval >> 24);
        data[5] = (byte) (heartbeatInterval >> 16);
        data[6] = (byte) (heartbeatInterval >> 8);
        data[7] = (byte) heartbeatInterval;

        // When
        AckConfigCommand command = AckConfigCommand.fromBytes(data);

        // Then
        assertNotNull(command);
        assertEquals(Duration.ofSeconds(300), command.getStandbyTimeout());
        assertEquals(Duration.ofSeconds(60), command.getHeartbeatInterval());
    }

    @Test
    void fromBytes_shouldParseDeviceIdFromFirstTwoBytes() {
        // Given: Message with device ID = 1 (0x0001)
        byte[] data = new byte[8];
        data[0] = 0x00;  // Device ID high byte
        data[1] = 0x01;  // Device ID low byte
        data[2] = 0x00;
        data[3] = 0x00;
        data[4] = 0x00;
        data[5] = 0x00;
        data[6] = 0x00;
        data[7] = 0x00;

        // When
        AckConfigCommand command = AckConfigCommand.fromBytes(data);

        // Then
        assertNotNull(command);
        assertEquals(new DeviceId((short) 1), command.getDeviceId());
    }

    @Test
    void fromBytes_shouldHandleLargeTimeoutValues() {
        // Given: Large timeout values (24 hours = 86400 seconds)
        byte[] data = new byte[8];

        int standbyTimeout = 86400;
        data[0] = (byte) (standbyTimeout >> 24);
        data[1] = (byte) (standbyTimeout >> 16);
        data[2] = (byte) (standbyTimeout >> 8);
        data[3] = (byte) standbyTimeout;

        int heartbeatInterval = 3600; // 1 hour
        data[4] = (byte) (heartbeatInterval >> 24);
        data[5] = (byte) (heartbeatInterval >> 16);
        data[6] = (byte) (heartbeatInterval >> 8);
        data[7] = (byte) heartbeatInterval;

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
        byte[] data = new byte[8];
        // All bytes are 0

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
        byte[] data = new byte[8];

        // Max int = 2147483647
        data[0] = (byte) 0x7F;
        data[1] = (byte) 0xFF;
        data[2] = (byte) 0xFF;
        data[3] = (byte) 0xFF;

        data[4] = (byte) 0x7F;
        data[5] = (byte) 0xFF;
        data[6] = (byte) 0xFF;
        data[7] = (byte) 0xFF;

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

        // Verify payload (standbyTimeout = 300)
        assertEquals((byte) 0x00, result[4]);
        assertEquals((byte) 0x00, result[5]);
        assertEquals((byte) 0x01, result[6]);
        assertEquals((byte) 0x2C, result[7]); // 300 in hex = 0x012C

        // Verify payload (heartbeatInterval = 60)
        assertEquals((byte) 0x00, result[8]);
        assertEquals((byte) 0x00, result[9]);
        assertEquals((byte) 0x00, result[10]);
        assertEquals((byte) 0x3C, result[11]); // 60 in hex = 0x3C
    }

    @Test
    void roundTrip_shouldWork() {
        // This test documents the BUG in fromBytes()
        // Given: Create a command and convert to bytes
        DeviceId deviceId = new DeviceId((short) 1);
        Duration standbyTimeout = Duration.ofSeconds(300);
        Duration heartbeatInterval = Duration.ofSeconds(60);
        AckConfigCommand original = new AckConfigCommand(deviceId, standbyTimeout, heartbeatInterval);

        byte[] bytes = original.toBytes();

        // When: Parse the bytes back
        AckConfigCommand parsed = AckConfigCommand.fromBytes(bytes);

        // Then: The parsed command DOES NOT match the original due to the bug
        // fromBytes() reads config from [0-7] instead of payload [4-11]
        assertNotNull(parsed);

        // BUG: Device ID gets mixed with standby timeout because fromBytes reads from wrong indices
        // The device ID at [0-1] gets interpreted as part of standbyTimeout
        // bytes[0-3] = 0x00, 0x01, 0x07, 0x08 (deviceId=1, type=7, length=8)
        // fromBytes reads this as standbyTimeout = 0x00010708 = 67336 seconds
        assertEquals(standbyTimeout, parsed.getStandbyTimeout());
        assertEquals(heartbeatInterval, parsed.getHeartbeatInterval());
    }
}
