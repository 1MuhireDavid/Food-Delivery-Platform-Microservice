package com.david.restaurant_service.service;

import com.david.restaurant_service.client.CustomerClient;
import com.david.restaurant_service.dto.*;
import com.david.restaurant_service.exception.ResourceNotFoundException;
import com.david.restaurant_service.exception.UnauthorizedException;
import com.david.restaurant_service.model.MenuItem;
import com.david.restaurant_service.model.Restaurant;
import com.david.restaurant_service.repository.MenuItemRepository;
import com.david.restaurant_service.repository.RestaurantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final CustomerClient customerClient;

    public RestaurantService(RestaurantRepository restaurantRepository,
                             MenuItemRepository menuItemRepository,
                             CustomerClient customerClient) {
        this.restaurantRepository = restaurantRepository;
        this.menuItemRepository   = menuItemRepository;
        this.customerClient       = customerClient;
    }

    @Transactional
    public RestaurantResponse createRestaurant(String ownerUsername, RestaurantRequest request) {
        Restaurant restaurant = Restaurant.builder()
                .name(request.getName())
                .description(request.getDescription())
                .cuisineType(request.getCuisineType())
                .address(request.getAddress())
                .city(request.getCity())
                .phone(request.getPhone())
                .estimatedDeliveryMinutes(request.getEstimatedDeliveryMinutes())
                .ownerUsername(ownerUsername)
                .build();

        RestaurantResponse response = RestaurantResponse.fromEntity(
                restaurantRepository.save(restaurant));

        customerClient.promoteToRestaurantOwner(ownerUsername);

        return response;
    }

    @Transactional(readOnly = true)
    public RestaurantResponse getById(Long id) {
        return RestaurantResponse.fromEntity(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public List<RestaurantResponse> searchByCity(String city) {
        return restaurantRepository.findByCityIgnoreCaseAndActiveTrue(city)
                .stream().map(RestaurantResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<RestaurantResponse> searchByCuisine(String cuisineType) {
        return restaurantRepository.findByCuisineTypeIgnoreCaseAndActiveTrue(cuisineType)
                .stream().map(RestaurantResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<RestaurantResponse> getAllActive() {
        return restaurantRepository.findByActiveTrue()
                .stream().map(RestaurantResponse::fromEntity).toList();
    }

    @Transactional
    public MenuItemResponse addMenuItem(Long restaurantId, String ownerUsername,
                                        MenuItemRequest request) {
        Restaurant restaurant = findEntityById(restaurantId);
        if (!restaurant.getOwnerUsername().equals(ownerUsername))
            throw new UnauthorizedException("You don't own this restaurant");

        MenuItem item = MenuItem.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(request.getCategory())
                .imageUrl(request.getImageUrl())
                .restaurant(restaurant)
                .build();

        return MenuItemResponse.fromEntity(menuItemRepository.save(item));
    }

    @Transactional(readOnly = true)
    public List<MenuItemResponse> getMenu(Long restaurantId) {
        return menuItemRepository.findByRestaurantIdAndAvailableTrue(restaurantId)
                .stream().map(MenuItemResponse::fromEntity).toList();
    }

    @Transactional
    public MenuItemResponse updateMenuItem(Long itemId, String ownerUsername,
                                           MenuItemRequest request) {
        MenuItem item = findMenuItemEntityById(itemId);
        if (!item.getRestaurant().getOwnerUsername().equals(ownerUsername))
            throw new UnauthorizedException("You don't own this restaurant");

        if (request.getName() != null)        item.setName(request.getName());
        if (request.getDescription() != null) item.setDescription(request.getDescription());
        if (request.getPrice() != null)       item.setPrice(request.getPrice());
        if (request.getCategory() != null)    item.setCategory(request.getCategory());

        return MenuItemResponse.fromEntity(menuItemRepository.save(item));
    }

    @Transactional
    public void toggleMenuItemAvailability(Long itemId, String ownerUsername) {
        MenuItem item = findMenuItemEntityById(itemId);
        if (!item.getRestaurant().getOwnerUsername().equals(ownerUsername))
            throw new UnauthorizedException("You don't own this restaurant");

        item.setAvailable(!item.isAvailable());
        menuItemRepository.save(item);
    }

    public Restaurant findEntityById(Long id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", "id", id));
    }

    public MenuItem findMenuItemEntityById(Long id) {
        return menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", "id", id));
    }
}
