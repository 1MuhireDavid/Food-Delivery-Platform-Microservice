package com.fooddelivery.orderservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE        = "order.exchange";
    public static final String PLACED_QUEUE    = "order.placed.queue";
    public static final String CANCELLED_QUEUE = "order.cancelled.queue";
    public static final String PLACED_KEY      = "order.placed";
    public static final String CANCELLED_KEY   = "order.cancelled";

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue orderPlacedQueue() {
        return QueueBuilder.durable(PLACED_QUEUE).build();
    }

    @Bean
    public Queue orderCancelledQueue() {
        return QueueBuilder.durable(CANCELLED_QUEUE).build();
    }

    @Bean
    public Binding placedBinding(Queue orderPlacedQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(orderPlacedQueue).to(orderExchange).with(PLACED_KEY);
    }

    @Bean
    public Binding cancelledBinding(Queue orderCancelledQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(orderCancelledQueue).to(orderExchange).with(CANCELLED_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
