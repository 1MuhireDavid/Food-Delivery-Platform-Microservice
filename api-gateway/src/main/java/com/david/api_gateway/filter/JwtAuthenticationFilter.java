package com.david.api_gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
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

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/restaurants/search/**",
            "/api/restaurants/*/menu",
            "/actuator/**"
    );

    @Override
    public int getOrder() {
        return -1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isPublicPath(path)) {
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

        String username = claims.getSubject();
        String role = claims.get("role", String.class);

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header("X-Username", username)
                .header("X-User-Role", role != null ? role : "")
                .headers(h -> h.remove("Authorization"))
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }
}
