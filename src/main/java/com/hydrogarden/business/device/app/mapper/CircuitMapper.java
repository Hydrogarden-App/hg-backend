package com.hydrogarden.business.device.app.mapper;

import com.hydrogarden.business.device.core.entity.Circuit;
import com.hydrogarden.business.device.core.entity.CircuitId;
import com.hydrogarden.business.device.core.entity.Device;
import com.hydrogarden.model.CircuitVitalsViewModel;
import com.hydrogarden.model.DeviceVitalsViewModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = { })
public interface CircuitMapper {
    CircuitMapper INSTANCE = Mappers.getMapper(CircuitMapper.class);

    @Mapping(target = "id", source = "id.id")
    @Mapping(target = "state", source="state.value")
    @Mapping(target = "desiredState", source="desiredState.value")
    CircuitVitalsViewModel toViewModel(Circuit circuit);
}
