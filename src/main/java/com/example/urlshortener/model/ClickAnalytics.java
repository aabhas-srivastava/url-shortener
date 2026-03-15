package com.example.urlshortener.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "click_analytics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClickAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "url_mapping_id", nullable = false)
    private UrlMapping urlMapping;

    @Column(nullable = false)
    private String ipAddress;

    @Column
    private String userAgent;

    @Column
    private String referer;

    @Column(nullable = false)
    private LocalDateTime clickedAt;

    @PrePersist
    public void prePersist() {
        this.clickedAt = LocalDateTime.now();
    }
}
