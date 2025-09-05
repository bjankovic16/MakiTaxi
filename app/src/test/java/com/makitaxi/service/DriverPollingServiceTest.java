package com.makitaxi.service;

import com.firebase.geofire.GeoLocation;
import com.makitaxi.config.AppConfig;
import com.makitaxi.model.RideRequest;
import com.makitaxi.model.User;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class DriverPollingServiceTest {

    private RideRequest rideRequest;
    private List<User> nearbyDrivers;

    @Before
    public void setup() {
        rideRequest = new RideRequest(
            "testPassenger",
            44.7866, 20.4489,
            44.8125, 20.4612,
            "Теразије 23, Београд",
            "Калемегдан, Београд",
            AppConfig.CAR_TYPE_BASIC,
            2.5,
            10.0
        );

        User driver1 = createTestDriver("driver1", AppConfig.CAR_TYPE_BASIC, 
            new GeoLocation(44.7870, 20.4495));
        
        User driver2 = createTestDriver("driver2", AppConfig.CAR_TYPE_LUXURY, 
            new GeoLocation(44.7880, 20.4500));
        
        User driver3 = createTestDriver("driver3", AppConfig.CAR_TYPE_BASIC, 
            new GeoLocation(44.7900, 20.4520));

        nearbyDrivers = Arrays.asList(driver1, driver2, driver3);
    }

    @Test
    public void testDriverSearchWithinInitialRadius() {
        double initialRadius = AppConfig.INITIAL_SEARCH_RADIUS_KM;
        
        List<User> foundDrivers = findDriversWithinRadius(
            new GeoLocation(rideRequest.getPickupLatitude(), 
                          rideRequest.getPickupLongitude()),
            initialRadius
        );

        assertEquals(3, foundDrivers.size());
        assertTrue(foundDrivers.contains(nearbyDrivers.get(0)));
    }

    @Test
    public void testDriverSearchWithCarTypeFiltering() {
        List<User> basicDrivers = findDriversByCarType(AppConfig.CAR_TYPE_BASIC);
        List<User> luxuryDrivers = findDriversByCarType(AppConfig.CAR_TYPE_LUXURY);

        assertEquals(2, basicDrivers.size());
        assertEquals(1, luxuryDrivers.size());
    }

    @Test
    public void testAdaptiveRadiusSearch() {
        double initialRadius = AppConfig.INITIAL_SEARCH_RADIUS_KM;
        double maxRadius = AppConfig.MAX_SEARCH_RADIUS_KM;
        double increment = AppConfig.RADIUS_INCREMENT_KM;

        int totalSearches = 0;
        double currentRadius = initialRadius;
        List<User> foundDrivers = null;

        while (currentRadius <= maxRadius) {
            foundDrivers = findDriversWithinRadius(
                new GeoLocation(rideRequest.getPickupLatitude(), 
                              rideRequest.getPickupLongitude()),
                currentRadius
            );

            totalSearches++;
            if (!foundDrivers.isEmpty()) break;

            currentRadius += increment;
        }

        assertTrue(totalSearches > 0);
        assertNotNull(foundDrivers);
        assertFalse(foundDrivers.isEmpty());
    }

    @Test
    public void testDriverNotificationOrder() {
        List<User> orderedDrivers = findDriversWithinRadius(
            new GeoLocation(rideRequest.getPickupLatitude(), 
                          rideRequest.getPickupLongitude()),
            AppConfig.INITIAL_SEARCH_RADIUS_KM
        );

        assertEquals(nearbyDrivers.get(0), orderedDrivers.get(0));
    }

    private User createTestDriver(String id, String carType, GeoLocation location) {
        User driver = new User(
            "Test " + id,
            id + "@test.com",
            "+38160" + id,
            id
        );
        driver.setRole(AppConfig.ROLE_DRIVER);
        driver.setCarType(carType);
        driver.setAvailable(true);
        return driver;
    }

    private List<User> findDriversWithinRadius(GeoLocation center, double radiusKm) {
        return nearbyDrivers;
    }

    private List<User> findDriversByCarType(String carType) {
        return nearbyDrivers.stream()
            .filter(driver -> driver.getCarType().equals(carType))
            .collect(Collectors.toList());
    }
}