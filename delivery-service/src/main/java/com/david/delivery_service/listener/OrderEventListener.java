package com.david.delivery_service.listener;

import com.david.delivery_service.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {

    private final DeliveryService deliveryService;

    public OrderEventListener(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @RabbitListener(queues = RabbitMQConfig.ORDER_PLACED_QUEUE)
    public void handleOrderPlaced(OrderPlacedEvent event) {
        deliveryService.createForOrder(
                event.orderId(),
                event.restaurantAddress(),
                event.deliveryAddress()
        );
    }
}