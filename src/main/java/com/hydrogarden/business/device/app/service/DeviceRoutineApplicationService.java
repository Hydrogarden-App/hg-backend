package com.hydrogarden.business.device.app.service;

/**
 * Application Service interface for running device routine tasks.
 */
public interface DeviceRoutineApplicationService {

    /**
     * Periodically evaluate all devices, send commands if necessary,
     * and acknowledge execution.
     */
    void runDeviceRoutine();
}
