package com.themiya.shortener.dto;

import java.util.List;

public class AnalyticsResponse {
    private long totalClicks;
    private List<Bucket> byDay;
    private List<Bucket> byCountry;
    private List<Bucket> byDevice;
    private List<Bucket> byReferer;

    public static AnalyticsResponse of(long totalClicks,
                                       List<Bucket> byDay,
                                       List<Bucket> byCountry,
                                       List<Bucket> byDevice,
                                       List<Bucket> byReferer) {
        AnalyticsResponse response = new AnalyticsResponse();
        response.totalClicks = totalClicks;
        response.byDay = byDay;
        response.byCountry = byCountry;
        response.byDevice = byDevice;
        response.byReferer = byReferer;
        return response;
    }

    public long getTotalClicks() {
        return totalClicks;
    }

    public List<Bucket> getByDay() {
        return byDay;
    }

    public List<Bucket> getByCountry() {
        return byCountry;
    }

    public List<Bucket> getByDevice() {
        return byDevice;
    }

    public List<Bucket> getByReferer() {
        return byReferer;
    }

    public record Bucket(String label, long total) {
    }
}
