package org.bublapi.dent.integration.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.bublapi.dent.auth.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class TestJwtHelper {

   @Autowired
   private JwtService jwtService;

   @Value("${jwt.secret}")
   private String secret;

   public String token(UUID userId) {
      return "Bearer " + jwtService.generateToken(userId);
   }

   public String expiredToken(UUID userId) {
      SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
      String token = Jwts.builder()
                         .subject(userId.toString())
                         .issuedAt(new Date(0))
                         .expiration(new Date(1))
                         .signWith(key)
                         .compact();
      return "Bearer " + token;
   }

   public String invalidToken() {
      return "Bearer totally.invalid.token";
   }
}
