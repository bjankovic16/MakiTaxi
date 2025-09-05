package com.makitaxi.utils;

import org.junit.Test;
import static org.junit.Assert.*;

public class NotificationStatusTest {

    @Test
    public void testAllStatusesExist() {
        // Test that all required statuses are defined
        assertNotNull(NotificationStatus.CREATED);
        assertNotNull(NotificationStatus.CANCELLED_BY_PASSENGER);
        assertNotNull(NotificationStatus.NO_AVAILABLE_DRIVERS);
        assertNotNull(NotificationStatus.ALL_DRIVERS_DECLINED);
        assertNotNull(NotificationStatus.CANCELLED_BY_DRIVER);
        assertNotNull(NotificationStatus.CANCELLED_BY_DRIVER_WHILE_WAITING);
        assertNotNull(NotificationStatus.CANCELLED_BY_DRIVER_DURING_RIDE);
        assertNotNull(NotificationStatus.ACCEPTED_BY_DRIVER);
        assertNotNull(NotificationStatus.ACCEPTED_BY_PASSENGER);
        assertNotNull(NotificationStatus.DECLINED_BY_PASSENGER);
        assertNotNull(NotificationStatus.TIMEOUT);
        assertNotNull(NotificationStatus.FINISHED);
        assertNotNull(NotificationStatus.DRIVER_EXITED_APP);
        assertNotNull(NotificationStatus.PASSENGER_EXITED_APP);
    }

    @Test
    public void testStatusTransitions() {
        // Test valid status transitions
        assertTrue(isValidTransition(NotificationStatus.CREATED, NotificationStatus.ACCEPTED_BY_DRIVER));
        assertTrue(isValidTransition(NotificationStatus.CREATED, NotificationStatus.NO_AVAILABLE_DRIVERS));
        assertTrue(isValidTransition(NotificationStatus.ACCEPTED_BY_DRIVER, NotificationStatus.ACCEPTED_BY_PASSENGER));
        assertTrue(isValidTransition(NotificationStatus.ACCEPTED_BY_PASSENGER, NotificationStatus.FINISHED));
        
        // Test invalid status transitions
        assertFalse(isValidTransition(NotificationStatus.FINISHED, NotificationStatus.CREATED));
        assertFalse(isValidTransition(NotificationStatus.CANCELLED_BY_PASSENGER, NotificationStatus.ACCEPTED_BY_DRIVER));
    }

    private boolean isValidTransition(NotificationStatus from, NotificationStatus to) {
        switch (from) {
            case CREATED:
                return to == NotificationStatus.ACCEPTED_BY_DRIVER ||
                       to == NotificationStatus.NO_AVAILABLE_DRIVERS ||
                       to == NotificationStatus.CANCELLED_BY_PASSENGER;
            
            case ACCEPTED_BY_DRIVER:
                return to == NotificationStatus.ACCEPTED_BY_PASSENGER ||
                       to == NotificationStatus.DECLINED_BY_PASSENGER ||
                       to == NotificationStatus.CANCELLED_BY_DRIVER;
            
            case ACCEPTED_BY_PASSENGER:
                return to == NotificationStatus.FINISHED ||
                       to == NotificationStatus.CANCELLED_BY_DRIVER_DURING_RIDE ||
                       to == NotificationStatus.CANCELLED_BY_PASSENGER;
            
            default:
                return false;
        }
    }
}

