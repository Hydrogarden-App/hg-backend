package com.hydrogarden.business.device.core.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "circuit")
@NoArgsConstructor
@AllArgsConstructor
public class Circuit {
    @EmbeddedId
    @Getter
    private CircuitId id;

    @Column
    @Getter
    @Setter
    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "state"))
    })
    private CircuitState state;

    @Column
    @Setter
    @Getter
    private String name;

    @Getter
    @Setter
    @Column
    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "desired_state"))
    })
    private CircuitState desiredState;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device;

    public boolean isUnsynchronised() {
        return !desiredState.equals(state);
    }
}
