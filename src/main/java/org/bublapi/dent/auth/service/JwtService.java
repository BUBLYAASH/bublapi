package org.bublapi.dent.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtService {
   private final String secret;
   private final long expirationMs;

   public JwtService(@Value("${jwt.secret}") String secret, @Value("${jwt.expiration-ms}") long expirationMs) {
      this.secret = secret;
      this.expirationMs = expirationMs;
   }

   public String generateToken(UUID userId) {
      return generateToken(Map.of(), userId.toString());
   }

   private String generateToken(Map<String, Object> claims, String userId) {
      return Jwts.builder()
                 .claims(claims)
                 .subject(userId)
                 .issuedAt(new Date())
                 .expiration(new Date(System.currentTimeMillis() + expirationMs))
                 .signWith(getSignKey())
                 .compact();
   }

   private SecretKey getSignKey() {
      return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
   }

   public String extractUserId(String token) {
      return extractClaim(token, Claims::getSubject);
   }

   public Date extractExpiration(String token) {
      return extractClaim(token, Claims::getExpiration);
   }

   public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
      Claims claims = extractAllClaims(token);

      return claimsResolver.apply(claims);
   }

   private Claims extractAllClaims(String token) {
      return Jwts.parser().verifyWith(getSignKey()).build().parseSignedClaims(token).getPayload();
   }

   private boolean isTokenExpired(String token) {
      Date expiration = extractExpiration(token);

      return expiration.before(new Date());
   }

   public boolean isTokenValid(String token, UUID userId) {
      String tokenUserId = extractUserId(token);

      return tokenUserId.equals(userId.toString()) && !isTokenExpired(token);
   }
}
