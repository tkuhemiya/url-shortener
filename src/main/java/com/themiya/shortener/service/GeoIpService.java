package com.themiya.shortener.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(GeoIpService.class);

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
        } catch (Exception ex) {
            log.warn("Geo IP lookup failed for ip={}", ipAddress, ex);
            return "unknown";
        }
    }

    private boolean isLocalOrPrivate(String ipAddress) {
        if (ipAddress.equals("::1") || ipAddress.startsWith("fe80:") || ipAddress.startsWith("fc") || ipAddress.startsWith("fd")) {
            return true;
        }

        String[] octets = ipAddress.split("\\.");
        if (octets.length != 4) {
            return false;
        }

        try {
            int o1 = Integer.parseInt(octets[0]);
            int o2 = Integer.parseInt(octets[1]);
            int o3 = Integer.parseInt(octets[2]);
            int o4 = Integer.parseInt(octets[3]);
            if (!inOctetRange(o1) || !inOctetRange(o2) || !inOctetRange(o3) || !inOctetRange(o4)) {
                return false;
            }

            return o1 == 10
                    || (o1 == 127)
                    || (o1 == 192 && o2 == 168)
                    || (o1 == 172 && o2 >= 16 && o2 <= 31);
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private boolean inOctetRange(int value) {
        return value >= 0 && value <= 255;
    }
}
