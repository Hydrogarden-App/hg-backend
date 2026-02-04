package com.hydrogarden.common;


import com.hydrogarden.business.device.core.entity.DeviceId;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Profile("dev")
public class DevAuthenticationFilter extends HydrogardenAuthenticationFilter {
    private UserSecurityModel mockUser = new UserSecurityModel(new UserId("user_1234567"), new DeviceId((short) 1L));

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        SecurityContextHolder.getContext().setAuthentication(mockUser);

        filterChain.doFilter(request, response);
    }
}
