package com.hydrogarden.business.device.app.mapper;

import com.hydrogarden.business.device.core.vo.CircuitId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface CircuitIdMapper {
    CircuitIdMapper INSTANCE = Mappers.getMapper(CircuitIdMapper.class);

    @Mapping(target = ".", source = "id")
    String toString(CircuitId circuitId);

}
