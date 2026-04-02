package com.themiya.shortener.service;

import com.themiya.shortener.dto.AnalyticsResponse;
import com.themiya.shortener.repository.ClickEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AnalyticsService {
    private final ClickEventRepository clickEventRepository;

    public AnalyticsService(ClickEventRepository clickEventRepository) {
        this.clickEventRepository = clickEventRepository;
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics(Long linkId) {
        long total = clickEventRepository.countByLinkId(linkId);

        List<AnalyticsResponse.Bucket> byDay = toBuckets(clickEventRepository.countByDay(linkId));
        List<AnalyticsResponse.Bucket> byCountry = toBuckets(clickEventRepository.countByCountry(linkId));
        List<AnalyticsResponse.Bucket> byDevice = toBuckets(clickEventRepository.countByDeviceType(linkId));
        List<AnalyticsResponse.Bucket> byReferer = toBuckets(clickEventRepository.countByReferer(linkId));

        return AnalyticsResponse.of(total, byDay, byCountry, byDevice, byReferer);
    }

    private List<AnalyticsResponse.Bucket> toBuckets(List<ClickEventRepository.LabelCountProjection> rows) {
        return rows.stream()
                .map(row -> new AnalyticsResponse.Bucket(row.getLabel(), row.getTotal() == null ? 0 : row.getTotal()))
                .toList();
    }
}
