package com.hydrogarden.security.filter;


import com.hydrogarden.business.common.vo.DeviceId;
import com.hydrogarden.business.common.vo.UserId;
import com.hydrogarden.business.device.infra.repository.DeviceOwnershipRepository;
import com.hydrogarden.security.webservice.JwtKeyCache;
import com.hydrogarden.security.UserSecurityModel;
import io.jsonwebtoken.*;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.Key;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j2
@Component
@Profile("!dev")
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends HydrogardenAuthenticationFilter {
    private final JwtKeyCache jwtKeyCache;
    private final DeviceOwnershipRepository deviceOwnershipRepository;


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

                Set<DeviceId> devices = deviceOwnershipRepository.findAllByOwnerId(new UserId(userId)).stream().map(o -> o.getId().getDeviceId()).collect(Collectors.toSet());
                UserSecurityModel auth =
                        new UserSecurityModel(new UserId(userId), devices);

                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("User authorised userId={}", userId);
            } catch (Exception e) {
                log.debug("User unauthorised");
                log.debug(e);
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
