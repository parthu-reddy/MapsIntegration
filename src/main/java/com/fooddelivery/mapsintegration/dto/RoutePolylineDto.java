package com.fooddelivery.mapsintegration.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutePolylineDto {
    private String polyline;
    private String distance;
    private String duration;
    private List<Map<String, Object>> steps;
}
