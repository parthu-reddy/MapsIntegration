package com.fooddelivery.mapsintegration.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * What the fleet HTTP contracts cannot see.
 *
 * <p>{@code ContractTestBase} stands the controller up with a mocked {@code FleetTrackingService},
 * so `fleet-set-availability.groovy` pins the request and response shape and nothing about Redis.
 * Someone inverting the boolean inside {@link FleetTrackingService#setDriverAvailability} would
 * keep every contract green while quietly making "re-reserve" mean "release".
 *
 * <p>That direction is load-bearing: candidate selection removes a driver from
 * {@code drivers:available:<cityId>} with an atomic SREM, {@code releaseDriver} puts them back, and
 * {@code DeliveryExecutiveApplication.reserveDriverLock} calls this with {@code available=false} to
 * undo a release when a decline could not be recorded.
 */
class FleetAvailabilitySemanticsTest {

    private static final String CITY = "BLR";
    private static final String DRIVER = "4f4a4e37-6ca5-5598-94f1-43ef1628f631";
    private static final String AVAILABILITY_KEY = com.fooddelivery.common.constants.RedisKeyConstants.PREFIX_DRIVERS_AVAILABLE + CITY;

    private RedisTemplate<String, String> redis;
    private SetOperations<String, String> sets;
    private GeoOperations<String, String> geo;
    private FleetTrackingService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redis = mock(RedisTemplate.class);
        sets = mock(SetOperations.class);
        geo = mock(GeoOperations.class);
        when(redis.opsForSet()).thenReturn(sets);
        when(redis.opsForGeo()).thenReturn(geo);
        service = new FleetTrackingService(redis, mock(LogisticsDispatchService.class));
    }

    @Test
    void availableTrueAddsTheDriverToThePool() {
        service.setDriverAvailability(CITY, DRIVER, true);

        verify(sets).add(AVAILABILITY_KEY, DRIVER);
        verify(sets, never()).remove(eq(AVAILABILITY_KEY), eq(DRIVER));
    }

    /**
     * The re-reserve direction, and the whole reason this endpoint exists separately from
     * {@code /api/fleet/release} — which ignores the flag and always releases.
     */
    @Test
    void availableFalseRemovesTheDriverFromThePool() {
        service.setDriverAvailability(CITY, DRIVER, false);

        verify(sets).remove(AVAILABILITY_KEY, DRIVER);
        verify(sets, never()).add(eq(AVAILABILITY_KEY), eq(DRIVER));
    }

    @Test
    void releasingADriverMakesThemAvailableAndDropsTheirLock() {
        service.releaseDriver(CITY, DRIVER);

        verify(sets).add(AVAILABILITY_KEY, DRIVER);
        verify(redis).delete("driver:lock:" + DRIVER);
    }

    @Test
    void releasingABatchRestoresEveryDriver() {
        String second = "9a1b2c3d-4e5f-6071-8293-a4b5c6d7e8f9";

        service.releaseDrivers(CITY, List.of(DRIVER, second));

        verify(sets).add(eq(AVAILABILITY_KEY), eq(DRIVER), eq(second));
    }

    @Test
    void releasingAnEmptyBatchTouchesNothing() {
        service.releaseDrivers(CITY, List.of());

        verify(sets, never()).add(eq(AVAILABILITY_KEY), org.mockito.ArgumentMatchers.<String>any());
    }

    @Test
    void deletingADriverRemovesThemFromBothGeoAndAvailability() {
        service.deleteDriver(CITY, DRIVER);

        verify(geo).remove("drivers:geo:" + CITY, DRIVER);
        verify(sets).remove(AVAILABILITY_KEY, DRIVER);
    }

    /**
     * The availability key is spelled by hand in six places in this class, and again in
     * DeliveryExecutiveApplication. A drift between the set selection removes from and the set a
     * release adds to would strand drivers in a pool nothing reads, with no error anywhere.
     */
    @Test
    void everyAvailabilityOperationUsesTheSameKey() {
        service.setDriverAvailability(CITY, DRIVER, true);
        service.setDriverAvailability(CITY, DRIVER, false);
        service.releaseDriver(CITY, DRIVER);
        service.deleteDriver(CITY, DRIVER);

        org.mockito.ArgumentCaptor<String> keys = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(sets, org.mockito.Mockito.atLeastOnce()).add(keys.capture(), org.mockito.ArgumentMatchers.<String>any());
        verify(sets, org.mockito.Mockito.atLeastOnce()).remove(keys.capture(), org.mockito.ArgumentMatchers.<Object>any());

        org.assertj.core.api.Assertions.assertThat(keys.getAllValues())
                .describedAs("selection, release and re-reserve must all address one set")
                .containsOnly(AVAILABILITY_KEY);
    }
}
