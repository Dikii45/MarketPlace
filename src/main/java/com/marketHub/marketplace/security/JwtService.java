package com.marketHub.marketplace.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

// генерация и проверка JWT для REST API (/api/**) — отдельно от сессии обычного сайта
@Component
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    //время жизни токена 24 часа
    @Value("${app.jwt.expiration-ms:86400000}")
    private long expirationMs;

    //создание ключа
    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }


    // subject токена — email пользователя
    public String generateToken(String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        // передаем данные и формируем готовую строку JWT
        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key())
                .compact();
    }

    //по токену получаем email
    public String extractEmail(String token) {

        //проверяем что токен был подписан и возвращаем email
        return Jwts.parser().verifyWith(key()).build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    //проверка, что токен действителен
    public boolean isValid(String token) {
        try {
            Jwts.parser().verifyWith(key()).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
