package ua.vbielskyi.bmf.core.service.location;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ua.vbielskyi.bmf.core.entity.tenant.TenantLocationEntity;
import ua.vbielskyi.bmf.core.exception.LocationServiceException;
import ua.vbielskyi.bmf.core.repository.tenant.TenantLocationRepository;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationService {

    private final RestTemplate restTemplate;
    private final TenantLocationRepository locationRepository;

    @Value("${google.maps.api-key}")
    private String googleMapsApiKey;

    /**
     * Find nearest tenant locations to a customer's coordinates
     */
    public List<TenantLocationEntity> findNearbyLocations(UUID tenantId, double latitude, double longitude, int maxResults) {
        List<TenantLocationEntity> allLocations = locationRepository.findAllByTenantIdAndActiveTrue(tenantId);

        // Calculate distances and sort
        List<LocationWithDistance> locationsWithDistances = new ArrayList<>();
        for (TenantLocationEntity location : allLocations) {
            if (location.getLatitude() != null && location.getLongitude() != null) {
                double distance = calculateDistance(
                        latitude, longitude,
                        location.getLatitude(), location.getLongitude());

                locationsWithDistances.add(new LocationWithDistance(location, distance));
            }
        }

        // Sort by distance
        locationsWithDistances.sort(Comparator.comparingDouble(LocationWithDistance::getDistance));

        // Return top N results
        return locationsWithDistances.stream()
                .limit(maxResults)
                .map(LocationWithDistance::getLocation)
                .toList();
    }

    /**
     * Estimate delivery time based on distance
     */
    public int estimateDeliveryTime(double customerLatitude, double customerLongitude,
                                    double storeLatitude, double storeLongitude) {
        // Calculate distance
        double distanceKm = calculateDistance(
                customerLatitude, customerLongitude,
                storeLatitude, storeLongitude);

        // Base delivery time: 30 minutes + 5 minutes per km
        int baseTimeMinutes = 30 + (int)(distanceKm * 5);

        // Add buffer for traffic
        int trafficBuffer = (int)(baseTimeMinutes * 0.2); // 20% buffer

        return baseTimeMinutes + trafficBuffer;
    }

    /**
     * Geocode an address to coordinates
     */
    public Map<String, Double> geocodeAddress(String address) {
        try {
            String encodedAddress = address.replace(" ", "+");
            String url = String.format(
                    "https://maps.googleapis.com/maps/api/geocode/json?address=%s&key=%s",
                    encodedAddress, googleMapsApiKey);

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> responseBody = response.getBody();

            if (responseBody != null && "OK".equals(responseBody.get("status"))) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> results = (List<Map<String, Object>>) responseBody.get("results");

                if (!results.isEmpty()) {
                    Map<String, Object> location = (Map<String, Object>)
                            ((Map<String, Object>) results.get(0).get("geometry")).get("location");

                    Map<String, Double> coordinates = new HashMap<>();
                    coordinates.put("latitude", (Double) location.get("lat"));
                    coordinates.put("longitude", (Double) location.get("lng"));

                    return coordinates;
                }
            }

            throw new LocationServiceException("Could not geocode address: " + address);
        } catch (Exception e) {
            log.error("Error geocoding address: {}", address, e);
            throw new LocationServiceException("Error geocoding address: " + e.getMessage(), e);
        }
    }

    /**
     * Calculate distance between two coordinates using Haversine formula
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in kilometers

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    /**
     * Helper class to store location with calculated distance
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class LocationWithDistance {
        private TenantLocationEntity location;
        private double distance;
    }
}