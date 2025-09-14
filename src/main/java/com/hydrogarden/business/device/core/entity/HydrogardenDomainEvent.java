package com.hydrogarden.business.device.core.entity;

import java.time.LocalDateTime;

public abstract class HydrogardenDomainEvent {
    private final LocalDateTime timestamp;

    protected HydrogardenDomainEvent(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
