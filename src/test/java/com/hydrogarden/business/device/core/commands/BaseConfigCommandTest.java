package com.hydrogarden.business.device.core.commands;

import com.hydrogarden.business.device.core.entity.DeviceId;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class BaseConfigCommandTest {

    @Test
    void getPayload_shouldCreateEightByteArray() {
        // Given
        DeviceId deviceId = new DeviceId((short) 1);
        Duration standbyTimeout = Duration.ofSeconds(300);
        Duration heartbeatInterval = Duration.ofSeconds(60);
        ConfigCommand command = new ConfigCommand(deviceId, standbyTimeout, heartbeatInterval);

        // When
        byte[] payload = command.getPayload();

        // Then
        assertNotNull(payload);
        assertEquals(8, payload.length);
    }

    @Test
    void getPayload_shouldEncodeStandbyTimeoutInBigEndian() {
        // Given
        DeviceId deviceId = new DeviceId((short) 1);
        Duration standbyTimeout = Duration.ofSeconds(300);
        Duration heartbeatInterval = Duration.ofSeconds(60);
        ConfigCommand command = new ConfigCommand(deviceId, standbyTimeout, heartbeatInterval);

        // When
        byte[] payload = command.getPayload();

        // Then: standbyTimeout = 300 = 0x0000012C in big-endian
        assertEquals((byte) 0x00, payload[0]); // Most significant byte
        assertEquals((byte) 0x00, payload[1]);
        assertEquals((byte) 0x01, payload[2]);
        assertEquals((byte) 0x2C, payload[3]); // Least significant byte
    }

    @Test
    void getPayload_shouldEncodeHeartbeatIntervalInBigEndian() {
        // Given
        DeviceId deviceId = new DeviceId((short) 1);
        Duration standbyTimeout = Duration.ofSeconds(300);
        Duration heartbeatInterval = Duration.ofSeconds(60);
        ConfigCommand command = new ConfigCommand(deviceId, standbyTimeout, heartbeatInterval);

        // When
        byte[] payload = command.getPayload();

        // Then: heartbeatInterval = 60 = 0x0000003C in big-endian
        assertEquals((byte) 0x00, payload[4]); // Most significant byte
        assertEquals((byte) 0x00, payload[5]);
        assertEquals((byte) 0x00, payload[6]);
        assertEquals((byte) 0x3C, payload[7]); // Least significant byte
    }

    @Test
    void getPayload_shouldHandleZeroValues() {
        // Given
        DeviceId deviceId = new DeviceId((short) 1);
        Duration standbyTimeout = Duration.ZERO;
        Duration heartbeatInterval = Duration.ZERO;
        ConfigCommand command = new ConfigCommand(deviceId, standbyTimeout, heartbeatInterval);

        // When
        byte[] payload = command.getPayload();

        // Then: All bytes should be 0
        assertNotNull(payload);
        assertEquals(8, payload.length);
        for (int i = 0; i < 8; i++) {
            assertEquals((byte) 0x00, payload[i], "Byte at index " + i + " should be 0");
        }
    }

    @Test
    void getPayload_shouldHandleLargeValues() {
        // Given: 24 hours = 86400 seconds, 1 hour = 3600 seconds
        DeviceId deviceId = new DeviceId((short) 1);
        Duration standbyTimeout = Duration.ofSeconds(86400);
        Duration heartbeatInterval = Duration.ofSeconds(3600);
        ConfigCommand command = new ConfigCommand(deviceId, standbyTimeout, heartbeatInterval);

        // When
        byte[] payload = command.getPayload();

        // Then: standbyTimeout = 86400 = 0x00015180
        assertEquals((byte) 0x00, payload[0]);
        assertEquals((byte) 0x01, payload[1]);
        assertEquals((byte) 0x51, payload[2]);
        assertEquals((byte) 0x80, payload[3]);

        // heartbeatInterval = 3600 = 0x00000E10
        assertEquals((byte) 0x00, payload[4]);
        assertEquals((byte) 0x00, payload[5]);
        assertEquals((byte) 0x0E, payload[6]);
        assertEquals((byte) 0x10, payload[7]);
    }

    @Test
    void getPayload_shouldHandleMaxIntValues() {
        // Given: Maximum positive integer value
        DeviceId deviceId = new DeviceId((short) 1);
        Duration standbyTimeout = Duration.ofSeconds(Integer.MAX_VALUE);
        Duration heartbeatInterval = Duration.ofSeconds(Integer.MAX_VALUE);
        ConfigCommand command = new ConfigCommand(deviceId, standbyTimeout, heartbeatInterval);

        // When
        byte[] payload = command.getPayload();

        // Then: Integer.MAX_VALUE = 2147483647 = 0x7FFFFFFF
        assertEquals((byte) 0x7F, payload[0]);
        assertEquals((byte) 0xFF, payload[1]);
        assertEquals((byte) 0xFF, payload[2]);
        assertEquals((byte) 0xFF, payload[3]);

        assertEquals((byte) 0x7F, payload[4]);
        assertEquals((byte) 0xFF, payload[5]);
        assertEquals((byte) 0xFF, payload[6]);
        assertEquals((byte) 0xFF, payload[7]);
    }

    @Test
    void getPayload_shouldHandleNegativeByteValues() {
        // Given: Value that produces negative bytes when cast
        // 255 seconds = 0x000000FF, byte 0xFF is -1 when cast to signed byte
        DeviceId deviceId = new DeviceId((short) 1);
        Duration standbyTimeout = Duration.ofSeconds(255);
        Duration heartbeatInterval = Duration.ofSeconds(128);
        ConfigCommand command = new ConfigCommand(deviceId, standbyTimeout, heartbeatInterval);

        // When
        byte[] payload = command.getPayload();

        // Then: standbyTimeout = 255 = 0x000000FF
        assertEquals((byte) 0x00, payload[0]);
        assertEquals((byte) 0x00, payload[1]);
        assertEquals((byte) 0x00, payload[2]);
        assertEquals((byte) 0xFF, payload[3]); // This is -1 as signed byte

        // heartbeatInterval = 128 = 0x00000080
        assertEquals((byte) 0x00, payload[4]);
        assertEquals((byte) 0x00, payload[5]);
        assertEquals((byte) 0x00, payload[6]);
        assertEquals((byte) 0x80, payload[7]); // This is -128 as signed byte
    }

    @Test
    void getPayload_shouldBeConsistent() {
        // Given: Same input values
        DeviceId deviceId = new DeviceId((short) 1);
        Duration standbyTimeout = Duration.ofSeconds(300);
        Duration heartbeatInterval = Duration.ofSeconds(60);
        ConfigCommand command = new ConfigCommand(deviceId, standbyTimeout, heartbeatInterval);

        // When: Called multiple times
        byte[] payload1 = command.getPayload();
        byte[] payload2 = command.getPayload();

        // Then: Should produce identical results
        assertArrayEquals(payload1, payload2);
    }

    @Test
    void getPayload_shouldSupportRoundTrip() {
        // Given: Original command with specific values
        DeviceId deviceId = new DeviceId((short) 42);
        Duration standbyTimeout = Duration.ofSeconds(1200);
        Duration heartbeatInterval = Duration.ofSeconds(120);
        ConfigCommand original = new ConfigCommand(deviceId, standbyTimeout, heartbeatInterval);

        // When: Get payload and parse it back
        byte[] payload = original.getPayload();
        ConfigCommand parsed = ConfigCommand.fromBytes(payload);

        // Then: Parsed command should have same timeout values
        assertNotNull(parsed);
        assertEquals(standbyTimeout, parsed.getStandbyTimeout());
        assertEquals(heartbeatInterval, parsed.getHeartbeatInterval());
    }

    @Test
    void getPayload_shouldHandleOneSecondInterval() {
        // Given: Minimum practical values (1 second)
        DeviceId deviceId = new DeviceId((short) 1);
        Duration standbyTimeout = Duration.ofSeconds(1);
        Duration heartbeatInterval = Duration.ofSeconds(1);
        ConfigCommand command = new ConfigCommand(deviceId, standbyTimeout, heartbeatInterval);

        // When
        byte[] payload = command.getPayload();

        // Then: 1 = 0x00000001
        assertEquals((byte) 0x00, payload[0]);
        assertEquals((byte) 0x00, payload[1]);
        assertEquals((byte) 0x00, payload[2]);
        assertEquals((byte) 0x01, payload[3]);

        assertEquals((byte) 0x00, payload[4]);
        assertEquals((byte) 0x00, payload[5]);
        assertEquals((byte) 0x00, payload[6]);
        assertEquals((byte) 0x01, payload[7]);
    }

    @Test
    void getPayload_shouldConvertDurationToSeconds() {
        // Given: Duration specified in minutes
        DeviceId deviceId = new DeviceId((short) 1);
        Duration standbyTimeout = Duration.ofMinutes(5); // 300 seconds
        Duration heartbeatInterval = Duration.ofMinutes(1); // 60 seconds
        ConfigCommand command = new ConfigCommand(deviceId, standbyTimeout, heartbeatInterval);

        // When
        byte[] payload = command.getPayload();

        // Then: Should convert to seconds (300 and 60)
        // 300 = 0x0000012C
        assertEquals((byte) 0x00, payload[0]);
        assertEquals((byte) 0x00, payload[1]);
        assertEquals((byte) 0x01, payload[2]);
        assertEquals((byte) 0x2C, payload[3]);

        // 60 = 0x0000003C
        assertEquals((byte) 0x00, payload[4]);
        assertEquals((byte) 0x00, payload[5]);
        assertEquals((byte) 0x00, payload[6]);
        assertEquals((byte) 0x3C, payload[7]);
    }
}
