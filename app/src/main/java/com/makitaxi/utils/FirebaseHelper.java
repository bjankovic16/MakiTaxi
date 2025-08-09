package com.makitaxi.utils;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.makitaxi.config.AppConfig;

public class FirebaseHelper {

    private static final DatabaseReference rootRef = FirebaseDatabase.getInstance(AppConfig.FIREBASE_DATABASE_URL).getReference();

    private FirebaseHelper() {
    }

    public static DatabaseReference getRideRequestsRef() {
        return rootRef.child(AppConfig.NODE_RIDE_REQUESTS);
    }

    public static DatabaseReference getUserRequestsRef() {
        return rootRef.child(AppConfig.NODE_USERS);
    }

    public static DatabaseReference getDriverLocationRef() {
        return rootRef.child(AppConfig.NODE_DRIVER_LOCATIONS);
    }

    public static DatabaseReference getDriverNotificationRef() {
        return rootRef.child(AppConfig.NODE_DRIVER_NOTIFICATIONS);
    }

    public static DatabaseReference getPassengerResponseRef() {
        return rootRef.child(AppConfig.NODE_PASSENGER_RESPONSE);
    }

    public static DatabaseReference getFeedbackRequestsRef() {
        return rootRef.child(AppConfig.NODE_FEEDBACK_REQUESTS);
    }

    public static DatabaseReference getRootRef() {
        return rootRef;
    }
}
