package com.hydrogarden.business.device.infra.repository;

import com.hydrogarden.business.device.core.entity.Device;
import com.hydrogarden.business.device.core.entity.DeviceId;
import org.springframework.data.repository.ListCrudRepository;

public interface DeviceRepository extends ListCrudRepository<Device, DeviceId> {
}
