package com.themiya.shortener.dto;

import java.time.OffsetDateTime;

public class LinkResponse {
    private Long id;
    private String slug;
    private String originalUrl;
    private String shortUrl;
    private long clickCount;
    private OffsetDateTime createdAt;

    public static LinkResponse of(Long id, String slug, String originalUrl, String shortUrl, long clickCount, OffsetDateTime createdAt) {
        LinkResponse response = new LinkResponse();
        response.id = id;
        response.slug = slug;
        response.originalUrl = originalUrl;
        response.shortUrl = shortUrl;
        response.clickCount = clickCount;
        response.createdAt = createdAt;
        return response;
    }

    public Long getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public String getShortUrl() {
        return shortUrl;
    }

    public long getClickCount() {
        return clickCount;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
