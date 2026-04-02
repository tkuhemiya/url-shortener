package com.themiya.shortener.controller;

import com.themiya.shortener.dto.AnalyticsResponse;
import com.themiya.shortener.service.AnalyticsService;
import com.themiya.shortener.service.LinkService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {
    private final AnalyticsService analyticsService;
    private final LinkService linkService;

    public AnalyticsController(AnalyticsService analyticsService, LinkService linkService) {
        this.analyticsService = analyticsService;
        this.linkService = linkService;
    }

    @GetMapping
    public AnalyticsResponse analytics(@RequestParam(required = false) Long linkId) {
        if (linkId != null) {
            linkService.getLinkOrThrow(linkId);
        }
        return analyticsService.getAnalytics(linkId);
    }
}
