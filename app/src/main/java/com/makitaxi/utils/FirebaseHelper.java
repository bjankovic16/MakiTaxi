package com.makitaxi.utils;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.makitaxi.config.AppConfig;

/**
 * Firebase Database helper class
 * Provides centralized access to Firebase database references
 */
public class FirebaseHelper {

    private static final DatabaseReference rootRef = FirebaseDatabase.getInstance(AppConfig.FIREBASE_DATABASE_URL).getReference();

    private FirebaseHelper() {
        // Private constructor to prevent instantiation
    }

    /**
     * Get reference to ride requests node
     */
    public static DatabaseReference getRideRequestsRef() {
        return rootRef.child(AppConfig.NODE_RIDE_REQUESTS);
    }

    /**
     * Get reference to users node
     */
    public static DatabaseReference getUserRequestsRef() {
        return rootRef.child(AppConfig.NODE_USERS);
    }

    /**
     * Get reference to driver locations node
     */
    public static DatabaseReference getDriverLocationRef() {
        return rootRef.child(AppConfig.NODE_DRIVER_LOCATIONS);
    }

    /**
     * Get reference to driver notifications node
     */
    public static DatabaseReference getDriverNotificationRef() {
        return rootRef.child(AppConfig.NODE_DRIVER_NOTIFICATIONS);
    }

    /**
     * Get reference to passenger response node
     */
    public static DatabaseReference getPassengerResponseRef() {
        return rootRef.child(AppConfig.NODE_PASSENGER_RESPONSE);
    }

    /**
     * Get reference to feedback requests node
     */
    public static DatabaseReference getFeedbackRequestsRef() {
        return rootRef.child(AppConfig.NODE_FEEDBACK_REQUESTS);
    }

    /**
     * Get reference to root database
     */
    public static DatabaseReference getRootRef() {
        return rootRef;
    }
}
