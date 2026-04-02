package com.themiya.shortener.service;

import com.themiya.shortener.entity.ClickEvent;
import com.themiya.shortener.entity.Link;
import com.themiya.shortener.repository.ClickEventRepository;
import com.themiya.shortener.repository.LinkRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class ClickTrackingService {
    private final ClickEventRepository clickEventRepository;
    private final LinkRepository linkRepository;

    public ClickTrackingService(ClickEventRepository clickEventRepository, LinkRepository linkRepository) {
        this.clickEventRepository = clickEventRepository;
        this.linkRepository = linkRepository;
    }

    @Async
    @Transactional
    public void logClickAsync(Long linkId, HttpServletRequest request) {
        Link link = linkRepository.findById(linkId).orElse(null);
        if (link == null) {
            return;
        }

        ClickEvent event = new ClickEvent();
        event.setLink(link);
        event.setClickedAt(OffsetDateTime.now());
        event.setIpAddress(extractIp(request));
        event.setUserAgent(request.getHeader("User-Agent"));
        event.setReferer(request.getHeader("Referer"));
        event.setCountry("unknown");
        event.setDeviceType(extractDeviceType(request.getHeader("User-Agent")));
        event.setBrowser(extractBrowser(request.getHeader("User-Agent")));
        event.setOs(extractOs(request.getHeader("User-Agent")));

        clickEventRepository.save(event);

        link.setClickCount(link.getClickCount() + 1);
        linkRepository.save(link);
    }

    private String extractIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
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
