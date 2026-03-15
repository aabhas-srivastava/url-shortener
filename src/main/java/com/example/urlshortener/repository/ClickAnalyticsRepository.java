package com.example.urlshortener.repository;

import com.example.urlshortener.model.ClickAnalytics;
import com.example.urlshortener.model.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClickAnalyticsRepository extends JpaRepository<ClickAnalytics, Long> {

    List<ClickAnalytics> findByUrlMapping(UrlMapping urlMapping);

    long countByUrlMapping(UrlMapping urlMapping);
}
