package com.hydrogarden.business.device.infra.adapter;

import com.hydrogarden.business.device.core.commands.DeviceCommand;
import com.hydrogarden.business.device.core.port.out.DeviceOutputPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Primary
public class RabbitMQDeviceAdapter implements DeviceOutputPort {
    private final RabbitTemplate rabbitTemplate;
    private final Queue toDevice;

    public RabbitMQDeviceAdapter(RabbitTemplate rabbitTemplate, @Qualifier("toDevice") Queue toDevice, @Qualifier("toServer") Queue toServer) {
        this.rabbitTemplate = rabbitTemplate;
        this.toDevice = toDevice;
    }

    @Override
    public void sendDeviceCommand(DeviceCommand command) {
        rabbitTemplate.invoke(callback -> {
            MessageProperties props = new MessageProperties();
            props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            Message message = MessageBuilder.withBody(command.toBytes()).setContentType(MessageProperties.CONTENT_TYPE_BYTES).build();
            callback.convertAndSend("amq.topic", toDevice.getName(), message);
            callback.waitForConfirmsOrDie(5000);
            return null;
        });

        log.info("Sending device command: {}, {}", command.getCommandType().name(), command.toBytes());
    }


}
