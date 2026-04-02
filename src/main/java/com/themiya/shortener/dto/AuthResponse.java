package com.themiya.shortener.dto;

public class AuthResponse {
    private Long userId;
    private String email;

    public static AuthResponse of(Long userId, String email) {
        AuthResponse response = new AuthResponse();
        response.userId = userId;
        response.email = email;
        return response;
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }
}
