/**
 * Ola Maps Fleet Command Application
 * Core JavaScript Logic
 */

const CITY_ID = "BENGALURU";
const CENTER_LAT = 12.935;
const CENTER_LNG = 77.625;

let mapInstance = null;
let driverMarkers = {};
let currentRestaurantMarker = null;

/**
 * Bootstraps the application, fetching API keys and initializing the map.
 */
async function initApp() {
    setupEventListeners();
    
    let apiKey = "";
    try {
        const res = await fetch('/api/config/maps-key');
        if (!res.ok) throw new Error(`HTTP error! status: ${res.status}`);
        const data = await res.json();
        apiKey = data.key;
    } catch (err) {
        console.error("Failed to fetch API key", err);
        logStatus("Critical Error: Failed to load map credentials.", "error");
        return;
    }

    try {
        const olaMaps = new window.OlaMaps({ apiKey });
        mapInstance = await olaMaps.init({
            style: "https://api.olamaps.io/tiles/vector/v1/styles/default-light-standard/style.json",
            container: 'map',
            center: [CENTER_LNG, CENTER_LAT],
            zoom: 13,
        });

        mapInstance.on('click', handleMapClick);
        logStatus("System initialized. Waiting for commands...", "info");
    } catch (err) {
        console.error("Map initialization failed", err);
        logStatus("Failed to initialize map.", "error");
    }
}

/**
 * Sets up DOM event listeners.
 */
function setupEventListeners() {
    document.getElementById('btn-spawn').addEventListener('click', spawnFleet);
    document.getElementById('btn-reset').addEventListener('click', resetMap);
}

/**
 * Appends a log message to the UI.
 * @param {string} msg 
 * @param {string} type 'info', 'warn', 'error'
 */
function logStatus(msg, type = "info") {
    const logContainer = document.getElementById('status-log');
    if (!logContainer) return;
    
    const time = new Date().toLocaleTimeString();
    const entry = document.createElement('div');
    entry.className = `log-entry ${type}`;
    entry.textContent = `[${time}] ${msg}`;
    
    logContainer.appendChild(entry);
    logContainer.scrollTop = logContainer.scrollHeight;
}

/**
 * Spawns a virtual fleet of drivers on the map and registers them in Redis.
 */
async function spawnFleet() {
    if (!mapInstance) return;
    logStatus("Spawning virtual fleet...", "info");
    
    // Clear existing fleet
    Object.values(driverMarkers).forEach(m => m.marker.remove());
    driverMarkers = {};

    for (let i = 1; i <= 10; i++) {
        const driverId = `driver-${100 + i}`;
        // Randomize nearby coordinates (approx ~3-4km radius)
        const lat = CENTER_LAT + (Math.random() - 0.5) * 0.05;
        const lng = CENTER_LNG + (Math.random() - 0.5) * 0.05;

        // Create DOM element for the marker
        const el = document.createElement('div');
        el.className = 'driver-marker';
        
        const marker = new maplibregl.Marker({ element: el })
            .setLngLat([lng, lat])
            .addTo(mapInstance);
        
        driverMarkers[driverId] = { marker, el, lat, lng };

        // Register with backend asynchronously
        registerDriverLocation(driverId, lat, lng);
        registerDriverAvailability(driverId, true);
    }
    
    logStatus("Registered 10 available drivers in Redis geo-index.", "info");
}

async function registerDriverLocation(driverId, lat, lng) {
    try {
        await fetch('/api/fleet/location', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({ cityId: CITY_ID, driverId, lat, lng })
        });
    } catch (e) {
        console.error("Failed to register driver location", e);
    }
}

async function registerDriverAvailability(driverId, available) {
    try {
        await fetch('/api/fleet/availability', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({ cityId: CITY_ID, driverId, available })
        });
    } catch (e) {
        console.error("Failed to register driver availability", e);
    }
}

/**
 * Handles map click to trigger order dispatch workflow.
 */
async function handleMapClick(e) {
    const restLng = e.lngLat.lng;
    const restLat = e.lngLat.lat;

    clearRouteAndRestaurant();
    
    // Reset driver styles
    Object.values(driverMarkers).forEach(d => d.el.classList.remove('assigned'));

    // Drop Restaurant Marker
    const restEl = document.createElement('div');
    restEl.className = 'restaurant-marker';
    currentRestaurantMarker = new maplibregl.Marker({ element: restEl })
        .setLngLat([restLng, restLat])
        .addTo(mapInstance);

    logStatus(`Order placed at ${restLat.toFixed(4)}, ${restLng.toFixed(4)}`, "info");
    logStatus(`Dispatching to nearest available driver...`, "info");

    try {
        const dispatchRes = await fetch('/api/logistics/dispatch', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({
                cityId: CITY_ID,
                restaurantCoords: `${restLat},${restLng}`
            })
        });

        if (!dispatchRes.ok) {
            throw new Error(`Dispatch API returned ${dispatchRes.status}`);
        }

        const dispatchData = await dispatchRes.json();

        if (!dispatchData.success || !dispatchData.driverId) {
            logStatus("No available drivers found nearby!", "error");
            return;
        }

        const driverId = dispatchData.driverId;
        logStatus(`Driver ${driverId} atomically assigned!`, "info");

        // Highlight Driver
        if (driverMarkers[driverId]) {
            driverMarkers[driverId].el.classList.add('assigned');
            const dLat = driverMarkers[driverId].lat;
            const dLng = driverMarkers[driverId].lng;

            fetchAndDrawRoute(dLat, dLng, restLat, restLng);
        }
    } catch (err) {
        logStatus("Error during dispatch: " + err.message, "error");
    }
}

/**
 * Fetches turn-by-turn route and draws it on the map.
 */
async function fetchAndDrawRoute(startLat, startLng, endLat, endLng) {
    logStatus("Fetching turn-by-turn route...", "info");
    try {
        const routeRes = await fetch(`/api/logistics/route?origin=${startLat},${startLng}&destination=${endLat},${endLng}`);
        if (!routeRes.ok) throw new Error(`Route API returned ${routeRes.status}`);
        
        const routeData = await routeRes.json();

        if (routeData.polyline) {
            logStatus(`Route found! ETA: ${routeData.duration} (${routeData.distance} km)`, "info");
            drawRoute(routeData.polyline);
        } else {
            logStatus("No route polyline returned.", "warn");
        }
    } catch (err) {
        logStatus("Error fetching route: " + err.message, "error");
    }
}

/**
 * Decodes Google Polyline algorithm string.
 */
function decodePolyline(encoded) {
    let points = [];
    let index = 0, len = encoded.length;
    let lat = 0, lng = 0;
    
    while (index < len) {
        let b, shift = 0, result = 0;
        do {
            b = encoded.charCodeAt(index++) - 63;
            result |= (b & 0x1f) << shift;
            shift += 5;
        } while (b >= 0x20);
        let dlat = ((result & 1) ? ~(result >> 1) : (result >> 1));
        lat += dlat;
        
        shift = 0;
        result = 0;
        do {
            b = encoded.charCodeAt(index++) - 63;
            result |= (b & 0x1f) << shift;
            shift += 5;
        } while (b >= 0x20);
        let dlng = ((result & 1) ? ~(result >> 1) : (result >> 1));
        lng += dlng;
        
        points.push([lng / 1e5, lat / 1e5]); // [lng, lat] for GeoJSON
    }
    return points;
}

/**
 * Draws the polyline route on the map and fits bounds.
 */
function drawRoute(encodedPolyline) {
    if (!mapInstance) return;
    
    const coordinates = decodePolyline(encodedPolyline);

    if (mapInstance.getSource('route')) {
        mapInstance.getSource('route').setData({
            type: 'Feature',
            properties: {},
            geometry: { type: 'LineString', coordinates: coordinates }
        });
    } else {
        mapInstance.addSource('route', {
            type: 'geojson',
            data: {
                type: 'Feature',
                properties: {},
                geometry: { type: 'LineString', coordinates: coordinates }
            }
        });

        mapInstance.addLayer({
            id: 'route',
            type: 'line',
            source: 'route',
            layout: {
                'line-join': 'round',
                'line-cap': 'round'
            },
            paint: {
                'line-color': '#3b82f6',
                'line-width': 5,
                'line-opacity': 0.8
            }
        });
    }

    // Fit bounds to route
    const bounds = coordinates.reduce((b, coord) => {
        return b.extend(coord);
    }, new maplibregl.LngLatBounds(coordinates[0], coordinates[0]));
    
    mapInstance.fitBounds(bounds, { padding: 80, duration: 1000 });
}

/**
 * Helper to remove route and restaurant marker
 */
function clearRouteAndRestaurant() {
    if (!mapInstance) return;
    
    if (mapInstance.getLayer('route')) {
        mapInstance.removeLayer('route');
        mapInstance.removeSource('route');
    }
    if (currentRestaurantMarker) {
        currentRestaurantMarker.remove();
        currentRestaurantMarker = null;
    }
}

/**
 * Resets the entire map state.
 */
function resetMap() {
    Object.values(driverMarkers).forEach(m => m.marker.remove());
    driverMarkers = {};
    
    clearRouteAndRestaurant();
    
    const logContainer = document.getElementById('status-log');
    if (logContainer) {
        logContainer.innerHTML = '';
    }
    logStatus("System initialized. Waiting for commands...", "info");
    
    if (mapInstance) {
        mapInstance.flyTo({ center: [CENTER_LNG, CENTER_LAT], zoom: 13, pitch: 45 });
    }
}

// Bootstrap application once DOM is ready
document.addEventListener('DOMContentLoaded', initApp);
