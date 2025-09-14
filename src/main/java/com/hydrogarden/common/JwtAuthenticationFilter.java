package com.hydrogarden.common;


import io.jsonwebtoken.*;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.security.Key;
import java.util.List;

@RequiredArgsConstructor
@Log4j2
@Profile("!auth-disabled")
public class JwtAuthenticationFilter extends HydrogardenAuthenticationFilter {
    private final JwtKeyCache jwtKeyCache;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                Jws<Claims> jws = Jwts.parser()
                        .keyLocator(new MyKeyLocator())
                        .build().parseSignedClaims(token);

                String userId = jws.getPayload().get("userId", String.class);

                UserId userIdObj = new UserId(userId);

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(userIdObj, null, List.of());

                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception e) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private class MyKeyLocator implements Locator<Key> {

        @Override
        public Key locate(Header header) {
            return jwtKeyCache.getByKid((String) header.get("kid"));
        }
    }
}
