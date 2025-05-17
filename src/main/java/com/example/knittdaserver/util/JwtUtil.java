package com.example.knittdaserver.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParserBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.sql.Date;

@Component
public class JwtUtil {
    private final String SECRET_KEY = "YC1rkQyXpcig1pO8d7eNg4JtecikzYfavNYzTIDBdOk=";
    private final long EXPIRATION_TIME = 864000000;

    public String generateToken(Long userId) {
        Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
//                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME * 10000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Long validateAndExtractUserId(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY.getBytes(StandardCharsets.UTF_8))
                .build()
                .parseClaimsJws(token)
                .getBody();
        return Long.parseLong(claims.getSubject());
    }
}
