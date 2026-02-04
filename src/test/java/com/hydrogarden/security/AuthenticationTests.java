package com.hydrogarden.security;


import com.hydrogarden.business.device.app.service.DeviceApplicationService;
import com.hydrogarden.business.device.core.entity.DeviceId;
import com.hydrogarden.common.UserId;
import com.hydrogarden.common.UserSecurityModel;
import com.hydrogarden.test.utils.HydrogardenIntegrationTest;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class AuthenticationTests extends HydrogardenIntegrationTest {


    @Autowired
    DeviceApplicationService deviceApplicationService;

    private static @NonNull UserSecurityModel getUserWithDevice() {
        return new UserSecurityModel(new UserId("userId"), new DeviceId((short) 123));
    }

    private static @NonNull UserSecurityModel getUserWithoutDevice() {
        return new UserSecurityModel(new UserId("userId"), null);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    class User__with_device {
        @BeforeEach
        void setUp() {
            UserSecurityModel authentication = getUserWithDevice();
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        @Test
        void renameDevice_shouldPassSecurityCheck_whenDeviceIdMatches() {
            DeviceId userDeviceId = new DeviceId((short) 123);

            // Security check passes; throws IllegalArgumentException because device doesn't exist (not AccessDeniedException)
            assertThrows(IllegalArgumentException.class, () ->
                    deviceApplicationService.renameDevice(userDeviceId, "New Name"));
        }

        @Test
        void renameDevice_shouldBeDenied_whenDeviceIdDoesNotMatch() {
            DeviceId differentDeviceId = new DeviceId((short) 124);

            assertThrows(AccessDeniedException.class, () ->
                    deviceApplicationService.renameDevice(differentDeviceId, "New Name"));
        }
    }

    @Nested
    class User__without_device {
        @BeforeEach
        void setUp() {
            UserSecurityModel authentication = getUserWithoutDevice();
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        @Test
        void renameDevice_shouldBeDenied_forAnyDeviceId() {
            DeviceId anyDeviceId = new DeviceId((short) 123);

            assertThrows(AccessDeniedException.class, () ->
                    deviceApplicationService.renameDevice(anyDeviceId, "New Name"));
        }
    }
}
