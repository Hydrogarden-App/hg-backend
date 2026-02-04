package com.hydrogarden.business.device.infra.factory;

import com.hydrogarden.business.device.core.commands.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class DeviceCommandFactory {
    private static final Map<Byte, Function<byte[], DeviceCommand>> registry = new ConcurrentHashMap<>();

    static {
        register(DeviceCommandType.KEEP_ALIVE.getCode(), KeepAliveCommand::fromBytes);
        register(DeviceCommandType.HEARTBEAT.getCode(), HeartbeatCommand::fromBytes);
        register(DeviceCommandType.NEW_STATE.getCode(), NewStateCommand::fromBytes);
        register(DeviceCommandType.ACK_STATE.getCode(), AckStateCommand::fromBytes);
        register(DeviceCommandType.CONFIG.getCode(), ConfigCommand::fromBytes);
        register(DeviceCommandType.ACK_CONFIG.getCode(), AckConfigCommand::fromBytes);
        register(DeviceCommandType.REQUEST_CONFIG.getCode(), RequestConfigCommand::fromBytes);
    }

    public static void register(byte type, Function<byte[], DeviceCommand> parser) {
        registry.put(type, parser);
    }

    public static DeviceCommand fromBytes(byte[] data) {
        byte type = data[2];
        Function<byte[], DeviceCommand> parser = registry.get(type);
        if (parser == null) throw new IllegalArgumentException("Unknown command type " + type);
        DeviceCommand command = parser.apply(data);
        return command;
    }
}
