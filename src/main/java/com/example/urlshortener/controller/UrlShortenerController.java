package com.example.urlshortener.controller;

import com.example.urlshortener.dto.UrlDto;
import com.example.urlshortener.ratelimit.RateLimiter;
import com.example.urlshortener.service.QrCodeService;
import com.example.urlshortener.service.UrlShortenerService;
import com.google.zxing.WriterException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;

@Slf4j
@RestController
@RequiredArgsConstructor
public class UrlShortenerController {

    private final UrlShortenerService urlShortenerService;
    private final QrCodeService qrCodeService;
    private final RateLimiter rateLimiter;

    @PostMapping("/api/urls")
    public ResponseEntity<?> createShortUrl(
            @Valid @RequestBody UrlDto.CreateRequest request,
            HttpServletRequest httpRequest) {

        String ip = getClientIp(httpRequest);
        if (!rateLimiter.isAllowed(ip)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("X-RateLimit-Remaining", "0")
                    .body("Rate limit exceeded. Max " + " requests/minute allowed.");
        }

        UrlDto.Response response = urlShortenerService.createShortUrl(request, ip);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("X-RateLimit-Remaining", String.valueOf(rateLimiter.getRemainingRequests(ip)))
                .body(response);
    }

    @GetMapping("/{shortCode:^(?!index\\.html|favicon\\.ico|static).*$}")
    public ResponseEntity<Void> redirect(
            @PathVariable String shortCode,
            HttpServletRequest request) {

        String originalUrl = urlShortenerService.resolveUrl(shortCode);
        try {
            urlShortenerService.trackClick(shortCode, request);
        } catch (Exception e) {
            log.warn("Failed to track click for {}: {}", shortCode, e.getMessage());
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(originalUrl))
                .build();
    }

    @GetMapping("/api/urls/{shortCode}/analytics")
    public ResponseEntity<UrlDto.AnalyticsResponse> getAnalytics(@PathVariable String shortCode) {
        return ResponseEntity.ok(urlShortenerService.getAnalytics(shortCode));
    }

    @GetMapping("/api/urls/{shortCode}/qr")
    public ResponseEntity<byte[]> getQrCode(@PathVariable String shortCode) throws WriterException, IOException {
        urlShortenerService.resolveUrl(shortCode);
        String shortUrl = "http://localhost:8080/" + shortCode;
        byte[] qrImage = qrCodeService.generateQrCode(shortUrl);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"qr-" + shortCode + ".png\"")
                .body(qrImage);
    }

    @DeleteMapping("/api/urls/{shortCode}")
    public ResponseEntity<String> deactivateUrl(@PathVariable String shortCode) {
        urlShortenerService.deactivateUrl(shortCode);
        return ResponseEntity.ok("Short URL '" + shortCode + "' has been deactivated.");
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
