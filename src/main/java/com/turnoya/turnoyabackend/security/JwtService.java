package com.turnoya.turnoyabackend.security;

import com.turnoya.turnoyabackend.entity.Usuario;
import com.turnoya.turnoyabackend.exception.TokenExpiredException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_ROL = "rol";
    private static final String CLAIM_TIPO = "tipo";
    private static final String TIPO_ACCESS = "access";
    private static final String TIPO_REFRESH = "refresh";

    private final SecretKey signingKey;
    private final long expirationMs;
    private final long refreshExpirationMs;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration-ms}") long expirationMs,
                      @Value("${jwt.refresh-expiration-ms:604800000}") long refreshExpirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public String generarAccessToken(Usuario usuario) {
        return construirToken(usuario, TIPO_ACCESS, expirationMs);
    }

    public String generarRefreshToken(Usuario usuario) {
        return construirToken(usuario, TIPO_REFRESH, refreshExpirationMs);
    }

    public boolean esAccessTokenValido(String token) {
        try {
            return TIPO_ACCESS.equals(extraerClaims(token).get(CLAIM_TIPO, String.class));
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * Valida un refresh token y devuelve el email del usuario dueño del token.
     */
    public String validarRefreshToken(String token) {
        try {
            Claims claims = extraerClaims(token);
            if (!TIPO_REFRESH.equals(claims.get(CLAIM_TIPO, String.class))) {
                throw new TokenExpiredException("El token enviado no es un refresh token");
            }
            return claims.getSubject();
        } catch (ExpiredJwtException ex) {
            throw new TokenExpiredException("El refresh token expiró, vuelve a iniciar sesión");
        } catch (JwtException | IllegalArgumentException ex) {
            throw new TokenExpiredException("El refresh token no es válido");
        }
    }

    public String extraerEmail(String token) {
        return extraerClaims(token).getSubject();
    }

    public Long extraerUserId(String token) {
        Number userId = extraerClaims(token).get(CLAIM_USER_ID, Number.class);
        return userId != null ? userId.longValue() : null;
    }

    public String extraerRol(String token) {
        return extraerClaims(token).get(CLAIM_ROL, String.class);
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    private String construirToken(Usuario usuario, String tipo, long duracionMs) {
        Date ahora = new Date();
        return Jwts.builder()
                .subject(usuario.getEmail())
                .claim(CLAIM_USER_ID, usuario.getId())
                .claim(CLAIM_ROL, usuario.getRol().name())
                .claim(CLAIM_TIPO, tipo)
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + duracionMs))
                .signWith(signingKey)
                .compact();
    }

    private Claims extraerClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}