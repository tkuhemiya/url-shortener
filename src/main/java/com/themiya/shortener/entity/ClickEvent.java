package com.themiya.shortener.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "click_events", indexes = {
        @Index(name = "idx_click_events_link_id", columnList = "link_id"),
        @Index(name = "idx_click_events_clicked_at", columnList = "clicked_at"),
        @Index(name = "idx_click_events_link_id_clicked_at", columnList = "link_id, clicked_at")
})
public class ClickEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "link_id", nullable = false)
    private Link link;

    @Column(name = "clicked_at", nullable = false)
    private OffsetDateTime clickedAt;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 1024)
    private String userAgent;

    @Column(name = "referer", length = 1024)
    private String referer;

    @Column(name = "country", length = 64)
    private String country;

    @Column(name = "device_type", length = 64)
    private String deviceType;

    @Column(name = "browser", length = 64)
    private String browser;

    @Column(name = "os", length = 64)
    private String os;

    @PrePersist
    void prePersist() {
        if (this.clickedAt == null) {
            this.clickedAt = OffsetDateTime.now();
        }
    }

    public void setLink(Link link) {
        this.link = link;
    }

    public void setClickedAt(OffsetDateTime clickedAt) {
        this.clickedAt = clickedAt;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public void setReferer(String referer) {
        this.referer = referer;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public void setOs(String os) {
        this.os = os;
    }
}
