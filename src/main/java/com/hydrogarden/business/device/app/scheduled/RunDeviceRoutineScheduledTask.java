package com.hydrogarden.business.device.app.scheduled;


import com.hydrogarden.business.device.app.service.DeviceRoutineApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RunDeviceRoutineScheduledTask {
    private final DeviceRoutineApplicationService deviceRoutineApplicationService;

    @Scheduled(fixedRate = 5_000L)
    public void runDeviceRoutineScheduledTask() {
        deviceRoutineApplicationService.runDeviceRoutine();
    }
}
