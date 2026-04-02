package com.themiya.shortener.service;

import com.themiya.shortener.dto.AuthRequest;
import com.themiya.shortener.dto.AuthResponse;
import com.themiya.shortener.entity.UserAccount;
import com.themiya.shortener.exception.BadRequestException;
import com.themiya.shortener.exception.NotFoundException;
import com.themiya.shortener.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Service
public class AuthService {
    public static final String AUTH_COOKIE = "AUTH_TOKEN";
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final LinkService linkService;
    private final long sessionTtlDays;

    public AuthService(UserAccountRepository userAccountRepository,
                       PasswordEncoder passwordEncoder,
                       StringRedisTemplate redisTemplate,
                       LinkService linkService,
                       @Value("${app.auth.session-ttl-days:30}") long sessionTtlDays) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.redisTemplate = redisTemplate;
        this.linkService = linkService;
        this.sessionTtlDays = sessionTtlDays;
    }

    @Transactional
    public AuthResponse signup(AuthRequest request, String anonymousId) {
        String email = normalizeEmail(request.getEmail());
        if (userAccountRepository.existsByEmail(email)) {
            throw new BadRequestException("Email already in use");
        }

        UserAccount user = new UserAccount();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        UserAccount saved = userAccountRepository.save(user);

        if (anonymousId != null && !anonymousId.isBlank()) {
            linkService.transferAnonymousLinksToUser(anonymousId, saved);
        }

        return AuthResponse.of(saved.getId(), saved.getEmail());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest request) {
        String email = normalizeEmail(request.getEmail());
        UserAccount user = userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new NotFoundException("Invalid email or password");
        }

        return AuthResponse.of(user.getId(), user.getEmail());
    }

    public String createSession(Long userId) {
        String token = UUID.randomUUID() + "-" + UUID.randomUUID();
        redisTemplate.opsForValue().set(sessionKey(token), String.valueOf(userId), Duration.ofDays(sessionTtlDays));
        return token;
    }

    public void deleteSession(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        redisTemplate.delete(sessionKey(token));
    }

    @Transactional(readOnly = true)
    public UserAccount resolveUserByToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        String userIdValue = redisTemplate.opsForValue().get(sessionKey(token));
        if (userIdValue == null || userIdValue.isBlank()) {
            return null;
        }

        try {
            Long userId = Long.parseLong(userIdValue);
            return userAccountRepository.findById(userId).orElse(null);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String sessionKey(String token) {
        return "auth:session:" + token;
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
