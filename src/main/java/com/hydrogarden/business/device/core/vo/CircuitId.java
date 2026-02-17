package com.hydrogarden.business.device.core.vo;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class CircuitId implements Serializable {
    private Short id;

    public CircuitId(String circuitId) {
        this.id = Short.valueOf(circuitId);
    }
}
