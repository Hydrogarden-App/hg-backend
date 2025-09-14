package com.hydrogarden.business.device.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.time.Duration;

@Embeddable
@Getter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class DeviceConfig {
    @Column(nullable = false)
    private Duration standbyTimeout;
    @Column(nullable = false)
    private Duration heartbeatInterval;
}
