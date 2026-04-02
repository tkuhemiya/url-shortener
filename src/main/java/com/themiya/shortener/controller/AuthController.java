package com.themiya.shortener.controller;

import com.themiya.shortener.dto.AuthRequest;
import com.themiya.shortener.dto.AuthResponse;
import com.themiya.shortener.service.ActorContext;
import com.themiya.shortener.service.ActorContextService;
import com.themiya.shortener.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final ActorContextService actorContextService;

    public AuthController(AuthService authService, ActorContextService actorContextService) {
        this.authService = authService;
        this.actorContextService = actorContextService;
    }

    @PostMapping("/signup")
    public AuthResponse signup(@Valid @RequestBody AuthRequest request,
                               HttpServletRequest httpRequest,
                               HttpServletResponse httpResponse) {
        ActorContext actor = actorContextService.resolve(httpRequest, httpResponse);
        AuthResponse response = authService.signup(request, actor.anonymousId());
        String token = authService.createSession(response.getUserId());
        actorContextService.setAuthCookie(httpResponse, token);
        return response;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequest request,
                              HttpServletResponse httpResponse) {
        AuthResponse response = authService.login(request);
        String token = authService.createSession(response.getUserId());
        actorContextService.setAuthCookie(httpResponse, token);
        return response;
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpServletRequest request, HttpServletResponse response) {
        actorContextService.getAuthToken(request).ifPresent(authService::deleteSession);
        actorContextService.clearAuthCookie(response);
        return Map.of("ok", true);
    }

    @GetMapping("/me")
    public Map<String, Object> me(HttpServletRequest request, HttpServletResponse response) {
        ActorContext actor = actorContextService.resolve(request, response);
        Map<String, Object> payload = new HashMap<>();
        payload.put("authenticated", actor.isAuthenticated());
        payload.put("userId", actor.userId());
        payload.put("email", actor.email());
        return payload;
    }
}
