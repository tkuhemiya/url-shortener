package com.themiya.shortener.service;

import com.themiya.shortener.entity.UserAccount;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@Service
public class ActorContextService {
    public static final String ANON_COOKIE = "ANON_ID";
    private final AuthService authService;

    public ActorContextService(AuthService authService) {
        this.authService = authService;
    }

    public ActorContext resolve(HttpServletRequest request, HttpServletResponse response) {
        String authToken = getCookieValue(request, AuthService.AUTH_COOKIE).orElse(null);
        UserAccount user = authService.resolveUserByToken(authToken);
        if (user != null) {
            return new ActorContext(user.getId(), user.getEmail(), null);
        }

        String anonId = getCookieValue(request, ANON_COOKIE).orElse(null);
        if (anonId == null || anonId.isBlank()) {
            anonId = UUID.randomUUID().toString();
            setCookie(response, ANON_COOKIE, anonId, 365 * 24 * 60 * 60);
        }

        return new ActorContext(null, null, anonId);
    }

    public void setAuthCookie(HttpServletResponse response, String token) {
        setCookie(response, AuthService.AUTH_COOKIE, token, 30 * 24 * 60 * 60);
    }

    public void clearAuthCookie(HttpServletResponse response) {
        setCookie(response, AuthService.AUTH_COOKIE, "", 0);
    }

    public Optional<String> getAuthToken(HttpServletRequest request) {
        return getCookieValue(request, AuthService.AUTH_COOKIE);
    }

    private Optional<String> getCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> c.getName().equals(name))
                .map(Cookie::getValue)
                .findFirst();
    }

    private void setCookie(HttpServletResponse response, String name, String value, int maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .path("/")
                .httpOnly(true)
                .sameSite("Lax")
                .maxAge(maxAgeSeconds)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
}
