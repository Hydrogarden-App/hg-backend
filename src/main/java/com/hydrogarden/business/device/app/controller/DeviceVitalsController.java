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
    public ResponseEntity<com.hydrogarden.model.DeviceVitalsViewModel> disableCircuit(String  deviceId, String circuitId) {
        Device device = deviceApplicationService.requestChangeCircuitStatus(new DeviceId(deviceId), new CircuitId(circuitId), CircuitState.OFF);
        DeviceVitalsViewModel viewModel = DeviceMapper.INSTANCE.toDeviceVitals(device);
        return ResponseEntity.ok(viewModel);
    }

    @Override
    public ResponseEntity<com.hydrogarden.model.DeviceVitalsViewModel> disableDevice(String deviceId) {
        Device device = deviceApplicationService.disableDevice(new DeviceId(deviceId));
        DeviceVitalsViewModel viewModel = DeviceMapper.INSTANCE.toDeviceVitals(device);
        return ResponseEntity.ok(viewModel);
    }

    @Override
    public ResponseEntity<com.hydrogarden.model.DeviceVitalsViewModel> enableCircuit(String deviceId, String circuitId) {
        Device device = deviceApplicationService.requestChangeCircuitStatus(new DeviceId(deviceId), new CircuitId(circuitId), CircuitState.ON);
        DeviceVitalsViewModel viewModel = DeviceMapper.INSTANCE.toDeviceVitals(device);
        return ResponseEntity.ok(viewModel);
    }

    @Override
    public ResponseEntity<com.hydrogarden.model.DeviceVitalsViewModel> enableDevice(String deviceId) {
        Device device = deviceApplicationService.enableDevice(new DeviceId(deviceId));
        DeviceVitalsViewModel viewModel = DeviceMapper.INSTANCE.toDeviceVitals(device);
        return ResponseEntity.ok(viewModel);
    }

    @Override
    public ResponseEntity<com.hydrogarden.model.DeviceVitalsViewModel> getDeviceVitals(String deviceId) {
        Device device = deviceApplicationService.getDevice(new DeviceId(deviceId));
        DeviceVitalsViewModel viewModel = DeviceMapper.INSTANCE.toDeviceVitals(device);
        return ResponseEntity.ok(viewModel);
    }
}
