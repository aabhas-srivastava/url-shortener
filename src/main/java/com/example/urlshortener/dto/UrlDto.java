package com.example.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.validator.constraints.URL;
import java.time.LocalDateTime;

public class UrlDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRequest {

        @NotBlank(message = "Original URL must not be blank")
        @URL(message = "Must be a valid URL")
        private String originalUrl;

        @Size(min = 3, max = 10, message = "Custom code must be between 3 and 10 characters")
        @Pattern(regexp = "^[a-zA-Z0-9]*$", message = "Custom code must be alphanumeric")
        private String customCode;

        private LocalDateTime expiresAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private String shortCode;
        private String shortUrl;
        private String originalUrl;
        private LocalDateTime createdAt;
        private LocalDateTime expiresAt;
        private Long clickCount;
        private boolean active;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AnalyticsResponse {
        private String shortCode;
        private String originalUrl;
        private Long totalClicks;
        private LocalDateTime createdAt;
        private LocalDateTime expiresAt;
        private boolean active;
    }
}
