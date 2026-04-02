package com.themiya.shortener.controller;

import com.themiya.shortener.dto.AnalyticsResponse;
import com.themiya.shortener.dto.CreateLinkRequest;
import com.themiya.shortener.dto.LinkResponse;
import com.themiya.shortener.exception.RateLimitExceededException;
import com.themiya.shortener.service.*;
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
    private final ActorContextService actorContextService;

    public LinkController(LinkService linkService,
                          AnalyticsService analyticsService,
                          RateLimitService rateLimitService,
                          ActorContextService actorContextService) {
        this.linkService = linkService;
        this.analyticsService = analyticsService;
        this.rateLimitService = rateLimitService;
        this.actorContextService = actorContextService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LinkResponse create(@Valid @RequestBody CreateLinkRequest request,
                               HttpServletRequest httpRequest,
                               jakarta.servlet.http.HttpServletResponse httpResponse) {
        String ip = RequestUtils.extractClientIp(httpRequest);
        if (!rateLimitService.allowCreate(ip)) {
            throw new RateLimitExceededException("Too many link creation requests");
        }
        ActorContext actor = actorContextService.resolve(httpRequest, httpResponse);
        return linkService.createShortLink(request.getUrl(), actor);
    }

    @GetMapping
    public List<LinkResponse> list(HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response) {
        ActorContext actor = actorContextService.resolve(request, response);
        return linkService.listLinks(actor);
    }

    @GetMapping("/{id}")
    public LinkResponse getById(@PathVariable Long id,
                                HttpServletRequest request,
                                jakarta.servlet.http.HttpServletResponse response) {
        ActorContext actor = actorContextService.resolve(request, response);
        return linkService.getOwnedLinkResponseOrThrow(id, actor);
    }

    @GetMapping("/{id}/analytics")
    public AnalyticsResponse analytics(@PathVariable Long id,
                                       HttpServletRequest request,
                                       jakarta.servlet.http.HttpServletResponse response) {
        ActorContext actor = actorContextService.resolve(request, response);
        linkService.getOwnedLinkOrThrow(id, actor);
        return analyticsService.getAnalytics(actor, id);
    }
}
