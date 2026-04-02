package com.themiya.shortener.service;

import com.themiya.shortener.entity.ClickEvent;
import com.themiya.shortener.entity.Link;
import com.themiya.shortener.repository.ClickEventRepository;
import com.themiya.shortener.repository.LinkRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class ClickTrackingService {
    private final ClickEventRepository clickEventRepository;
    private final LinkRepository linkRepository;
    private final GeoIpService geoIpService;

    public ClickTrackingService(ClickEventRepository clickEventRepository,
                                LinkRepository linkRepository,
                                GeoIpService geoIpService) {
        this.clickEventRepository = clickEventRepository;
        this.linkRepository = linkRepository;
        this.geoIpService = geoIpService;
    }

    @Async
    @Transactional
    public void logClickAsync(String slug, String ipAddress, String userAgent, String referer) {
        Link link = linkRepository.findBySlugAndActiveTrue(slug).orElse(null);
        if (link == null) {
            return;
        }

        ClickEvent event = new ClickEvent();
        event.setLink(link);
        event.setClickedAt(OffsetDateTime.now());
        event.setIpAddress(ipAddress);
        event.setUserAgent(userAgent);
        event.setReferer(referer);
        event.setCountry(geoIpService.resolveCountry(ipAddress));
        event.setDeviceType(extractDeviceType(userAgent));
        event.setBrowser(extractBrowser(userAgent));
        event.setOs(extractOs(userAgent));

        clickEventRepository.save(event);
        linkRepository.incrementClickCount(link.getId());
    }

    private String extractDeviceType(String ua) {
        if (ua == null) return "unknown";
        String lower = ua.toLowerCase();
        if (lower.contains("mobile") || lower.contains("android") || lower.contains("iphone")) return "mobile";
        if (lower.contains("ipad") || lower.contains("tablet")) return "tablet";
        return "desktop";
    }

    private String extractBrowser(String ua) {
        if (ua == null) return "unknown";
        String lower = ua.toLowerCase();
        if (lower.contains("edg/")) return "edge";
        if (lower.contains("chrome/")) return "chrome";
        if (lower.contains("firefox/")) return "firefox";
        if (lower.contains("safari/") && !lower.contains("chrome/")) return "safari";
        return "other";
    }

    private String extractOs(String ua) {
        if (ua == null) return "unknown";
        String lower = ua.toLowerCase();
        if (lower.contains("windows")) return "windows";
        if (lower.contains("mac os")) return "macos";
        if (lower.contains("android")) return "android";
        if (lower.contains("iphone") || lower.contains("ios")) return "ios";
        if (lower.contains("linux")) return "linux";
        return "other";
    }
}
