package com.themiya.shortener.service;

import com.themiya.shortener.dto.LinkResponse;
import com.themiya.shortener.entity.Link;
import com.themiya.shortener.entity.UserAccount;
import com.themiya.shortener.exception.BadRequestException;
import com.themiya.shortener.exception.NotFoundException;
import com.themiya.shortener.repository.LinkRepository;
import com.themiya.shortener.repository.UserAccountRepository;
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
    private final int anonymousMax;
    private final int accountMax;
    private final UserAccountRepository userAccountRepository;

    public LinkService(LinkRepository linkRepository,
                       SlugService slugService,
                       StringRedisTemplate redisTemplate,
                       UserAccountRepository userAccountRepository,
                       @Value("${app.short-base-url}") String shortBaseUrl,
                       @Value("${app.cache-ttl-seconds}") long cacheTtlSeconds,
                       @Value("${app.link-limits.anonymous-max}") int anonymousMax,
                       @Value("${app.link-limits.account-max}") int accountMax) {
        this.linkRepository = linkRepository;
        this.slugService = slugService;
        this.redisTemplate = redisTemplate;
        this.userAccountRepository = userAccountRepository;
        this.shortBaseUrl = shortBaseUrl;
        this.cacheTtlSeconds = cacheTtlSeconds;
        this.anonymousMax = anonymousMax;
        this.accountMax = accountMax;
    }

    @Transactional
    public LinkResponse createShortLink(String originalUrl, ActorContext actorContext) {
        validateUrl(originalUrl);
        enforceLinkLimit(actorContext);

        String slug = generateUniqueSlug();
        Link link = new Link();
        link.setSlug(slug);
        link.setOriginalUrl(originalUrl);
        if (actorContext.isAuthenticated()) {
            UserAccount user = userAccountRepository.findById(actorContext.userId())
                    .orElseThrow(() -> new NotFoundException("User not found"));
            link.setUser(user);
            link.setOwnerToken(null);
        } else {
            link.setOwnerToken(actorContext.anonymousId());
        }

        Link saved = linkRepository.save(link);
        putInCache(saved.getSlug(), saved.getOriginalUrl());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public String resolveOriginalUrlBySlug(String slug) {
        String cached = redisTemplate.opsForValue().get(cacheKey(slug));
        if (cached != null && !cached.isBlank()) {
            return cached;
        }

        Link link = linkRepository.findBySlugAndActiveTrue(slug)
                .orElseThrow(() -> new NotFoundException("Short URL not found"));

        putInCache(slug, link.getOriginalUrl());
        return link.getOriginalUrl();
    }

    @Transactional(readOnly = true)
    public List<LinkResponse> listLinks(ActorContext actorContext) {
        return findOwnedLinks(actorContext).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Link getOwnedLinkOrThrow(Long id, ActorContext actorContext) {
        if (actorContext.isAuthenticated()) {
            return linkRepository.findByIdAndUserId(id, actorContext.userId())
                    .orElseThrow(() -> new NotFoundException("Link not found"));
        }

        return linkRepository.findByIdAndOwnerTokenAndUserIsNull(id, actorContext.anonymousId())
                .orElseThrow(() -> new NotFoundException("Link not found"));
    }

    @Transactional(readOnly = true)
    public LinkResponse getOwnedLinkResponseOrThrow(Long id, ActorContext actorContext) {
        return toResponse(getOwnedLinkOrThrow(id, actorContext));
    }

    @Transactional
    public void transferAnonymousLinksToUser(String anonymousId, UserAccount user) {
        if (anonymousId == null || anonymousId.isBlank()) {
            return;
        }
        List<Link> links = linkRepository.findAllByOwnerTokenAndUserIsNull(anonymousId);
        for (Link link : links) {
            link.setUser(user);
            link.setOwnerToken(null);
        }
        linkRepository.saveAll(links);
    }

    @Transactional(readOnly = true)
    public List<Long> getOwnedLinkIds(ActorContext actorContext) {
        return findOwnedLinks(actorContext).stream().map(Link::getId).toList();
    }

    private List<Link> findOwnedLinks(ActorContext actorContext) {
        if (actorContext.isAuthenticated()) {
            return linkRepository.findAllByUserIdOrderByCreatedAtDesc(actorContext.userId());
        }
        return linkRepository.findAllByOwnerTokenAndUserIsNullOrderByCreatedAtDesc(actorContext.anonymousId());
    }

    private void enforceLinkLimit(ActorContext actorContext) {
        if (actorContext.isAuthenticated()) {
            long count = linkRepository.countByUserId(actorContext.userId());
            if (count >= accountMax) {
                throw new BadRequestException("Account link limit reached");
            }
            return;
        }

        long count = linkRepository.countByOwnerTokenAndUserIsNull(actorContext.anonymousId());
        if (count >= anonymousMax) {
            throw new BadRequestException("Anonymous users can only create up to " + anonymousMax + " links. Create an account to add more.");
        }
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
