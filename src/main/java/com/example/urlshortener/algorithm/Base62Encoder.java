package com.example.urlshortener.algorithm;

import org.springframework.stereotype.Component;

@Component
public class Base62Encoder {

    private static final String BASE62_CHARS =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = 62;
    private static final int DEFAULT_LENGTH = 6;

    public String encode(long number) {
        if (number == 0) return String.valueOf(BASE62_CHARS.charAt(0));
        StringBuilder sb = new StringBuilder();
        while (number > 0) {
            sb.append(BASE62_CHARS.charAt((int) (number % BASE)));
            number /= BASE;
        }
        while (sb.length() < DEFAULT_LENGTH) {
            sb.append(BASE62_CHARS.charAt(0));
        }
        return sb.reverse().toString();
    }

    public long decode(String code) {
        long result = 0;
        for (char c : code.toCharArray()) {
            result = result * BASE + BASE62_CHARS.indexOf(c);
        }
        return result;
    }

    public String generateShortCode(String originalUrl) {
        long seed = Math.abs((originalUrl + System.nanoTime()).hashCode());
        seed = seed * 31L + System.currentTimeMillis();
        return encode(seed).substring(0, DEFAULT_LENGTH);
    }

    public boolean isValidCode(String code) {
        if (code == null || code.isBlank()) return false;
        for (char c : code.toCharArray()) {
            if (BASE62_CHARS.indexOf(c) == -1) return false;
        }
        return true;
    }
}
