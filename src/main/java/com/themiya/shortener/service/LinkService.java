package com.themiya.shortener.service;

import com.themiya.shortener.dto.LinkResponse;
import com.themiya.shortener.entity.Link;
import com.themiya.shortener.exception.BadRequestException;
import com.themiya.shortener.exception.NotFoundException;
import com.themiya.shortener.repository.LinkRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Duration;
import java.util.List;

@Service
public class LinkService {
    private final LinkRepository linkRepository;
    private final SlugService slugService;
    private final StringRedisTemplate redisTemplate;
    private final String shortBaseUrl;
    private final long cacheTtlSeconds;

    public LinkService(LinkRepository linkRepository,
                       SlugService slugService,
                       StringRedisTemplate redisTemplate,
                       @Value("${app.short-base-url}") String shortBaseUrl,
                       @Value("${app.cache-ttl-seconds}") long cacheTtlSeconds) {
        this.linkRepository = linkRepository;
        this.slugService = slugService;
        this.redisTemplate = redisTemplate;
        this.shortBaseUrl = shortBaseUrl;
        this.cacheTtlSeconds = cacheTtlSeconds;
    }

    @Transactional
    public LinkResponse createShortLink(String originalUrl) {
        validateUrl(originalUrl);

        String slug = generateUniqueSlug();
        Link link = new Link();
        link.setSlug(slug);
        link.setOriginalUrl(originalUrl);

        Link saved = linkRepository.save(link);
        putInCache(saved.getSlug(), saved.getOriginalUrl());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Link resolveBySlug(String slug) {
        String cached = redisTemplate.opsForValue().get(cacheKey(slug));
        if (cached != null && !cached.isBlank()) {
            Link link = new Link();
            link.setSlug(slug);
            link.setOriginalUrl(cached);
            return link;
        }

        Link link = linkRepository.findBySlugAndActiveTrue(slug)
                .orElseThrow(() -> new NotFoundException("Short URL not found"));

        putInCache(slug, link.getOriginalUrl());
        return link;
    }

    @Transactional(readOnly = true)
    public List<LinkResponse> listLinks() {
        return linkRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Link getLinkOrThrow(Long id) {
        return linkRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Link not found"));
    }

    private void putInCache(String slug, String originalUrl) {
        redisTemplate.opsForValue().set(cacheKey(slug), originalUrl, Duration.ofSeconds(cacheTtlSeconds));
    }

    private String generateUniqueSlug() {
        for (int i = 0; i < 10; i++) {
            String slug = slugService.generateSlug();
            if (!linkRepository.existsBySlug(slug)) {
                return slug;
            }
        }
        throw new BadRequestException("Could not generate unique slug, please retry");
    }

    private void validateUrl(String url) {
        try {
            URI uri = URI.create(url);
            if (uri.getScheme() == null || (!uri.getScheme().equalsIgnoreCase("http") && !uri.getScheme().equalsIgnoreCase("https"))) {
                throw new BadRequestException("URL must start with http:// or https://");
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new BadRequestException("URL host is invalid");
            }
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid URL");
        }
    }

    private String cacheKey(String slug) {
        return "slug:" + slug;
    }

    private LinkResponse toResponse(Link link) {
        return LinkResponse.of(
                link.getId(),
                link.getSlug(),
                link.getOriginalUrl(),
                shortBaseUrl + "/" + link.getSlug(),
                link.getClickCount(),
                link.getCreatedAt()
        );
    }
}
