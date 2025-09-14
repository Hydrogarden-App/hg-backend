package com.hydrogarden.business.device.core.event;

import com.hydrogarden.business.device.core.entity.CircuitState;
import com.hydrogarden.business.device.core.entity.DeviceId;
import com.hydrogarden.business.device.core.entity.HydrogardenDomainEvent;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class RegisteredDeviceStateChangeDE extends HydrogardenDomainEvent {
    private final DeviceId deviceId;
    private final List<CircuitState> oldState;
    private final List<CircuitState> newState;

    protected RegisteredDeviceStateChangeDE(LocalDateTime timestamp, DeviceId deviceId, List<CircuitState> oldState, List<CircuitState> newState) {
        super(timestamp);
        this.deviceId = deviceId;
        this.oldState = oldState;
        this.newState = newState;
    }
}
