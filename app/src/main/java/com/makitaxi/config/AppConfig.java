package com.makitaxi.config;

/**
 * Application configuration constants
 * This class contains all configuration values used throughout the app
 */
public class AppConfig {
    
    // Firebase Database Configuration
    public static final String FIREBASE_DATABASE_URL = "https://makitaxi-e4108-default-rtdb.europe-west1.firebasedatabase.app/";
    
    // Database Node Names
    public static final String NODE_USERS = "users";
    public static final String NODE_RIDE_REQUESTS = "ride_requests";
    public static final String NODE_DRIVER_LOCATIONS = "driver_locations";
    public static final String NODE_DRIVER_NOTIFICATIONS = "driver_notifications";
    public static final String NODE_PASSENGER_RESPONSE = "passenger_response";
    public static final String NODE_FEEDBACK_REQUESTS = "feedback_requests";
    
    // App Constants
    public static final int MIN_RIDE_DISTANCE_KM = 1;
    public static final int MIN_FEEDBACK_CHARACTERS = 20;
    public static final int MAX_DRIVERS_TO_NOTIFY = 10;
    public static final double INITIAL_SEARCH_RADIUS_KM = 2.0;
    public static final double MAX_SEARCH_RADIUS_KM = 10.0;
    public static final double RADIUS_INCREMENT_KM = 2.0;
    
    // Car Types
    public static final String CAR_TYPE_BASIC = "BASIC";
    public static final String CAR_TYPE_LUXURY = "LUXURY";
    public static final String CAR_TYPE_TRANSPORT = "TRANSPORT";
    
    // User Roles
    public static final String ROLE_DRIVER = "DRIVER";
    public static final String ROLE_PASSENGER = "PASSENGER";
    
    // Pricing Configuration
    public static final double BASE_PRICE_RSD = 150.0;
    public static final double BASIC_PRICE_PER_KM = 80.0;
    public static final double LUXURY_PRICE_PER_KM = 120.0;
    public static final double TRANSPORT_PRICE_PER_KM = 100.0;
    
    private AppConfig() {
        // Private constructor to prevent instantiation
    }
}
