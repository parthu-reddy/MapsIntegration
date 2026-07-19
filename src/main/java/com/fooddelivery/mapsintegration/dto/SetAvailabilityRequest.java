package com.fooddelivery.mapsintegration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SetAvailabilityRequest {
    @NotBlank(message = "cityId is required")
    @Size(max = 50)
    @Pattern(regexp = "^[A-Za-z0-9_\\-]+$")
    private String cityId;

    @NotBlank(message = "driverId is required")
    @Size(max = 36)
    @Pattern(regexp = "^[0-9a-fA-F\\-]{36}$")
    private String driverId;

    @NotNull(message = "available is required")
    private Boolean available;
}
