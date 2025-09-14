package com.hydrogarden.business.device.core.entity;


import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Log4j2
public abstract class HydrogardenAgreggateRoot {

    private final List<HydrogardenDomainEvent> domainEvents = new ArrayList<>();

    public List<HydrogardenDomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }

    protected void registerDomainEvent(HydrogardenDomainEvent event) {
        log.info("Registering domain event: {}", ReflectionToStringBuilder.toString(this));
        domainEvents.add(event);
    }
}
