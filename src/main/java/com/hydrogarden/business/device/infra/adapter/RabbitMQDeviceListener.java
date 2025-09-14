package com.hydrogarden.business.device.infra.adapter;

import com.hydrogarden.business.device.app.service.DeviceApplicationService;
import com.hydrogarden.business.device.core.commands.DeviceCommand;
import com.hydrogarden.business.device.infra.factory.DeviceCommandFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMQDeviceListener {

    private final DeviceApplicationService deviceApplicationService;


    @RabbitListener(queues = "toServer")
    public void receiveMessage(byte[] message) {
        DeviceCommand deviceCommand = DeviceCommandFactory.fromBytes(message);
        try{
            deviceApplicationService.handleDeviceCommand(deviceCommand);
        } catch (Exception e) {
            log.error("Error while handling device command from amqp", e);
        }

        log.info("Received message: {}", deviceCommand.getCommandType().name());
    }
}
