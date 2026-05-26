package com.fooddelivery.orderservice;

import com.fooddelivery.orderservice.client.CustomerClient;
import com.fooddelivery.orderservice.client.RestaurantClient;
import com.fooddelivery.orderservice.dto.client.CustomerInfo;
import com.fooddelivery.orderservice.dto.client.MenuItemInfo;
import com.fooddelivery.orderservice.dto.client.RestaurantInfo;
import com.fooddelivery.orderservice.dto.request.OrderItemRequest;
import com.fooddelivery.orderservice.dto.request.PlaceOrderRequest;
import com.fooddelivery.orderservice.dto.response.OrderResponse;
import com.fooddelivery.orderservice.exception.UnauthorizedException;
import com.fooddelivery.orderservice.repository.OrderRepository;
import com.fooddelivery.orderservice.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class OrderServiceIntegrationTest {

    @Autowired private OrderService orderService;
    @Autowired private OrderRepository orderRepository;

    @MockitoBean private CustomerClient customerClient;
    @MockitoBean private RestaurantClient restaurantClient;
    @MockitoBean private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();

        CustomerInfo customer = new CustomerInfo();
        customer.setId(1L);
        customer.setUsername("john");
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setDeliveryAddress("123 Main St");

        RestaurantInfo restaurant = new RestaurantInfo();
        restaurant.setId(10L);
        restaurant.setName("Pizza Palace");
        restaurant.setAddress("456 Pizza Ave");
        restaurant.setActive(true);
        restaurant.setEstimatedDeliveryMinutes(30);

        MenuItemInfo menuItem = new MenuItemInfo();
        menuItem.setId(100L);
        menuItem.setName("Margherita Pizza");
        menuItem.setPrice(new BigDecimal("12.99"));
        menuItem.setAvailable(true);
        menuItem.setRestaurantId(10L);

        when(customerClient.getById(1L)).thenReturn(customer);
        when(restaurantClient.getById(10L)).thenReturn(restaurant);
        when(restaurantClient.getMenuItemById(100L)).thenReturn(menuItem);
    }

    @Test
    void placeOrder_storesSnapshotData() {
        OrderResponse response = orderService.placeOrder(1L, buildRequest());

        assertThat(response.getStatus()).isEqualTo("PLACED");
        assertThat(response.getCustomerId()).isEqualTo(1L);
        assertThat(response.getRestaurantId()).isEqualTo(10L);
        assertThat(response.getTotalAmount()).isEqualByComparingTo("25.98");
        assertThat(response.getCustomerName()).isEqualTo("John Doe");
        assertThat(response.getRestaurantName()).isEqualTo("Pizza Palace");
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getItemName()).isEqualTo("Margherita Pizza");
        assertThat(response.getItems().get(0).getUnitPrice()).isEqualByComparingTo("12.99");
    }

    @Test
    void placeOrder_failsWhenRestaurantInactive() {
        RestaurantInfo inactive = new RestaurantInfo();
        inactive.setId(10L);
        inactive.setActive(false);
        when(restaurantClient.getById(10L)).thenReturn(inactive);

        assertThatThrownBy(() -> orderService.placeOrder(1L, buildRequest()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not accepting orders");
    }

    @Test
    void cancelOrder_succeedsForOwner() {
        OrderResponse placed = orderService.placeOrder(1L, buildRequest());
        OrderResponse cancelled = orderService.cancelOrder(placed.getId(), 1L);

        assertThat(cancelled.getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void cancelOrder_throwsUnauthorizedForOtherCustomer() {
        OrderResponse placed = orderService.placeOrder(1L, buildRequest());

        assertThatThrownBy(() -> orderService.cancelOrder(placed.getId(), 99L))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("your own orders");
    }

    private PlaceOrderRequest buildRequest() {
        OrderItemRequest item = new OrderItemRequest();
        item.setMenuItemId(100L);
        item.setQuantity(2);

        PlaceOrderRequest req = new PlaceOrderRequest();
        req.setRestaurantId(10L);
        req.setItems(List.of(item));
        return req;
    }
}
