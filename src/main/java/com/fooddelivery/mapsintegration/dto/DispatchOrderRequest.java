package com.fooddelivery.mapsintegration.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DispatchOrderRequest {
    @NotBlank(message = "cityId is required")
    private String cityId;

    @NotBlank(message = "restaurantCoords is required")
    private String restaurantCoords;
}
