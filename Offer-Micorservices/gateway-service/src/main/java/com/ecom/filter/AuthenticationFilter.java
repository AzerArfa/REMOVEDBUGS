package com.ecom.filter;

import com.ecom.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);

    @Autowired
    private RouteValidator validator;

    @Autowired
    private JwtUtil jwtUtil;

    public AuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            logger.info("Request Path: " + exchange.getRequest().getPath().toString());

            // Bypass authentication for login, signup, and visitor paths
            if (exchange.getRequest().getPath().toString().startsWith("/auth/login") ||
                exchange.getRequest().getPath().toString().startsWith("/auth/signup") ||
                exchange.getRequest().getPath().toString().startsWith("/offer/visitor")) {
                return chain.filter(exchange);
            }

            if (validator.isSecured.test(exchange.getRequest())) {
                String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                logger.info("Authorization Header: " + authHeader);

                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    try {
                        Claims claims = jwtUtil.extractAllClaims(token);
                        logger.info("Extracted Claims: " + claims);

                        // Handle roles as a String and convert to List
                        String rolesString = claims.get("roles", String.class);
                        List<String> roles = Arrays.asList(rolesString.split(","));
                        logger.info("Roles: " + roles);

                        String requestPath = exchange.getRequest().getPath().toString();
                        boolean isAuthorized = roles.stream().anyMatch(role ->
                            (role.equals("ROLE_ADMIN") && requestPath.startsWith("/offer/admin")) ||
                            (role.equals("ROLE_USER") && requestPath.startsWith("/offer/user")) ||
                            (role.equals("ROLE_SUPERADMIN") && requestPath.startsWith("/offer/superadmin")) ||
                            (role.equals("ROLE_SUPERADMIN") && requestPath.startsWith("/superadmin"))
                        );

                        if (!isAuthorized) {
                            throw new Exception("Unauthorized access to application");
                        }
                    } catch (Exception e) {
                        logger.error("Unauthorized access", e);
                        ServerHttpResponse response = exchange.getResponse();
                        response.setStatusCode(HttpStatus.UNAUTHORIZED);
                        return response.setComplete();
                    }
                } else {
                    logger.error("Authorization header is missing or invalid");
                    ServerHttpResponse response = exchange.getResponse();
                    response.setStatusCode(HttpStatus.UNAUTHORIZED);
                    return response.setComplete();
                }
            }
            return chain.filter(exchange);
        };
    }

    public static class Config {
        // Config class for properties if needed
    }
}
