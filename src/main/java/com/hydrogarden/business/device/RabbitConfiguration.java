package com.hydrogarden.business.device;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfiguration {

    @Bean
    public Queue toServer() {
        return new Queue("toServer");
    }

    @Bean
    public Queue toDevice() {
        return new Queue("toDevice");
    }


    @Bean
    public Binding toDeviceBinding(@Qualifier("toDevice") Queue toDevice) {
        return BindingBuilder
                .bind(toDevice)
                .to(new TopicExchange("amq.topic", true, false))
                .with(toDevice.getName());
    }

    @Bean
    public Binding toServerBinding(@Qualifier("toServer") Queue toDevice) {
        return BindingBuilder
                .bind(toDevice)
                .to(new TopicExchange("amq.topic", true, false))
                .with(toDevice.getName());
    }


/*    @Bean
    public Binding circuitStateRefreshBinding(@Qualifier("CircuitStateRefreshDeviceRequestQueue") Queue CircuitStateRefreshDeviceRequestQueue) {
        return BindingBuilder
                .bind(CircuitStateRefreshDeviceRequestQueue)
                .to(new TopicExchange("amq.topic", true, false))
                .with(CircuitStateRefreshDeviceRequestQueue.getName());
    }

    @Bean
    public Binding logEventBinding(@Qualifier("LogEventDeviceRequestQueue") Queue LogEventDeviceRequestQueue) {
        return BindingBuilder
                .bind(LogEventDeviceRequestQueue)
                .to(new TopicExchange("amq.topic", true, false))
                .with(LogEventDeviceRequestQueue.getName());
    }*/

    @Bean
    public RabbitTemplate rabbitTemplate(CachingConnectionFactory factory) {
        RabbitTemplate template = new RabbitTemplate(factory);
        template.setMandatory(true);

        template.setReturnsCallback((callback) -> {
            throw new RuntimeException(
                    "Message returned! replyCode=" + callback.getReplyCode() + ", replyText=" + callback.getReplyText()
            );
        });

        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                throw new RuntimeException("Message not confirmed! cause=" + cause);
            }
        });

        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(final ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        return factory;
    }


    private static class MessageExpirationAddingPostProcessor implements MessagePostProcessor {
        private Integer ttlMillis;

        public MessageExpirationAddingPostProcessor(int i) {
            this.ttlMillis = i;
        }

        @Override
        public org.springframework.amqp.core.Message postProcessMessage(org.springframework.amqp.core.Message message) throws AmqpException {
            message.getMessageProperties().setExpiration(ttlMillis.toString());
            return message;
        }
    }
}
