package com.hydrogarden.business.device.app.controller;


import com.hydrogarden.api.DeviceVitalsApi;
import com.hydrogarden.business.device.app.mapper.DeviceMapper;
import com.hydrogarden.business.device.app.service.DeviceApplicationService;
import com.hydrogarden.business.device.core.entity.CircuitId;
import com.hydrogarden.business.device.core.entity.CircuitState;
import com.hydrogarden.business.device.core.entity.Device;
import com.hydrogarden.business.device.core.entity.DeviceId;
import com.hydrogarden.model.DeviceVitalsViewModel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class DeviceVitalsController implements DeviceVitalsApi {
    private final DeviceApplicationService deviceApplicationService;

    @Override
    public ResponseEntity<com.hydrogarden.model.DeviceVitalsViewModel> disableCircuit(Integer deviceId, Integer circuitId) {
        Device device = deviceApplicationService.requestChangeCircuitStatus(new DeviceId((short) deviceId.intValue()), new CircuitId((short) circuitId.intValue()), CircuitState.OFF);
        DeviceVitalsViewModel viewModel = DeviceMapper.INSTANCE.toViewModel(device);
        return ResponseEntity.ok(viewModel);
    }

    @Override
    public ResponseEntity<com.hydrogarden.model.DeviceVitalsViewModel> disableDevice(Integer deviceId) {
        Device device = deviceApplicationService.disableDevice(new DeviceId((short) deviceId.intValue()));
        DeviceVitalsViewModel viewModel = DeviceMapper.INSTANCE.toViewModel(device);
        return ResponseEntity.ok(viewModel);
    }

    @Override
    public ResponseEntity<com.hydrogarden.model.DeviceVitalsViewModel> enableCircuit(Integer deviceId, Integer circuitId) {
        Device device = deviceApplicationService.requestChangeCircuitStatus(new DeviceId((short) deviceId.intValue()), new CircuitId((short) circuitId.intValue()), CircuitState.ON);
        DeviceVitalsViewModel viewModel = DeviceMapper.INSTANCE.toViewModel(device);
        return ResponseEntity.ok(viewModel);
    }

    @Override
    public ResponseEntity<com.hydrogarden.model.DeviceVitalsViewModel> enableDevice(Integer deviceId) {
        Device device = deviceApplicationService.enableDevice(new DeviceId((short) deviceId.intValue()));
        DeviceVitalsViewModel viewModel = DeviceMapper.INSTANCE.toViewModel(device);
        return ResponseEntity.ok(viewModel);
    }

    @Override
    public ResponseEntity<com.hydrogarden.model.DeviceVitalsViewModel> getDeviceVitals(Integer deviceId) {
        Device device = deviceApplicationService.getDevice(new DeviceId((short) deviceId.intValue()));
        DeviceVitalsViewModel viewModel = DeviceMapper.INSTANCE.toViewModel(device);
        return ResponseEntity.ok(viewModel);
    }
}
