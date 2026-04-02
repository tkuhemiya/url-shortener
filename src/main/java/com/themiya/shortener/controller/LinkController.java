package com.themiya.shortener.controller;

import com.themiya.shortener.dto.AnalyticsResponse;
import com.themiya.shortener.dto.CreateLinkRequest;
import com.themiya.shortener.dto.LinkResponse;
import com.themiya.shortener.exception.RateLimitExceededException;
import com.themiya.shortener.service.AnalyticsService;
import com.themiya.shortener.service.LinkService;
import com.themiya.shortener.service.RateLimitService;
import com.themiya.shortener.util.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/links")
public class LinkController {
    private final LinkService linkService;
    private final AnalyticsService analyticsService;
    private final RateLimitService rateLimitService;

    public LinkController(LinkService linkService,
                          AnalyticsService analyticsService,
                          RateLimitService rateLimitService) {
        this.linkService = linkService;
        this.analyticsService = analyticsService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LinkResponse create(@Valid @RequestBody CreateLinkRequest request, HttpServletRequest httpRequest) {
        String ip = RequestUtils.extractClientIp(httpRequest);
        if (!rateLimitService.allowCreate(ip)) {
            throw new RateLimitExceededException("Too many link creation requests");
        }
        return linkService.createShortLink(request.getUrl());
    }

    @GetMapping
    public List<LinkResponse> list() {
        return linkService.listLinks();
    }

    @GetMapping("/{id}")
    public LinkResponse getById(@PathVariable Long id) {
        return linkService.getLinkResponseOrThrow(id);
    }

    @GetMapping("/{id}/analytics")
    public AnalyticsResponse analytics(@PathVariable Long id) {
        linkService.getLinkOrThrow(id);
        return analyticsService.getAnalytics(id);
    }
}
