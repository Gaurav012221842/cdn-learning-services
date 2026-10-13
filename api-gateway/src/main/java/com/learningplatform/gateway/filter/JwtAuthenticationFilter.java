package com.learningplatform.gateway.filter;

import com.learningplatform.common.constants.SecurityHeaders;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.Key;
import java.util.List;

/**
 * Validates JWT tokens at the API Gateway level and injects trusted identity headers
 * (X-User-Email, X-User-Role, X-User-Id) into downstream microservice requests.
 */
@Component
@Slf4j
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret:default_jwt_secret_key_for_development_environment_only}")
    private String secretKey;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String token = extractToken(request);

        if (token == null || token.isBlank()) {
            // Unauthenticated request, pass through (public endpoints or will be challenged downstream)
            return chain.filter(exchange);
        }

        try {
            Claims claims = validateAndExtractClaims(token);
            String email = claims.getSubject();

            ServerHttpRequest.Builder requestBuilder = request.mutate();
            if (email != null && !email.isBlank()) {
                requestBuilder.header(SecurityHeaders.USER_EMAIL_HEADER, email);
            }

            Object role = claims.get("role");
            if (role != null) {
                requestBuilder.header(SecurityHeaders.USER_ROLE_HEADER, role.toString());
            }

            Object userId = claims.get("userId");
            if (userId != null) {
                requestBuilder.header(SecurityHeaders.USER_ID_HEADER, userId.toString());
            }

            return chain.filter(exchange.mutate().request(requestBuilder.build()).build());
        } catch (Exception e) {
            log.warn("Invalid JWT token detected at gateway: {}", e.getMessage());
            // Allow request to proceed without injected headers; downstream or security will handle
            return chain.filter(exchange);
        }
    }

    private String extractToken(ServerHttpRequest request) {
        // 1. Check Authorization header
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }

        // 2. Fallback to 'token' cookie
        List<HttpCookie> cookies = request.getCookies().get("token");
        if (cookies != null && !cookies.isEmpty()) {
            return cookies.get(0).getValue();
        }

        return null;
    }

    private Claims validateAndExtractClaims(String token) {
        Key key;
        try {
            byte[] keyBytes = Decoders.BASE64.decode(secretKey);
            key = Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception e) {
            key = Keys.hmacShaKeyFor(secretKey.getBytes());
        }

        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
