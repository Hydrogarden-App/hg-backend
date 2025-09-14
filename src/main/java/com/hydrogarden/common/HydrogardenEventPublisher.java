package com.hydrogarden.common;


import com.hydrogarden.business.device.core.entity.HydrogardenDomainEvent;

import java.util.List;

public interface HydrogardenEventPublisher {
    void publish(HydrogardenDomainEvent event);
    void publish(List<HydrogardenDomainEvent> events);
}
