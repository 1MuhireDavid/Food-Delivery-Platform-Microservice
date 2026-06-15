package com.david.delivery_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String ORDER_EXCHANGE        = "order.exchange";
    public static final String ORDER_PLACED_QUEUE    = "order.placed.queue";
    public static final String ORDER_PLACED_KEY      = "order.placed";
    public static final String ORDER_CANCELLED_QUEUE = "order.cancelled.queue";
    public static final String ORDER_CANCELLED_KEY   = "order.cancelled";

    public static final String DEAD_LETTER_EXCHANGE  = "order.dlx";
    public static final String DLQ_ORDER_PLACED      = "order.placed.dlq";
    public static final String DLQ_ORDER_CANCELLED   = "order.cancelled.dlq";

    public static final String DELIVERY_EXCHANGE     = "delivery.exchange";
    public static final String DELIVERY_STATUS_QUEUE = "delivery.status.queue";
    public static final String DELIVERY_STATUS_KEY   = "delivery.status";

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange deliveryExchange() {
        return new TopicExchange(DELIVERY_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderPlacedQueue() {
        return QueueBuilder.durable(ORDER_PLACED_QUEUE)
                .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_ORDER_PLACED)
                .build();
    }

    @Bean
    public Queue orderCancelledQueue() {
        return QueueBuilder.durable(ORDER_CANCELLED_QUEUE)
                .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_ORDER_CANCELLED)
                .build();
    }

    @Bean
    public Queue deadLetterOrderPlacedQueue() {
        return QueueBuilder.durable(DLQ_ORDER_PLACED).build();
    }

    @Bean
    public Queue deadLetterOrderCancelledQueue() {
        return QueueBuilder.durable(DLQ_ORDER_CANCELLED).build();
    }

    @Bean
    public Queue deliveryStatusQueue() {
        return QueueBuilder.durable(DELIVERY_STATUS_QUEUE).build();
    }

    @Bean
    public Binding orderPlacedBinding(Queue orderPlacedQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(orderPlacedQueue).to(orderExchange).with(ORDER_PLACED_KEY);
    }

    @Bean
    public Binding orderCancelledBinding(Queue orderCancelledQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(orderCancelledQueue).to(orderExchange).with(ORDER_CANCELLED_KEY);
    }

    @Bean
    public Binding dlqOrderPlacedBinding(Queue deadLetterOrderPlacedQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterOrderPlacedQueue).to(deadLetterExchange).with(DLQ_ORDER_PLACED);
    }

    @Bean
    public Binding dlqOrderCancelledBinding(Queue deadLetterOrderCancelledQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterOrderCancelledQueue).to(deadLetterExchange).with(DLQ_ORDER_CANCELLED);
    }

    @Bean
    public Binding deliveryStatusBinding(Queue deliveryStatusQueue, TopicExchange deliveryExchange) {
        return BindingBuilder.bind(deliveryStatusQueue).to(deliveryExchange).with(DELIVERY_STATUS_KEY);
    }

    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter jacksonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jacksonMessageConverter);
        return template;
    }
}
