package com.hydrogarden.business.device.app.mapper;

import com.hydrogarden.business.common.vo.DeviceId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface DeviceIdMapper {
    DeviceIdMapper INSTANCE = Mappers.getMapper(DeviceIdMapper.class);

    @Mapping(target=".", source = "id")
    String toString(DeviceId circuitId);

}
