package org.bublapi.dent.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {
   private static final String secret = "my-super-secret-key-my-super-secret-key-123456";

   public String generateToken(String email) {
      return generateToken(Map.of(), email);
   }

   private String generateToken(Map<String, Object> claims, String email) {
      return Jwts.builder()
                 .claims(claims)
                 .subject(email)
                 .issuedAt(new Date())
                 .expiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 30))
                 .signWith(getSignKey())
                 .compact();
   }

   private SecretKey getSignKey() {
      return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
   }

   public String extractUsername(String token) {
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

   public boolean isTokenValid(String token, UserDetails userDetails) {
      String username = extractUsername(token);
      return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
   }
}
