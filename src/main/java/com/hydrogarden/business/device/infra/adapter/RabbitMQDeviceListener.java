package com.hydrogarden.business.device.infra.adapter;

import com.hydrogarden.business.device.app.service.DeviceApplicationService;
import com.hydrogarden.business.device.core.commands.DeviceCommand;
import com.hydrogarden.business.device.core.commands.InboundDeviceCommand;
import com.hydrogarden.business.device.core.commands.OutboundDeviceCommand;
import com.hydrogarden.business.device.infra.factory.DeviceCommandFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMQDeviceListener {

    private final DeviceApplicationService deviceApplicationService;


    @RabbitListener(queues = "toServer")
    public void receiveMessage(byte[] message) {
        try{
            DeviceCommand deviceCommand = DeviceCommandFactory.fromBytes(message);

            if(deviceCommand instanceof OutboundDeviceCommand){
                throw new IllegalStateException("Received Outbound Command! This should really not happen...");
            }

            log.info("Received message: {}", deviceCommand.getCommandType().name());
            deviceApplicationService.handleDeviceCommand((InboundDeviceCommand) deviceCommand);
        } catch (Exception e) {
            log.error("Error while handling device command from amqp", e);
        }

    }
}
