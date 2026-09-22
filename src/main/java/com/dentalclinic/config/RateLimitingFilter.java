package com.dentalclinic.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_WINDOW = 60;
    private static final long WINDOW_DURATION_MS = 10000; // 10s

    private static class RequestCount {
        long windowStart;
        AtomicInteger count;

        RequestCount(long start) {
            this.windowStart = start;
            this.count = new AtomicInteger(1);
        }
    }

    private final ConcurrentHashMap<String, RequestCount> ipRequestMap = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        if (path.startsWith("/api/")) {
            String clientIp = getSanitizedClientIp(request);
            long now = System.currentTimeMillis();

            RequestCount reqCount = ipRequestMap.compute(clientIp, (ip, current) -> {
                if (current == null || (now - current.windowStart) > WINDOW_DURATION_MS) {
                    return new RequestCount(now);
                } else {
                    current.count.incrementAndGet();
                    return current;
                }
            });

            if (reqCount.count.get() > MAX_REQUESTS_PER_WINDOW) {
                response.setStatus(429);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"success\":false,\"message\":\"Phát hiện lưu lượng truy cập bất thường (Spam/DoS). Vui lòng thử lại sau vài giây!\",\"statusCode\":429}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Tự động dọn dẹp bộ nhớ mỗi 60 giây để chống Memory Leak
     */
    @Scheduled(fixedRate = 60000)
    public void cleanupExpiredEntries() {
        long now = System.currentTimeMillis();
        ipRequestMap.entrySet().removeIf(entry -> (now - entry.getValue().windowStart) > (WINDOW_DURATION_MS * 2));
    }

    private String getSanitizedClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isBlank()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
