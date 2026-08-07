package com.fooddelivery.mapsintegration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class DispatchOrderRequest {
    @NotBlank(message = "cityId is required")
    @Size(max = 50)
    @Pattern(regexp = "^[A-Za-z0-9_\\-]+$")
    private String cityId;
    @NotBlank(message = "restaurantCoords is required")
    @Size(max = 50)
    @Pattern(regexp = "^[-+]?([1-8]?\\d(\\.\\d+)?|90(\\.0+)?),\\s*[-+]?(180(\\.0+)?|((1[0-7]\\d)|([1-9]?\\d))(\\.\\d+)?)$")
    private String restaurantCoords;

    @java.lang.SuppressWarnings("all")
    public DispatchOrderRequest() {
    }

    @java.lang.SuppressWarnings("all")
    public String getCityId() {
        return this.cityId;
    }

    @java.lang.SuppressWarnings("all")
    public String getRestaurantCoords() {
        return this.restaurantCoords;
    }

    @java.lang.SuppressWarnings("all")
    public void setCityId(final String cityId) {
        this.cityId = cityId;
    }

    @java.lang.SuppressWarnings("all")
    public void setRestaurantCoords(final String restaurantCoords) {
        this.restaurantCoords = restaurantCoords;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof DispatchOrderRequest)) return false;
        final DispatchOrderRequest other = (DispatchOrderRequest) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$cityId = this.getCityId();
        final java.lang.Object other$cityId = other.getCityId();
        if (this$cityId == null ? other$cityId != null : !this$cityId.equals(other$cityId)) return false;
        final java.lang.Object this$restaurantCoords = this.getRestaurantCoords();
        final java.lang.Object other$restaurantCoords = other.getRestaurantCoords();
        if (this$restaurantCoords == null ? other$restaurantCoords != null : !this$restaurantCoords.equals(other$restaurantCoords)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof DispatchOrderRequest;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $cityId = this.getCityId();
        result = result * PRIME + ($cityId == null ? 43 : $cityId.hashCode());
        final java.lang.Object $restaurantCoords = this.getRestaurantCoords();
        result = result * PRIME + ($restaurantCoords == null ? 43 : $restaurantCoords.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "DispatchOrderRequest(cityId=" + this.getCityId() + ", restaurantCoords=" + this.getRestaurantCoords() + ")";
    }
}
