package com.fooddelivery.mapsintegration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateLocationRequest {
    @NotBlank(message = "cityId is required")
    @Size(max = 50)
    @Pattern(regexp = "^[A-Za-z0-9_\\-]+$")
    private String cityId;
    @NotBlank(message = "driverId is required")
    @Size(max = 36)
    @Pattern(regexp = "^[0-9a-fA-F\\-]{36}$")
    private String driverId;
    @NotNull(message = "lat is required")
    private Double lat;
    @NotNull(message = "lng is required")
    private Double lng;

    @java.lang.SuppressWarnings("all")
    public UpdateLocationRequest() {
    }

    @java.lang.SuppressWarnings("all")
    public String getCityId() {
        return this.cityId;
    }

    @java.lang.SuppressWarnings("all")
    public String getDriverId() {
        return this.driverId;
    }

    @java.lang.SuppressWarnings("all")
    public Double getLat() {
        return this.lat;
    }

    @java.lang.SuppressWarnings("all")
    public Double getLng() {
        return this.lng;
    }

    @java.lang.SuppressWarnings("all")
    public void setCityId(final String cityId) {
        this.cityId = cityId;
    }

    @java.lang.SuppressWarnings("all")
    public void setDriverId(final String driverId) {
        this.driverId = driverId;
    }

    @java.lang.SuppressWarnings("all")
    public void setLat(final Double lat) {
        this.lat = lat;
    }

    @java.lang.SuppressWarnings("all")
    public void setLng(final Double lng) {
        this.lng = lng;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof UpdateLocationRequest)) return false;
        final UpdateLocationRequest other = (UpdateLocationRequest) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$lat = this.getLat();
        final java.lang.Object other$lat = other.getLat();
        if (this$lat == null ? other$lat != null : !this$lat.equals(other$lat)) return false;
        final java.lang.Object this$lng = this.getLng();
        final java.lang.Object other$lng = other.getLng();
        if (this$lng == null ? other$lng != null : !this$lng.equals(other$lng)) return false;
        final java.lang.Object this$cityId = this.getCityId();
        final java.lang.Object other$cityId = other.getCityId();
        if (this$cityId == null ? other$cityId != null : !this$cityId.equals(other$cityId)) return false;
        final java.lang.Object this$driverId = this.getDriverId();
        final java.lang.Object other$driverId = other.getDriverId();
        if (this$driverId == null ? other$driverId != null : !this$driverId.equals(other$driverId)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof UpdateLocationRequest;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $lat = this.getLat();
        result = result * PRIME + ($lat == null ? 43 : $lat.hashCode());
        final java.lang.Object $lng = this.getLng();
        result = result * PRIME + ($lng == null ? 43 : $lng.hashCode());
        final java.lang.Object $cityId = this.getCityId();
        result = result * PRIME + ($cityId == null ? 43 : $cityId.hashCode());
        final java.lang.Object $driverId = this.getDriverId();
        result = result * PRIME + ($driverId == null ? 43 : $driverId.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "UpdateLocationRequest(cityId=" + this.getCityId() + ", driverId=" + this.getDriverId() + ", lat=" + this.getLat() + ", lng=" + this.getLng() + ")";
    }
}
