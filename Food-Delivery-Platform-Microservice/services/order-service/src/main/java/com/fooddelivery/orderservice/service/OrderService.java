package com.fooddelivery.orderservice.service;

import com.fooddelivery.orderservice.client.CustomerClient;
import com.fooddelivery.orderservice.client.RestaurantClient;
import com.fooddelivery.orderservice.config.RabbitMQConfig;
import com.fooddelivery.orderservice.dto.client.CustomerInfo;
import com.fooddelivery.orderservice.dto.client.MenuItemInfo;
import com.fooddelivery.orderservice.dto.client.RestaurantInfo;
import com.fooddelivery.orderservice.dto.event.OrderCancelledEvent;
import com.fooddelivery.orderservice.dto.event.OrderPlacedEvent;
import com.fooddelivery.orderservice.dto.request.OrderItemRequest;
import com.fooddelivery.orderservice.dto.request.PlaceOrderRequest;
import com.fooddelivery.orderservice.dto.response.OrderResponse;
import com.fooddelivery.orderservice.exception.ResourceNotFoundException;
import com.fooddelivery.orderservice.exception.UnauthorizedException;
import com.fooddelivery.orderservice.model.Order;
import com.fooddelivery.orderservice.model.OrderItem;
import com.fooddelivery.orderservice.repository.OrderRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerClient customerClient;
    private final RestaurantClient restaurantClient;
    private final RabbitTemplate rabbitTemplate;

    public OrderService(OrderRepository orderRepository,
                        CustomerClient customerClient,
                        RestaurantClient restaurantClient,
                        RabbitTemplate rabbitTemplate) {
        this.orderRepository = orderRepository;
        this.customerClient = customerClient;
        this.restaurantClient = restaurantClient;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional
    public OrderResponse placeOrder(Long customerId, PlaceOrderRequest request) {
        CustomerInfo customer = customerClient.getById(customerId);
        RestaurantInfo restaurant = restaurantClient.getById(request.getRestaurantId());

        if (!restaurant.isActive()) {
            throw new IllegalStateException("Restaurant is currently not accepting orders");
        }

        String deliveryAddress = request.getDeliveryAddress() != null
                ? request.getDeliveryAddress()
                : customer.getDeliveryAddress();

        Order order = Order.builder()
                .customerId(customerId)
                .restaurantId(request.getRestaurantId())
                .deliveryAddress(deliveryAddress)
                .specialInstructions(request.getSpecialInstructions())
                .estimatedDeliveryTime(
                        LocalDateTime.now().plusMinutes(restaurant.getEstimatedDeliveryMinutes()))
                .build();

        BigDecimal total = BigDecimal.ZERO;
        List<OrderPlacedEvent.OrderItemSummary> eventItems = new ArrayList<>();

        for (OrderItemRequest itemReq : request.getItems()) {
            MenuItemInfo menuItem = restaurantClient.getMenuItemById(itemReq.getMenuItemId());

            if (!menuItem.isAvailable()) {
                throw new IllegalStateException(
                        "Menu item '" + menuItem.getName() + "' is not available");
            }
            if (!menuItem.getRestaurantId().equals(request.getRestaurantId())) {
                throw new IllegalStateException(
                        "Menu item '" + menuItem.getName() + "' does not belong to the selected restaurant");
            }

            BigDecimal subtotal = menuItem.getPrice()
                    .multiply(BigDecimal.valueOf(itemReq.getQuantity()));

            OrderItem orderItem = OrderItem.builder()
                    .menuItemId(menuItem.getId())
                    .itemName(menuItem.getName())       // price + name snapshot at order time
                    .quantity(itemReq.getQuantity())
                    .unitPrice(menuItem.getPrice())
                    .subtotal(subtotal)
                    .specialInstructions(itemReq.getSpecialInstructions())
                    .order(order)
                    .build();

            order.getItems().add(orderItem);
            total = total.add(subtotal);

            eventItems.add(OrderPlacedEvent.OrderItemSummary.builder()
                    .menuItemId(menuItem.getId())
                    .itemName(menuItem.getName())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(menuItem.getPrice())
                    .build());
        }

        order.setTotalAmount(total);
        Order saved = orderRepository.save(order);

        OrderPlacedEvent event = OrderPlacedEvent.builder()
                .orderId(saved.getId())
                .customerId(customerId)
                .restaurantId(request.getRestaurantId())
                .customerName(customer.getFullName())
                .deliveryAddress(deliveryAddress)
                .restaurantAddress(restaurant.getAddress())
                .totalAmount(total)
                .estimatedDeliveryTime(saved.getEstimatedDeliveryTime())
                .items(eventItems)
                .occurredAt(LocalDateTime.now())
                .build();

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.PLACED_KEY, event);

        OrderResponse response = OrderResponse.fromEntity(saved);
        response.setCustomerName(customer.getFullName());
        response.setRestaurantName(restaurant.getName());
        return response;
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        return OrderResponse.fromEntity(orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId)));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomer(Long customerId) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream().map(OrderResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByRestaurant(Long restaurantId) {
        return orderRepository.findByRestaurantIdOrderByCreatedAtDesc(restaurantId)
                .stream().map(OrderResponse::fromEntity).toList();
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        try {
            order.setStatus(Order.OrderStatus.valueOf(status.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid order status: " + status);
        }
        return OrderResponse.fromEntity(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId, Long customerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (!order.getCustomerId().equals(customerId)) {
            throw new UnauthorizedException("You can only cancel your own orders");
        }
        if (order.getStatus() != Order.OrderStatus.PLACED
                && order.getStatus() != Order.OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Cannot cancel order in status: " + order.getStatus());
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);

        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .orderId(orderId)
                .customerId(customerId)
                .restaurantId(order.getRestaurantId())
                .occurredAt(LocalDateTime.now())
                .build();

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.CANCELLED_KEY, event);

        return OrderResponse.fromEntity(saved);
    }
}
