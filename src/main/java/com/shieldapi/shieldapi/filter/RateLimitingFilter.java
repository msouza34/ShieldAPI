package com.shieldapi.shieldapi.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shieldapi.shieldapi.redis.RateLimitResult;
import com.shieldapi.shieldapi.security.JwtService;
import com.shieldapi.shieldapi.service.RateLimiterService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimitingFilter.class);

    private final RateLimiterService rateLimiterService;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public RateLimitingFilter(RateLimiterService rateLimiterService, JwtService jwtService, ObjectMapper objectMapper) {
        this.rateLimiterService = rateLimiterService;
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return "/error".equals(uri)
                || "/".equals(uri)
                || "/favicon.ico".equals(uri)
                || "/swagger-ui.html".equals(uri)
                || uri.startsWith("/swagger-ui/")
                || uri.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String ip = resolveClientIp(request);
        String identityKey = resolveIdentityKey(request, ip);

        RateLimitResult result = rateLimiterService.check(identityKey);

        if (!result.allowed()) {
            long retryAfter = Math.max(1, result.retryAfterSeconds());
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Retry-After", String.valueOf(retryAfter));

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("timestamp", Instant.now());
            body.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
            body.put("error", HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase());
            body.put("message", result.banned() ? "Temporarily banned due to repeated limit violations" : "Rate limit exceeded");
            body.put("identity", identityKey);
            body.put("path", request.getRequestURI());

            LOGGER.warn(
                    "request_blocked reason={} identity={} ip={} path={} method={} timestamp={}",
                    result.reason(),
                    identityKey,
                    ip,
                    request.getRequestURI(),
                    request.getMethod(),
                    Instant.now()
            );

            objectMapper.writeValue(response.getWriter(), body);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveIdentityKey(HttpServletRequest request, String ip) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtService.isTokenValid(token)) {
                String username = jwtService.extractUsername(token);
                return "user:" + username;
            }
        }

        return "ip:" + ip;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }

        return request.getRemoteAddr();
    }
}
