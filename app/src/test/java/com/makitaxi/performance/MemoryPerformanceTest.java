package com.makitaxi.performance;

import android.content.Context;

import com.makitaxi.config.AppConfig;
import com.makitaxi.model.RideRequest;
import com.makitaxi.model.User;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class MemoryPerformanceTest {

    private static final int NUM_OBJECTS = 1000;
    private static final long MAX_MEMORY_PER_OBJECT_BYTES = 4096; // 4KB per object
    
    private Context context;
    private Runtime runtime;
    
    @Before
    public void setup() {
        context = RuntimeEnvironment.getApplication();
        runtime = Runtime.getRuntime();
        System.out.println("\n=== Memory Performance Tests ===");
    }
    
    @Test
    public void testUserObjectMemoryUsage() {
        System.out.println("\nTesting User object memory usage with " + NUM_OBJECTS + " objects");
        runtime.gc();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        
        List<User> users = new ArrayList<>();
        for (int i = 0; i < NUM_OBJECTS; i++) {
            User user = new User(
                "Test User " + i,
                "user" + i + "@test.com",
                "+38160" + String.format("%06d", i),
                "user" + i
            );
            user.setRole(AppConfig.ROLE_DRIVER);
            user.setCarType(AppConfig.CAR_TYPE_BASIC);
            user.setCarModel("Toyota Corolla");
            user.setCarColor("Black");
            user.setCarPlateNumber("BG-" + i + "-AB");
            user.setLicenseNumber("12345" + i);
            user.setAvailable(true);
            users.add(user);
        }
        
        runtime.gc();
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long totalMemoryUsed = memoryAfter - memoryBefore;
        long memoryPerObject = totalMemoryUsed / NUM_OBJECTS;
        
        System.out.println("Memory Usage Results:");
        System.out.println("- Total memory used: " + formatBytes(totalMemoryUsed));
        System.out.println("- Memory per User object: " + formatBytes(memoryPerObject));
        System.out.println("- Maximum allowed: " + formatBytes(MAX_MEMORY_PER_OBJECT_BYTES));
        
        assertTrue("Memory usage per User object too high: " + memoryPerObject + " bytes",
                  memoryPerObject <= MAX_MEMORY_PER_OBJECT_BYTES);
    }
    
    @Test
    public void testRideRequestObjectMemoryUsage() {
        System.out.println("\nTesting RideRequest object memory usage with " + NUM_OBJECTS + " objects");
        runtime.gc();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        
        List<RideRequest> requests = new ArrayList<>();
        for (int i = 0; i < NUM_OBJECTS; i++) {
            RideRequest request = new RideRequest(
                "passenger" + i,
                44.7866 + (i * 0.0001), 20.4489 + (i * 0.0001),
                44.8125 + (i * 0.0001), 20.4612 + (i * 0.0001),
                "Test Pickup " + i,
                "Test Dropoff " + i,
                AppConfig.CAR_TYPE_BASIC,
                5.0, // distance
                15.0 // duration
            );
            requests.add(request);
        }
        
        runtime.gc();
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long totalMemoryUsed = memoryAfter - memoryBefore;
        long memoryPerObject = totalMemoryUsed / NUM_OBJECTS;
        
        System.out.println("Memory Usage Results:");
        System.out.println("- Total memory used: " + formatBytes(totalMemoryUsed));
        System.out.println("- Memory per RideRequest object: " + formatBytes(memoryPerObject));
        System.out.println("- Maximum allowed: " + formatBytes(MAX_MEMORY_PER_OBJECT_BYTES));
        
        assertTrue("Memory usage per RideRequest object too high: " + memoryPerObject + " bytes",
                  memoryPerObject <= MAX_MEMORY_PER_OBJECT_BYTES);
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }
}