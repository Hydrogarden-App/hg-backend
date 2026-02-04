package com.hydrogarden.business.device.core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "device_ownership")
@NoArgsConstructor
@AllArgsConstructor
public class DeviceOwnership {

    @EmbeddedId
    @Getter
    private DeviceOwnershipId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", insertable = false, updatable = false)
    private Device device;
}
