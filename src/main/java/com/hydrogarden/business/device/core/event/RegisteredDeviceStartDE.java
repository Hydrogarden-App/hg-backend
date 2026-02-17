package com.hydrogarden.business.device.core.event;

import com.hydrogarden.business.common.vo.DeviceId;
import com.hydrogarden.business.common.event.HydrogardenDomainEvent;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class RegisteredDeviceStartDE extends HydrogardenDomainEvent {
    private final DeviceId deviceId;

    public RegisteredDeviceStartDE(DeviceId deviceId, LocalDateTime timestamp) {
        super(timestamp);
        this.deviceId = deviceId;
    }
}
