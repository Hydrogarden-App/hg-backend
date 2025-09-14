package com.hydrogarden.business.device.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@NoArgsConstructor(force = true)
@RequiredArgsConstructor
@EqualsAndHashCode
public class CircuitState {
    public static final CircuitState OFF = new CircuitState(false);
    public static final CircuitState ON = new CircuitState(true);
    @Column
    private final Boolean value;

}
