package com.hydrogarden.business.device.core.event;

import com.hydrogarden.business.device.core.entity.DeviceId;
import com.hydrogarden.business.device.core.entity.HydrogardenDomainEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
public class RegisteredDeviceStartDE extends HydrogardenDomainEvent {
    private final DeviceId deviceId;

    public RegisteredDeviceStartDE(DeviceId deviceId, LocalDateTime timestamp) {
        super(timestamp);
        this.deviceId = deviceId;
    }
}
