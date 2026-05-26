package com.david.delivery_service.listener;

import com.david.delivery_service.config.RabbitMQConfig;
import com.david.delivery_service.event.OrderPlacedEvent;
import com.david.delivery_service.service.DeliveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private final DeliveryService deliveryService;

    public OrderEventListener(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void handleOrderPlaced(OrderPlacedEvent event) {
        log.info("Received OrderPlacedEvent for orderId={}", event.orderId());
        deliveryService.createForOrder(event);
    }
}
