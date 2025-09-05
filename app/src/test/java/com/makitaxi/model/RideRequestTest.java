package com.makitaxi.model;

import com.makitaxi.config.AppConfig;
import com.makitaxi.utils.NotificationStatus;

import org.junit.Test;
import static org.junit.Assert.*;

public class RideRequestTest {

    @Test
    public void testRideRequestCreation() {
        RideRequest request = new RideRequest(
            "testPassenger",
            44.7866, 20.4489,  // pickup (Belgrade center)
            44.8125, 20.4612,  // dropoff
            "Terazije 23, Belgrade",
            "Knez Mihailova 54, Belgrade",
            AppConfig.CAR_TYPE_BASIC,
            5.0,    // distance in km
            15.0    // duration in minutes
        );

        // Test initial state
        assertEquals(NotificationStatus.CREATED, request.getStatus());
        assertEquals("testPassenger", request.getPassengerId());
        assertEquals(AppConfig.CAR_TYPE_BASIC, request.getCarType());
        
        // Test location data
        assertEquals(44.7866, request.getPickupLatitude(), 0.0001);
        assertEquals(20.4489, request.getPickupLongitude(), 0.0001);
        assertEquals(44.8125, request.getDropoffLatitude(), 0.0001);
        assertEquals(20.4612, request.getDropoffLongitude(), 0.0001);
        
        // Test addresses
        assertEquals("Terazije 23, Belgrade", request.getPickupAddress());
        assertEquals("Knez Mihailova 54, Belgrade", request.getDropoffAddress());
        
        // Test price calculation
        double expectedPrice = AppConfig.BASE_PRICE_RSD + (5.0 * AppConfig.BASIC_PRICE_PER_KM);
        assertEquals(expectedPrice, request.getEstimatedPrice(), 0.01);
    }

    @Test
    public void testRideRequestStatusTransitions() {
        RideRequest request = new RideRequest(
            "testPassenger",
            44.7866, 20.4489,
            44.8125, 20.4612,
            "Test Pickup",
            "Test Dropoff",
            AppConfig.CAR_TYPE_BASIC,
            5.0,
            15.0
        );

        // Test initial status
        assertEquals(NotificationStatus.CREATED, request.getStatus());

        // Test driver acceptance
        request.setStatus(NotificationStatus.ACCEPTED_BY_DRIVER);
        assertEquals(NotificationStatus.ACCEPTED_BY_DRIVER, request.getStatus());

        // Test passenger acceptance
        request.setStatus(NotificationStatus.ACCEPTED_BY_PASSENGER);
        assertEquals(NotificationStatus.ACCEPTED_BY_PASSENGER, request.getStatus());

        // Test ride completion
        request.setStatus(NotificationStatus.FINISHED);
        assertEquals(NotificationStatus.FINISHED, request.getStatus());
    }

    @Test
    public void testPriceCalculationForDifferentCarTypes() {
        // Test BASIC car type
        RideRequest basicRequest = new RideRequest(
            "testPassenger", 0, 0, 0, 0, "", "", 
            AppConfig.CAR_TYPE_BASIC, 10.0, 30.0
        );
        double basicExpectedPrice = AppConfig.BASE_PRICE_RSD + (10.0 * AppConfig.BASIC_PRICE_PER_KM);
        assertEquals(basicExpectedPrice, basicRequest.getEstimatedPrice(), 0.01);

        // Test LUXURY car type
        RideRequest luxuryRequest = new RideRequest(
            "testPassenger", 0, 0, 0, 0, "", "", 
            AppConfig.CAR_TYPE_LUXURY, 10.0, 30.0
        );
        double luxuryExpectedPrice = AppConfig.BASE_PRICE_RSD + (10.0 * AppConfig.LUXURY_PRICE_PER_KM);
        assertEquals(luxuryExpectedPrice, luxuryRequest.getEstimatedPrice(), 0.01);

        // Test TRANSPORT car type
        RideRequest transportRequest = new RideRequest(
            "testPassenger", 0, 0, 0, 0, "", "", 
            AppConfig.CAR_TYPE_TRANSPORT, 10.0, 30.0
        );
        double transportExpectedPrice = AppConfig.BASE_PRICE_RSD + (10.0 * AppConfig.TRANSPORT_PRICE_PER_KM);
        assertEquals(transportExpectedPrice, transportRequest.getEstimatedPrice(), 0.01);
    }
}

