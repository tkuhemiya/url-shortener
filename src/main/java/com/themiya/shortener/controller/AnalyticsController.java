package com.themiya.shortener.controller;

import com.themiya.shortener.dto.AnalyticsResponse;
import com.themiya.shortener.service.ActorContext;
import com.themiya.shortener.service.ActorContextService;
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
    private final ActorContextService actorContextService;

    public AnalyticsController(AnalyticsService analyticsService,
                               LinkService linkService,
                               ActorContextService actorContextService) {
        this.analyticsService = analyticsService;
        this.linkService = linkService;
        this.actorContextService = actorContextService;
    }

    @GetMapping
    public AnalyticsResponse analytics(@RequestParam(required = false) Long linkId,
                                       jakarta.servlet.http.HttpServletRequest request,
                                       jakarta.servlet.http.HttpServletResponse response) {
        ActorContext actor = actorContextService.resolve(request, response);
        if (linkId != null) {
            linkService.getOwnedLinkOrThrow(linkId, actor);
        }
        return analyticsService.getAnalytics(actor, linkId);
    }
}
