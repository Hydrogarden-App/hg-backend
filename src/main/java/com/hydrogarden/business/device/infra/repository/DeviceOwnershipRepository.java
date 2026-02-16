package com.hydrogarden.business.device.infra.repository;

import com.hydrogarden.business.device.core.entity.DeviceOwnership;
import com.hydrogarden.business.device.core.entity.DeviceOwnershipId;
import com.hydrogarden.common.UserId;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Set;

public interface DeviceOwnershipRepository extends CrudRepository<DeviceOwnership, DeviceOwnershipId> {

    @Query("""
    SELECT d FROM DeviceOwnership d WHERE d.id.ownerId=:ownerId
""")
    Set<DeviceOwnership> findAllByOwnerId(UserId ownerId);
}
