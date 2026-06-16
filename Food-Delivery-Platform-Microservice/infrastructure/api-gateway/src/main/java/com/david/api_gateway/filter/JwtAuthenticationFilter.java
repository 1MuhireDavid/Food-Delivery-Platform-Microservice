package com.david.api_gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // Paths that are public regardless of HTTP method
    private static final List<String> ALWAYS_PUBLIC = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/actuator/**"
    );

    // Paths that are public for GET requests only
    private static final List<String> GET_PUBLIC = List.of(
            "/api/restaurants/search/**",
            "/api/restaurants/*/menu",
            "/api/restaurants/*"
    );

    @Override
    public int getOrder() {
        return -1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        HttpMethod method = exchange.getRequest().getMethod();

        if (isPublicPath(path, method)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);
        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String username   = claims.getSubject();
        String role       = claims.get("role", String.class);
        Object customerIdClaim = claims.get("customerId");

        ServerHttpRequest.Builder mutate = exchange.getRequest().mutate()
                .header("X-Username",  username)
                .header("X-User-Role", role != null ? role : "")
                .headers(h -> h.remove("Authorization"));

        if (customerIdClaim != null) {
            mutate.header("X-Customer-Id", customerIdClaim.toString());
        }

        ServerHttpRequest mutatedRequest = mutate.build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private boolean isPublicPath(String path, HttpMethod method) {
        if (ALWAYS_PUBLIC.stream().anyMatch(p -> pathMatcher.match(p, path))) {
            return true;
        }
        return HttpMethod.GET.equals(method)
                && GET_PUBLIC.stream().anyMatch(p -> pathMatcher.match(p, path));
    }
}
