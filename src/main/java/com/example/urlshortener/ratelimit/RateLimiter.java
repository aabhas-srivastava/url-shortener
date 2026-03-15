package com.example.urlshortener.ratelimit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimiter {

    @Value("${app.rate-limit.requests-per-minute:10}")
    private int maxRequestsPerMinute;

    private final ConcurrentHashMap<String, long[]> requestMap = new ConcurrentHashMap<>();

    public boolean isAllowed(String ipAddress) {
        long currentSecond = Instant.now().getEpochSecond();
        long windowStart = currentSecond - 60;

        requestMap.compute(ipAddress, (ip, data) -> {
            if (data == null || data[1] < windowStart) {
                return new long[]{1, currentSecond};
            } else {
                data[0]++;
                return data;
            }
        });

        long[] data = requestMap.get(ipAddress);
        return data[0] <= maxRequestsPerMinute;
    }

    public int getRemainingRequests(String ipAddress) {
        long[] data = requestMap.get(ipAddress);
        if (data == null) return maxRequestsPerMinute;
        long windowStart = Instant.now().getEpochSecond() - 60;
        if (data[1] < windowStart) return maxRequestsPerMinute;
        return (int) Math.max(0, maxRequestsPerMinute - data[0]);
    }

    @Scheduled(fixedDelay = 120_000)
    public void cleanUp() {
        long windowStart = Instant.now().getEpochSecond() - 60;
        requestMap.entrySet().removeIf(entry -> entry.getValue()[1] < windowStart);
    }
}
