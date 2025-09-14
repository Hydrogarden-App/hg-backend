package com.hydrogarden.common;

import java.time.LocalDateTime;

public interface HydrogardenTimeProvider {
    LocalDateTime getCurrentTime();
}
