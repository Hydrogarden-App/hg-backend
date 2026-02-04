package com.hydrogarden.business.device.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import org.hibernate.validator.constraints.time.DurationMin;

import java.time.Duration;

@Embeddable
@Getter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class DeviceConfig {
    @Column(nullable = false)
    @DurationMin(seconds = 1)
    private Duration standbyTimeout;

    @Column(nullable = false)
    @DurationMin(seconds = 1)
    private Duration heartbeatInterval;
}
