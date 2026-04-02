package com.themiya.shortener.service;

import com.themiya.shortener.dto.AnalyticsResponse;
import com.themiya.shortener.repository.ClickEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AnalyticsService {
    private final ClickEventRepository clickEventRepository;
    private final LinkService linkService;

    public AnalyticsService(ClickEventRepository clickEventRepository, LinkService linkService) {
        this.clickEventRepository = clickEventRepository;
        this.linkService = linkService;
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics(ActorContext actorContext, Long linkId) {
        if (linkId != null) {
            long total = clickEventRepository.countByLinkId(linkId);
            List<AnalyticsResponse.Bucket> byDay = toBuckets(clickEventRepository.countByDay(linkId));
            List<AnalyticsResponse.Bucket> byCountry = toBuckets(clickEventRepository.countByCountry(linkId));
            List<AnalyticsResponse.Bucket> byDevice = toBuckets(clickEventRepository.countByDeviceType(linkId));
            List<AnalyticsResponse.Bucket> byReferer = toBuckets(clickEventRepository.countByReferer(linkId));
            return AnalyticsResponse.of(total, byDay, byCountry, byDevice, byReferer);
        }

        List<Long> ownedLinkIds = linkService.getOwnedLinkIds(actorContext);
        if (ownedLinkIds.isEmpty()) {
            return AnalyticsResponse.of(0, List.of(), List.of(), List.of(), List.of());
        }

        long total = clickEventRepository.countByLinkIdIn(ownedLinkIds);
        List<AnalyticsResponse.Bucket> byDay = toBuckets(clickEventRepository.countByDayIn(ownedLinkIds));
        List<AnalyticsResponse.Bucket> byCountry = toBuckets(clickEventRepository.countByCountryIn(ownedLinkIds));
        List<AnalyticsResponse.Bucket> byDevice = toBuckets(clickEventRepository.countByDeviceTypeIn(ownedLinkIds));
        List<AnalyticsResponse.Bucket> byReferer = toBuckets(clickEventRepository.countByRefererIn(ownedLinkIds));

        return AnalyticsResponse.of(total, byDay, byCountry, byDevice, byReferer);
    }

    private List<AnalyticsResponse.Bucket> toBuckets(List<ClickEventRepository.LabelCountProjection> rows) {
        return rows.stream()
                .map(row -> new AnalyticsResponse.Bucket(row.getLabel(), row.getTotal() == null ? 0 : row.getTotal()))
                .toList();
    }
}
