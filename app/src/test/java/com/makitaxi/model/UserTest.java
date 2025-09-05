package com.makitaxi.model;

import com.makitaxi.config.AppConfig;
import org.junit.Test;
import static org.junit.Assert.*;

public class UserTest {

    @Test
    public void testUserCreation() {
        User user = new User(
            "Test Driver",
            "test@driver.com",
            "+381601234567",
            "testdriver"
        );

        assertEquals("Test Driver", user.getFullName());
        assertEquals("test@driver.com", user.getEmail());
        assertEquals("+381601234567", user.getPhone());
        assertEquals("testdriver", user.getUsername());
        
        assertEquals("PASSENGER", user.getRole());
        assertFalse(user.isVerified());
        assertEquals(0.0, user.getRating(), 0.01);
        assertEquals(0, user.getTotalRides());
    }

    @Test
    public void testDriverSpecificFields() {
        User driver = new User(
            "Test Driver",
            "test@driver.com",
            "+381601234567",
            "testdriver"
        );
        
        driver.setRole(AppConfig.ROLE_DRIVER);
        driver.setCarType(AppConfig.CAR_TYPE_BASIC);
        driver.setCarModel("Toyota Corolla");
        driver.setCarColor("Black");
        driver.setCarPlateNumber("BG-123-AB");
        driver.setLicenseNumber("12345");
        driver.setAvailable(true);

        assertEquals(AppConfig.ROLE_DRIVER, driver.getRole());
        assertEquals(AppConfig.CAR_TYPE_BASIC, driver.getCarType());
        assertEquals("Toyota Corolla", driver.getCarModel());
        assertEquals("Black", driver.getCarColor());
        assertEquals("BG-123-AB", driver.getCarPlateNumber());
        assertEquals("12345", driver.getLicenseNumber());
        assertTrue(driver.isAvailable());
    }

    @Test
    public void testRatingCalculation() {
        User user = new User(
            "Test User",
            "test@user.com",
            "+381601234567",
            "testuser"
        );

        assertEquals(0.0, user.getRating(), 0.01);
        assertEquals(0, user.getRatingCount());

        user.setRatingCount(3);
        user.setTotalRatingSum(15.0); // 3 ratings of 5 stars each

        double expectedRating = 15.0 / 3.0; // 5.0
        assertEquals(expectedRating, user.getRating(), 0.01);
    }

    @Test
    public void testRideStatistics() {
        User user = new User(
            "Test User",
            "test@user.com",
            "+381601234567",
            "testuser"
        );

        assertEquals(0, user.getTotalRides());
        assertEquals(0, user.getTotalDistance());
        assertEquals(0.0, user.getTotalMoneySpent(), 0.01);
        assertEquals(0.0, user.getTotalMoneyEarned(), 0.01);

        user.setTotalRides(10);
        user.setTotalDistance(100);
        user.setTotalMoneySpent(1500.0);
        user.setTotalMoneyEarned(2000.0);

        assertEquals(10, user.getTotalRides());
        assertEquals(100, user.getTotalDistance());
        assertEquals(1500.0, user.getTotalMoneySpent(), 0.01);
        assertEquals(2000.0, user.getTotalMoneyEarned(), 0.01);
    }
}