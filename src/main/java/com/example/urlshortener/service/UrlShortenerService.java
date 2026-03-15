package com.example.urlshortener.service;

import com.example.urlshortener.algorithm.Base62Encoder;
import com.example.urlshortener.dto.UrlDto;
import com.example.urlshortener.exception.ResourceNotFoundException;
import com.example.urlshortener.exception.ShortCodeConflictException;
import com.example.urlshortener.exception.UrlExpiredException;
import com.example.urlshortener.model.ClickAnalytics;
import com.example.urlshortener.model.UrlMapping;
import com.example.urlshortener.repository.ClickAnalyticsRepository;
import com.example.urlshortener.repository.UrlMappingRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UrlShortenerService {

    private final UrlMappingRepository urlMappingRepository;
    private final ClickAnalyticsRepository clickAnalyticsRepository;
    private final Base62Encoder base62Encoder;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.default-expiry-days:30}")
    private int defaultExpiryDays;

    @Transactional
    public UrlDto.Response createShortUrl(UrlDto.CreateRequest request, String ipAddress) {
        String shortCode;

        if (request.getCustomCode() != null && !request.getCustomCode().isBlank()) {
            shortCode = request.getCustomCode();
            if (urlMappingRepository.existsByShortCode(shortCode)) {
                throw new ShortCodeConflictException("Short code '" + shortCode + "' is already taken.");
            }
        } else {
            shortCode = generateUniqueCode(request.getOriginalUrl());
        }

        LocalDateTime expiresAt = request.getExpiresAt() != null
                ? request.getExpiresAt()
                : LocalDateTime.now().plusDays(defaultExpiryDays);

        UrlMapping urlMapping = UrlMapping.builder()
                .shortCode(shortCode)
                .originalUrl(request.getOriginalUrl())
                .createdBy(ipAddress)
                .expiresAt(expiresAt)
                .clickCount(0L)
                .active(true)
                .build();

        urlMappingRepository.save(urlMapping);
        log.info("Created short URL: {} -> {}", shortCode, request.getOriginalUrl());
        return toResponse(urlMapping);
    }

    @Cacheable(value = "urls", key = "#shortCode")
    public String resolveUrl(String shortCode) {
        UrlMapping urlMapping = urlMappingRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Short URL not found: " + shortCode));

        if (!urlMapping.isActive()) {
            throw new UrlExpiredException("This short URL is no longer active.");
        }
        if (urlMapping.getExpiresAt() != null && urlMapping.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UrlExpiredException("This short URL has expired.");
        }
        return urlMapping.getOriginalUrl();
    }

    @Transactional
    public void trackClick(String shortCode, HttpServletRequest request) {
        urlMappingRepository.findByShortCode(shortCode).ifPresent(urlMapping -> {
            urlMappingRepository.incrementClickCount(shortCode);
            ClickAnalytics analytics = ClickAnalytics.builder()
                    .urlMapping(urlMapping)
                    .ipAddress(getClientIp(request))
                    .userAgent(request.getHeader("User-Agent"))
                    .referer(request.getHeader("Referer"))
                    .build();
            clickAnalyticsRepository.save(analytics);
        });
    }

    public UrlDto.AnalyticsResponse getAnalytics(String shortCode) {
        UrlMapping urlMapping = urlMappingRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Short URL not found: " + shortCode));
        return UrlDto.AnalyticsResponse.builder()
                .shortCode(urlMapping.getShortCode())
                .originalUrl(urlMapping.getOriginalUrl())
                .totalClicks(urlMapping.getClickCount())
                .createdAt(urlMapping.getCreatedAt())
                .expiresAt(urlMapping.getExpiresAt())
                .active(urlMapping.isActive())
                .build();
    }

    @Transactional
    @CacheEvict(value = "urls", key = "#shortCode")
    public void deactivateUrl(String shortCode) {
        UrlMapping urlMapping = urlMappingRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Short URL not found: " + shortCode));
        urlMapping.setActive(false);
        urlMappingRepository.save(urlMapping);
        log.info("Deactivated short URL: {}", shortCode);
    }

    @Scheduled(fixedDelay = 3_600_000)
    @Transactional
    public void deactivateExpiredUrls() {
        int count = urlMappingRepository.deactivateExpiredUrls(LocalDateTime.now());
        if (count > 0) log.info("Deactivated {} expired URLs.", count);
    }

    private String generateUniqueCode(String originalUrl) {
        String code;
        int attempts = 0;
        do {
            code = base62Encoder.generateShortCode(originalUrl + attempts);
            attempts++;
            if (attempts > 10) throw new RuntimeException("Unable to generate unique short code.");
        } while (urlMappingRepository.existsByShortCode(code));
        return code;
    }

    private UrlDto.Response toResponse(UrlMapping urlMapping) {
        return UrlDto.Response.builder()
                .shortCode(urlMapping.getShortCode())
                .shortUrl(baseUrl + "/" + urlMapping.getShortCode())
                .originalUrl(urlMapping.getOriginalUrl())
                .createdAt(urlMapping.getCreatedAt())
                .expiresAt(urlMapping.getExpiresAt())
                .clickCount(urlMapping.getClickCount())
                .active(urlMapping.isActive())
                .build();
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
