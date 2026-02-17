package com.hydrogarden.business.device.app.mapper;

import com.hydrogarden.business.device.core.entity.Circuit;
import com.hydrogarden.model.CircuitInfoViewModel;
import com.hydrogarden.model.CircuitVitalsViewModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = { CircuitIdMapper.class})
public interface CircuitMapper {
    CircuitMapper INSTANCE = Mappers.getMapper(CircuitMapper.class);

    @Mapping(target = "state", source="state.value")
    @Mapping(target = "desiredState", source="desiredState.value")
    CircuitVitalsViewModel toVitals(Circuit circuit);

    CircuitInfoViewModel toInfo(Circuit circuit);
}
