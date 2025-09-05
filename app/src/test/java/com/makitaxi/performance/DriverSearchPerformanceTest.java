package com.makitaxi.performance;

import com.firebase.geofire.GeoLocation;
import com.makitaxi.config.AppConfig;
import com.makitaxi.model.RideRequest;
import com.makitaxi.model.User;
import com.makitaxi.utils.DriverPollingService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class DriverSearchPerformanceTest {
    
    private static final int NUM_DRIVERS = 1000;
    private static final int NUM_REQUESTS = 100;
    private static final long MAX_SEARCH_TIME_MS = 1000;
    private static final double BELGRADE_CENTER_LAT = 44.7866;
    private static final double BELGRADE_CENTER_LON = 20.4489;
    private static final double SEARCH_RADIUS_KM = 10.0;
    
    private List<User> drivers;
    private List<RideRequest> requests;
    private Random random;
    
    @Before
    public void setup() {
        random = new Random();
        System.out.println("\n=== Generating test data ===");
        System.out.println("Generating " + NUM_DRIVERS + " drivers...");
        drivers = generateRandomDrivers();
        System.out.println("Generating " + NUM_REQUESTS + " requests...");
        requests = generateRandomRequests();
    }
    
    @Test
    public void testDriverSearchPerformance() {
        System.out.println("\n=== Single Search Performance Test ===");
        System.out.println("Testing " + NUM_REQUESTS + " sequential searches");
        
        long totalSearchTime = 0;
        int totalDriversFound = 0;
        long minSearchTime = Long.MAX_VALUE;
        long maxSearchTime = 0;
        
        for (RideRequest request : requests) {
            long startTime = System.nanoTime();
            
            List<User> foundDrivers = findNearbyDrivers(
                request.getPickupLatitude(),
                request.getPickupLongitude(),
                SEARCH_RADIUS_KM
            );
            
            long endTime = System.nanoTime();
            long searchTime = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
            
            totalSearchTime += searchTime;
            totalDriversFound += foundDrivers.size();
            minSearchTime = Math.min(minSearchTime, searchTime);
            maxSearchTime = Math.max(maxSearchTime, searchTime);
            
            assertTrue("Search took too long: " + searchTime + "ms", 
                      searchTime <= MAX_SEARCH_TIME_MS);
        }
        
        double avgSearchTime = totalSearchTime / (double)NUM_REQUESTS;
        double avgDriversFound = totalDriversFound / (double)NUM_REQUESTS;
        
        System.out.println("Performance Results:");
        System.out.println("- Minimum search time: " + minSearchTime + "ms");
        System.out.println("- Maximum search time: " + maxSearchTime + "ms");
        System.out.println("- Average search time: " + String.format("%.6f", avgSearchTime) + "ms");
        System.out.println("- Average drivers found: " + String.format("%.2f", avgDriversFound));
        
        assertTrue("Average search time too high: " + avgSearchTime + "ms",
                  avgSearchTime <= MAX_SEARCH_TIME_MS);
    }
    
    @Test
    public void testConcurrentSearchPerformance() throws InterruptedException {
        System.out.println("\n=== Concurrent Search Performance Test ===");
        int numThreads = 10;
        System.out.println("Testing " + numThreads + " concurrent searches");
        
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numThreads);
        List<Long> searchTimes = new ArrayList<>();
        List<Integer> driversFoundList = new ArrayList<>();
        
        for (int i = 0; i < numThreads; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    
                    long startTime = System.nanoTime();
                    List<User> foundDrivers = findNearbyDrivers(
                        BELGRADE_CENTER_LAT,
                        BELGRADE_CENTER_LON,
                        SEARCH_RADIUS_KM
                    );
                    long endTime = System.nanoTime();
                    
                    synchronized (searchTimes) {
                        searchTimes.add(TimeUnit.NANOSECONDS.toMillis(endTime - startTime));
                        driversFoundList.add(foundDrivers.size());
                    }
                    
                    endLatch.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
        
        startLatch.countDown();
        endLatch.await(5, TimeUnit.SECONDS);
        
        long minSearchTime = searchTimes.stream().mapToLong(Long::longValue).min().orElse(0);
        long maxSearchTime = searchTimes.stream().mapToLong(Long::longValue).max().orElse(0);
        double avgSearchTime = searchTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double avgDriversFound = driversFoundList.stream().mapToInt(Integer::intValue).average().orElse(0.0);
            
        System.out.println("Concurrent Performance Results:");
        System.out.println("- Minimum search time: " + minSearchTime + "ms");
        System.out.println("- Maximum search time: " + maxSearchTime + "ms");
        System.out.println("- Average search time: " + String.format("%.6f", avgSearchTime) + "ms");
        System.out.println("- Average drivers found: " + String.format("%.2f", avgDriversFound));
        
        assertTrue("Concurrent search time too high: " + avgSearchTime + "ms",
                  avgSearchTime <= MAX_SEARCH_TIME_MS);
    }
    
    private List<User> generateRandomDrivers() {
        List<User> drivers = new ArrayList<>();
        for (int i = 0; i < NUM_DRIVERS; i++) {
            User driver = new User(
                "Driver " + i,
                "driver" + i + "@test.com",
                "+38160" + String.format("%06d", i),
                "driver" + i
            );
            
            driver.setRole(AppConfig.ROLE_DRIVER);
            driver.setCarType(getRandomCarType());
            driver.setAvailable(true);
            
            // Random location within 10km of Belgrade center
            double lat = BELGRADE_CENTER_LAT + (random.nextDouble() - 0.5) * 0.2;
            double lon = BELGRADE_CENTER_LON + (random.nextDouble() - 0.5) * 0.2;
            
            drivers.add(driver);
        }
        return drivers;
    }
    
    private List<RideRequest> generateRandomRequests() {
        List<RideRequest> requests = new ArrayList<>();
        for (int i = 0; i < NUM_REQUESTS; i++) {
            // Random pickup location within 5km of Belgrade center
            double pickupLat = BELGRADE_CENTER_LAT + (random.nextDouble() - 0.5) * 0.1;
            double pickupLon = BELGRADE_CENTER_LON + (random.nextDouble() - 0.5) * 0.1;
            
            // Random dropoff location within 10km
            double dropoffLat = pickupLat + (random.nextDouble() - 0.5) * 0.2;
            double dropoffLon = pickupLon + (random.nextDouble() - 0.5) * 0.2;
            
            RideRequest request = new RideRequest(
                "passenger" + i,
                pickupLat, pickupLon,
                dropoffLat, dropoffLon,
                "Test Pickup " + i,
                "Test Dropoff " + i,
                getRandomCarType(),
                5.0, // distance
                15.0 // duration
            );
            
            requests.add(request);
        }
        return requests;
    }
    
    private String getRandomCarType() {
        String[] types = {
            AppConfig.CAR_TYPE_BASIC,
            AppConfig.CAR_TYPE_LUXURY,
            AppConfig.CAR_TYPE_TRANSPORT
        };
        return types[random.nextInt(types.length)];
    }
    
    private List<User> findNearbyDrivers(double lat, double lon, double radiusKm) {
        List<User> nearbyDrivers = new ArrayList<>();
        GeoLocation center = new GeoLocation(lat, lon);
        
        for (User driver : drivers) {
            // Simple distance calculation for testing
            double dlat = Math.abs(lat - BELGRADE_CENTER_LAT);
            double dlon = Math.abs(lon - BELGRADE_CENTER_LON);
            double distance = Math.sqrt(dlat * dlat + dlon * dlon) * 111.0; // rough km conversion
            
            if (distance <= radiusKm) {
                nearbyDrivers.add(driver);
            }
        }
        
        return nearbyDrivers;
    }
}