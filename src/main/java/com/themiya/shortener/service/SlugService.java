package com.themiya.shortener.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class SlugService {
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int SLUG_LENGTH = 7;
    private final SecureRandom secureRandom = new SecureRandom();

    public String generateSlug() {
        StringBuilder sb = new StringBuilder(SLUG_LENGTH);
        for (int i = 0; i < SLUG_LENGTH; i++) {
            int idx = secureRandom.nextInt(ALPHABET.length());
            sb.append(ALPHABET.charAt(idx));
        }
        return sb.toString();
    }
}
