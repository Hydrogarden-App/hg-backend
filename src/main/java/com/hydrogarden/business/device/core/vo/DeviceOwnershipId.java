package com.hydrogarden.business.device.core.vo;

import com.hydrogarden.business.common.vo.DeviceId;
import com.hydrogarden.business.common.vo.UserId;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Getter
public class DeviceOwnershipId implements Serializable {

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "owner_id", nullable = false))
    })
    private UserId ownerId;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "id", column = @Column(name = "device_id", nullable = false))
    })
    private DeviceId deviceId;
}
