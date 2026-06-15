package com.david.api_gateway.filter;

import io.github.resilience4j.ratelimiter.RateLimiter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class OrderRateLimiterFilter implements GlobalFilter, Ordered {

    private final RateLimiter orderPlacementRateLimiter;

    public OrderRateLimiterFilter(RateLimiter orderPlacementRateLimiter) {
        this.orderPlacementRateLimiter = orderPlacementRateLimiter;
    }

    @Override
    public int getOrder() {
        // Runs after JWT filter (-1) so the user is already authenticated
        return 0;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!isOrderPlacement(exchange)) {
            return chain.filter(exchange);
        }

        if (!orderPlacementRateLimiter.acquirePermission()) {
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders().add("Retry-After", "1");
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }

    private boolean isOrderPlacement(ServerWebExchange exchange) {
        return HttpMethod.POST.equals(exchange.getRequest().getMethod())
                && exchange.getRequest().getURI().getPath().startsWith("/api/orders");
    }
}
