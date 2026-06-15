package com.david.delivery_service.listener;

import com.david.delivery_service.config.RabbitMQConfig;
import com.david.delivery_service.event.OrderCancelledEvent;
import com.david.delivery_service.event.OrderPlacedEvent;
import com.david.delivery_service.exception.ResourceNotFoundException;
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

    @RabbitListener(queues = RabbitMQConfig.ORDER_PLACED_QUEUE)
    public void handleOrderPlaced(OrderPlacedEvent event) {
        log.info("Received OrderPlacedEvent for orderId={}", event.orderId());
        deliveryService.createForOrder(event);
    }

    @RabbitListener(queues = RabbitMQConfig.ORDER_CANCELLED_QUEUE)
    public void handleOrderCancelled(OrderCancelledEvent event) {
        log.info("Received OrderCancelledEvent for orderId={}", event.orderId());
        try {
            deliveryService.cancelDelivery(event.orderId());
            log.info("Delivery cancelled for orderId={}", event.orderId());
        } catch (ResourceNotFoundException e) {
            // Delivery may not exist yet if the order was cancelled very quickly
            log.warn("No delivery found for cancelled orderId={}, nothing to cancel", event.orderId());
        } catch (IllegalStateException e) {
            log.warn("Cannot cancel delivery for orderId={}: {}", event.orderId(), e.getMessage());
        }
    }
}
