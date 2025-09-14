package com.hydrogarden.business.device.core.event;

import com.hydrogarden.business.device.core.entity.DeviceConfig;
import com.hydrogarden.business.device.core.entity.DeviceId;
import com.hydrogarden.business.device.core.entity.HydrogardenDomainEvent;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class RegisteredHeartbeatDE extends HydrogardenDomainEvent {
    private final DeviceId deviceId;

    public RegisteredHeartbeatDE(LocalDateTime timestamp, DeviceId deviceId, DeviceConfig oldConfig, DeviceConfig newConfig) {
        super(timestamp);
        this.deviceId = deviceId;
    }
}
