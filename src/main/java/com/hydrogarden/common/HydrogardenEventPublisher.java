package com.hydrogarden.common;


import com.hydrogarden.business.common.event.HydrogardenDomainEvent;

import java.util.List;

public interface HydrogardenEventPublisher {
    void publish(HydrogardenDomainEvent event);
    void publish(List<HydrogardenDomainEvent> events);
}
