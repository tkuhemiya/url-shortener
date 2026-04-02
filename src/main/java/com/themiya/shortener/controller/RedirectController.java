package com.themiya.shortener.controller;

import com.themiya.shortener.exception.RateLimitExceededException;
import com.themiya.shortener.service.ClickTrackingService;
import com.themiya.shortener.service.LinkService;
import com.themiya.shortener.service.RateLimitService;
import com.themiya.shortener.util.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
public class RedirectController {
    private final LinkService linkService;
    private final ClickTrackingService clickTrackingService;
    private final RateLimitService rateLimitService;

    public RedirectController(LinkService linkService,
                              ClickTrackingService clickTrackingService,
                              RateLimitService rateLimitService) {
        this.linkService = linkService;
        this.clickTrackingService = clickTrackingService;
        this.rateLimitService = rateLimitService;
    }

    @GetMapping("/{slug:[a-zA-Z0-9]{7}}")
    public ResponseEntity<Void> redirect(@PathVariable String slug, HttpServletRequest request) {
        String ip = RequestUtils.extractClientIp(request);
        if (!rateLimitService.allowRedirect(ip)) {
            throw new RateLimitExceededException("Too many redirect requests");
        }

        String destination = linkService.resolveOriginalUrlBySlug(slug);
        String userAgent = request.getHeader("User-Agent");
        String referer = request.getHeader("Referer");
        clickTrackingService.logClickAsync(slug, ip, userAgent, referer);

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(destination));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
