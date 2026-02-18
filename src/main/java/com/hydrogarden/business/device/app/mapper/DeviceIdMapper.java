package com.hydrogarden.business.device.app.mapper;

import com.hydrogarden.business.common.vo.DeviceId;
import com.hydrogarden.business.device.core.vo.CircuitId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface DeviceIdMapper {
    DeviceIdMapper INSTANCE = Mappers.getMapper(DeviceIdMapper.class);

    default String toString(DeviceId deviceId) {
        return deviceId == null ? null : String.valueOf(deviceId.getId());
    }

}
