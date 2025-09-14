package com.hydrogarden.business.device.core.event;

import com.hydrogarden.business.device.core.entity.DeviceConfig;
import com.hydrogarden.business.device.core.entity.DeviceId;
import com.hydrogarden.business.device.core.entity.HydrogardenDomainEvent;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ConfigSentDE extends HydrogardenDomainEvent {
    private final DeviceId deviceId;
    private final DeviceConfig oldConfig;
    private final DeviceConfig newConfig;

    public ConfigSentDE(LocalDateTime timestamp, DeviceId deviceId, DeviceConfig oldConfig, DeviceConfig newConfig) {
        super(timestamp);
        this.deviceId = deviceId;
        this.oldConfig = oldConfig;
        this.newConfig = newConfig;
    }
}
