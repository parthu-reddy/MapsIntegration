package com.fooddelivery.mapsintegration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

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

    @java.lang.SuppressWarnings("all")
    public SetAvailabilityRequest() {
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
    public Boolean getAvailable() {
        return this.available;
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
    public void setAvailable(final Boolean available) {
        this.available = available;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof SetAvailabilityRequest)) return false;
        final SetAvailabilityRequest other = (SetAvailabilityRequest) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$available = this.getAvailable();
        final java.lang.Object other$available = other.getAvailable();
        if (this$available == null ? other$available != null : !this$available.equals(other$available)) return false;
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
        return other instanceof SetAvailabilityRequest;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $available = this.getAvailable();
        result = result * PRIME + ($available == null ? 43 : $available.hashCode());
        final java.lang.Object $cityId = this.getCityId();
        result = result * PRIME + ($cityId == null ? 43 : $cityId.hashCode());
        final java.lang.Object $driverId = this.getDriverId();
        result = result * PRIME + ($driverId == null ? 43 : $driverId.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "SetAvailabilityRequest(cityId=" + this.getCityId() + ", driverId=" + this.getDriverId() + ", available=" + this.getAvailable() + ")";
    }
}
