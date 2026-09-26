package com.fooddelivery.mapsintegration;

import com.fooddelivery.mapsintegration.controller.IntegrationController;
import com.fooddelivery.mapsintegration.service.FleetTrackingService;
import com.fooddelivery.mapsintegration.service.LocationService;
import com.fooddelivery.mapsintegration.service.LogisticsDispatchService;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;

public abstract class ContractTestBase {
    @BeforeEach
    public void setup() {
        LocationService locationService = Mockito.mock(LocationService.class);
        LogisticsDispatchService dispatchService = Mockito.mock(LogisticsDispatchService.class);
        FleetTrackingService fleetTrackingService = Mockito.mock(FleetTrackingService.class);
        
        when(locationService.resolveCoordinatesToAddress(anyDouble(), anyDouble())).thenReturn("Bangalore, Karnataka, India");

        IntegrationController controller = new IntegrationController(locationService, dispatchService, fleetTrackingService);
        // Serialize as production does: see PlatformJson (contract-harness Jackson drift).
        com.fooddelivery.common.contract.PlatformJson.standaloneSetup(controller);
    }
}
