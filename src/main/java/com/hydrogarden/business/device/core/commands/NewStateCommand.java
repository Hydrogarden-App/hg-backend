package com.hydrogarden.business.device.core.commands;

import com.hydrogarden.business.device.core.BinaryUtils;
import com.hydrogarden.business.device.core.entity.CircuitState;
import com.hydrogarden.business.device.core.entity.DeviceId;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

// Payload commands
@Getter
public final class NewStateCommand extends OutboundDeviceCommand {
    private final List<CircuitState> states;

    public NewStateCommand(DeviceId deviceId, List<CircuitState> states) {
        super(deviceId, DeviceCommandType.NEW_STATE);
        this.states = states;
    }

    @Override
    public byte[] getPayload() {
        boolean[] booleans = new boolean[states.size()];

        for (int i = 0; i < states.size(); i++) {
            booleans[i] = states.get(i).getValue();
        }
        return BinaryUtils.packBooleans(booleans);
    }

    public static NewStateCommand fromBytes(byte[] data) {
        short id = (short) (((data[0] & 0xFF) << 8) | (data[1] & 0xFF));
        int payloadLen = data[3] & 0xFF;
        boolean[] states = BinaryUtils.unpackBooleans(Arrays.copyOfRange(data, 4, 4 + payloadLen), payloadLen * 8);

        return new NewStateCommand(new DeviceId(id), IntStream.range(0, states.length)
                .mapToObj(i -> new CircuitState(states[i]))
                .toList());
    }
}