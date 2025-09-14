package com.hydrogarden.business.device.core.event;

import com.hydrogarden.business.device.core.entity.DeviceId;
import com.hydrogarden.business.device.core.entity.HydrogardenDomainEvent;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class KeepaliveSentDE extends HydrogardenDomainEvent {
    private final DeviceId deviceId;

    public KeepaliveSentDE(DeviceId deviceId, LocalDateTime timestamp) {
        super(timestamp);
        this.deviceId = deviceId;
    }
}
