package com.hydrogarden.business.common.vo;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class DeviceId implements Serializable {
    private Short id;

    public DeviceId(String deviceId) {
        this.id = Short.valueOf(deviceId);
    }
}
