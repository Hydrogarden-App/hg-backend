package com.hydrogarden.business.device.core.entity;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public abstract class HydrogardenDomainEvent {
    private final LocalDateTime timestamp;

    protected HydrogardenDomainEvent(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
