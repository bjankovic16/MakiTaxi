package com.makitaxi.integration;

import com.makitaxi.config.AppConfig;
import com.makitaxi.model.RideRequest;
import com.makitaxi.model.User;
import com.makitaxi.utils.NotificationStatus;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class RideLifecycleTest {

    private RideRequest rideRequest;
    private User passenger;
    private User driver;

    @Before
    public void setup() {
        passenger = new User("Test Passenger", "passenger@test.com", "+381601234567", "testpassenger");
        passenger.setRole(AppConfig.ROLE_PASSENGER);

        driver = new User("Test Driver", "driver@test.com", "+381601234568", "testdriver");
        driver.setRole(AppConfig.ROLE_DRIVER);
        driver.setCarType(AppConfig.CAR_TYPE_BASIC);
        driver.setAvailable(true);

        rideRequest = new RideRequest(
            passenger.getUsername(),
            44.7866, 20.4489,
            44.8125, 20.4612,
            "Теразије 23, Београд",
            "Калемегдан, Београд",
            AppConfig.CAR_TYPE_BASIC,
            2.5,
            10.0
        );
    }

    @Test
    public void testCompleteRideLifecycle() {
        assertEquals(NotificationStatus.CREATED, rideRequest.getStatus());
        assertTrue(rideRequest.getEstimatedPrice() > 0);

        rideRequest.setStatus(NotificationStatus.ACCEPTED_BY_DRIVER);
        rideRequest.setDriverId(driver.getUsername());
        assertEquals(driver.getUsername(), rideRequest.getDriverId());

        rideRequest.setStatus(NotificationStatus.ACCEPTED_BY_PASSENGER);
        
        driver.setTotalRides(driver.getTotalRides() + 1);
        driver.setTotalDistance(driver.getTotalDistance() + (int)rideRequest.getDistance());
        passenger.setTotalRides(passenger.getTotalRides() + 1);
        passenger.setTotalDistance(passenger.getTotalDistance() + (int)rideRequest.getDistance());

        rideRequest.setStatus(NotificationStatus.FINISHED);
        
        assertEquals(NotificationStatus.FINISHED, rideRequest.getStatus());
        assertEquals(1, driver.getTotalRides());
        assertEquals(1, passenger.getTotalRides());
        assertTrue(driver.getTotalDistance() > 0);
        assertTrue(passenger.getTotalDistance() > 0);
    }

    @Test
    public void testRideLifecycleWithCancellation() {
        assertEquals(NotificationStatus.CREATED, rideRequest.getStatus());

        rideRequest.setStatus(NotificationStatus.ACCEPTED_BY_DRIVER);
        rideRequest.setDriverId(driver.getUsername());

        rideRequest.setStatus(NotificationStatus.ACCEPTED_BY_PASSENGER);

        rideRequest.setStatus(NotificationStatus.CANCELLED_BY_DRIVER_DURING_RIDE);
        
        assertEquals(NotificationStatus.CANCELLED_BY_DRIVER_DURING_RIDE, rideRequest.getStatus());
        assertEquals(0, driver.getTotalRides());
        assertEquals(0, passenger.getTotalRides());
    }

    @Test
    public void testRideLifecycleWithTimeout() {
        assertEquals(NotificationStatus.CREATED, rideRequest.getStatus());

        rideRequest.setStatus(NotificationStatus.TIMEOUT);
        rideRequest.setStatus(NotificationStatus.NO_AVAILABLE_DRIVERS);
        
        assertEquals(NotificationStatus.NO_AVAILABLE_DRIVERS, rideRequest.getStatus());
    }
}