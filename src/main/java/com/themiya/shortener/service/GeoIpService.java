package com.themiya.shortener.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
public class GeoIpService {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String lookupUrlTemplate;

    public GeoIpService(ObjectMapper objectMapper,
                        @Value("${app.geo-ip.lookup-url:https://ipapi.co/%s/json/}") String lookupUrlTemplate) {
        this.objectMapper = objectMapper;
        this.lookupUrlTemplate = lookupUrlTemplate;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    public String resolveCountry(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank() || isLocalOrPrivate(ipAddress)) {
            return "unknown";
        }

        try {
            String encodedIp = URLEncoder.encode(ipAddress, StandardCharsets.UTF_8);
            String url = lookupUrlTemplate.formatted(encodedIp);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return "unknown";
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode countryName = root.get("country_name");
            if (countryName != null && !countryName.asText().isBlank()) {
                return countryName.asText();
            }
            JsonNode countryCode = root.get("country");
            if (countryCode != null && !countryCode.asText().isBlank()) {
                return countryCode.asText();
            }
            return "unknown";
        } catch (Exception ignored) {
            return "unknown";
        }
    }

    private boolean isLocalOrPrivate(String ipAddress) {
        return ipAddress.startsWith("127.")
                || ipAddress.equals("::1")
                || ipAddress.startsWith("10.")
                || ipAddress.startsWith("192.168.")
                || ipAddress.startsWith("172.16.")
                || ipAddress.startsWith("172.17.")
                || ipAddress.startsWith("172.18.")
                || ipAddress.startsWith("172.19.")
                || ipAddress.startsWith("172.2")
                || ipAddress.startsWith("172.30.")
                || ipAddress.startsWith("172.31.");
    }
}
