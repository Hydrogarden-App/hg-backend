package com.hydrogarden.business.system_log.app.listener;

import com.hydrogarden.business.device.core.event.RegisteredDeviceStartDE;
import com.hydrogarden.business.system_log.app.service.SystemLogApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SystemLogDomainEventListener {

    private final SystemLogApplicationService systemLogApplicationService;


    @EventListener()
    public void receiveDomainEvent(RegisteredDeviceStartDE message) {

    }
}
