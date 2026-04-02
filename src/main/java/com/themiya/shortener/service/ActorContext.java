package com.themiya.shortener.service;

public record ActorContext(Long userId, String email, String anonymousId) {
    public boolean isAuthenticated() {
        return userId != null;
    }
}
