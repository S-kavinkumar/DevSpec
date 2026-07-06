package com.devspec.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitingFilter implements Filter {
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    private static class TokenBucket {
        private final int limit = 120; // 120 requests per minute per IP
        private final long timeWindow = TimeUnit.MINUTES.toMillis(1);
        private long lastRefillTime = System.currentTimeMillis();
        private final AtomicInteger tokens = new AtomicInteger(limit);

        public synchronized boolean tryConsume() {
            long now = System.currentTimeMillis();
            if (now - lastRefillTime > timeWindow) {
                tokens.set(limit);
                lastRefillTime = now;
            }
            if (tokens.get() > 0) {
                tokens.decrementAndGet();
                return true;
            }
            return false;
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Skip rate limits on H2 console or external downloads
        String path = httpRequest.getRequestURI();
        if (path.contains("/h2-console") || path.contains("/api/projects/download")) {
            chain.doFilter(request, response);
            return;
        }

        String ip = httpRequest.getRemoteAddr();
        TokenBucket bucket = buckets.computeIfAbsent(ip, k -> new TokenBucket());

        if (!bucket.tryConsume()) {
            httpResponse.setStatus(429); // Too Many Requests
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"error\": \"Too many requests. Please wait a moment before trying again.\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}
