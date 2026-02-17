package com.hydrogarden.business.device.app.controller;


import com.hydrogarden.api.DeviceInfoApi;
import com.hydrogarden.business.device.app.mapper.DeviceMapper;
import com.hydrogarden.business.device.app.service.DeviceApplicationService;
import com.hydrogarden.business.device.core.entity.Device;
import com.hydrogarden.business.common.vo.UserId;
import com.hydrogarden.security.UserSecurityModel;
import com.hydrogarden.model.DeviceInfoViewModel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class DeviceInfoController implements DeviceInfoApi {

    private final DeviceApplicationService deviceApplicationService;

    @Override
    public ResponseEntity<List<DeviceInfoViewModel>> getAllDeviceInfo() {
        UserSecurityModel user = (UserSecurityModel) SecurityContextHolder.getContext().getAuthentication();
        UserId userId = user.getUserId();
        List<Device> deviceList = this.deviceApplicationService.getDevicesForUser(userId);
        return ResponseEntity.ok(deviceList.stream().map(DeviceMapper.INSTANCE::toDeviceInfo).toList());
    }

    @Override
    public ResponseEntity<List<DeviceInfoViewModel>> updateDeviceInfo(String deviceId) {
        return null;
    }
}
