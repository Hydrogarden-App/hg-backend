package com.hydrogarden.common;

import com.hydrogarden.business.device.core.entity.HydrogardenDomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SpringEventPublisher implements HydrogardenEventPublisher {
    private final ApplicationEventPublisher publisher;

    @Override
    public void publish(HydrogardenDomainEvent event) {
        publisher.publishEvent(event);
    }

    @Override
    public void publish(List<HydrogardenDomainEvent> events) {
        events.forEach(publisher::publishEvent);
    }
}
