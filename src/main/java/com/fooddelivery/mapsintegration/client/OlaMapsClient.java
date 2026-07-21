package com.fooddelivery.mapsintegration.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "ola-maps", url = "${olamaps.api.base-url:https://api.olamaps.io}", configuration = com.fooddelivery.mapsintegration.config.OlaMapsFeignConfig.class)
public interface OlaMapsClient {

    @GetMapping("/places/v1/autocomplete")
    Map<String, Object> getAutocompleteSuggestions(
            @RequestParam("input") String input,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam("api_key") String apiKey);

    @GetMapping("/places/v1/reverse-geocode")
    Map<String, Object> reverseGeocode(
            @RequestParam("latlng") String latlng,
            @RequestParam("api_key") String apiKey);
            
    @GetMapping("/routing/v1/distanceMatrix")
    Map<String, Object> getDistanceMatrix(
            @RequestParam("origins") String origins,
            @RequestParam("destinations") String destinations,
            @RequestParam("mode") String mode,
            @RequestParam("route_preference") String routePreference,
            @RequestParam("api_key") String apiKey);
            
    @org.springframework.web.bind.annotation.PostMapping("/routing/v1/directions")
    Map<String, Object> getDirections(
            @RequestParam("origin") String origin,
            @RequestParam("destination") String destination,
            @RequestParam("mode") String mode,
            @RequestParam("steps") boolean steps,
            @RequestParam("overview") String overview,
            @RequestParam("language") String language,
            @RequestParam("route_preference") String routePreference,
            @RequestParam("api_key") String apiKey);
}
