package com.david.delivery_service.service;

import com.david.delivery_service.config.RabbitMQConfig;
import com.david.delivery_service.dto.DeliveryResponse;
import com.david.delivery_service.event.DeliveryStatusUpdatedEvent;
import com.david.delivery_service.event.OrderPlacedEvent;
import com.david.delivery_service.exception.ResourceNotFoundException;
import com.david.delivery_service.model.Delivery;
import com.david.delivery_service.repository.DeliveryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DeliveryService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryService.class);

    private static final String[] DRIVERS = {
        "James Mwangi", "Grace Wanjiku", "Brian Otieno",
        "Aisha Kamau", "Patrick Ndungu"
    };
    private static final String[] PHONES = {
        "+254700111001", "+254700111002", "+254700111003",
        "+254700111004", "+254700111005"
    };

    private final DeliveryRepository deliveryRepository;
    private final RabbitTemplate rabbitTemplate;

    public DeliveryService(DeliveryRepository deliveryRepository, RabbitTemplate rabbitTemplate) {
        this.deliveryRepository = deliveryRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional
    public void createForOrder(OrderPlacedEvent event) {
        if (deliveryRepository.findByOrderId(event.orderId()).isPresent()) {
            log.warn("Delivery already exists for orderId={}, skipping", event.orderId());
            return;
        }

        int idx = (int) (Math.random() * DRIVERS.length);

        Delivery delivery = Delivery.builder()
                .orderId(event.orderId())
                .customerUsername(event.customerUsername())
                .pickupAddress(event.restaurantAddress())
                .deliveryAddress(event.deliveryAddress())
                .driverName(DRIVERS[idx])
                .driverPhone(PHONES[idx])
                .status(Delivery.DeliveryStatus.ASSIGNED)
                .assignedAt(LocalDateTime.now())
                .build();

        deliveryRepository.save(delivery);
        log.info("Delivery created for orderId={}, driver={}", event.orderId(), DRIVERS[idx]);
    }

    @Transactional(readOnly = true)
    public DeliveryResponse getById(Long id) {
        return DeliveryResponse.fromEntity(findEntity(id));
    }

    @Transactional(readOnly = true)
    public DeliveryResponse getByOrderId(Long orderId) {
        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery", "orderId", orderId));
        return DeliveryResponse.fromEntity(delivery);
    }

    @Transactional(readOnly = true)
    public List<DeliveryResponse> getByStatus(String status) {
        Delivery.DeliveryStatus ds = Delivery.DeliveryStatus.valueOf(status.toUpperCase());
        return deliveryRepository.findByStatus(ds)
                .stream().map(DeliveryResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<DeliveryResponse> getMyDeliveries(String customerUsername) {
        return deliveryRepository.findByCustomerUsername(customerUsername)
                .stream().map(DeliveryResponse::fromEntity).toList();
    }

    @Transactional
    public DeliveryResponse updateStatus(Long id, String status) {
        Delivery delivery = findEntity(id);
        Delivery.DeliveryStatus newStatus = Delivery.DeliveryStatus.valueOf(status.toUpperCase());

        delivery.setStatus(newStatus);

        switch (newStatus) {
            case PICKED_UP -> delivery.setPickedUpAt(LocalDateTime.now());
            case DELIVERED -> delivery.setDeliveredAt(LocalDateTime.now());
            default -> {}
        }

        Delivery saved = deliveryRepository.save(delivery);

        if (newStatus == Delivery.DeliveryStatus.PICKED_UP
                || newStatus == Delivery.DeliveryStatus.DELIVERED) {
            DeliveryStatusUpdatedEvent event = new DeliveryStatusUpdatedEvent(
                    saved.getId(),
                    saved.getOrderId(),
                    newStatus.name(),
                    saved.getDriverName(),
                    Instant.now()
            );
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.DELIVERY_EXCHANGE,
                    RabbitMQConfig.DELIVERY_STATUS_KEY,
                    event
            );
            log.info("Published DeliveryStatusUpdatedEvent for orderId={}, status={}",
                    saved.getOrderId(), newStatus);
        }

        return DeliveryResponse.fromEntity(saved);
    }

    @Transactional
    public DeliveryResponse cancelDelivery(Long orderId) {
        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery", "orderId", orderId));

        if (delivery.getStatus() == Delivery.DeliveryStatus.DELIVERED)
            throw new IllegalStateException("Cannot cancel a completed delivery");

        delivery.setStatus(Delivery.DeliveryStatus.CANCELLED);
        return DeliveryResponse.fromEntity(deliveryRepository.save(delivery));
    }

    private Delivery findEntity(Long id) {
        return deliveryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery", "id", id));
    }
}
