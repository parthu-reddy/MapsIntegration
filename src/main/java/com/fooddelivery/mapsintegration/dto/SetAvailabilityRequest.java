package com.fooddelivery.mapsintegration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SetAvailabilityRequest {
    @NotBlank(message = "cityId is required")
    private String cityId;

    @NotBlank(message = "driverId is required")
    private String driverId;

    @NotNull(message = "available is required")
    private Boolean available;
}
