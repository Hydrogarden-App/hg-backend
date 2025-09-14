package com.hydrogarden.business.device.app.mapper;

import com.hydrogarden.business.device.core.entity.Device;
import com.hydrogarden.model.DeviceVitalsViewModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = { CircuitMapper.class})
public interface DeviceMapper {
    DeviceMapper INSTANCE = Mappers.getMapper(DeviceMapper.class);

    @Mapping(target="id", source = "id.id")
    @Mapping(target = "state", source="deviceVitals.state")
    @Mapping(target = "desiredState", source="deviceVitals.desiredState")
    @Mapping(target = "lastKeepAliveSendTime", source="deviceVitals.lastKeepAliveSendTime")
    @Mapping(target = "lastHeartbeatReceiveTime", source="deviceVitals.lastCommandReceiveTime")
    DeviceVitalsViewModel toViewModel(Device device);
}
