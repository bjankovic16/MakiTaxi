package com.makitaxi.driver;

import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.makitaxi.model.RideRequest;
import com.makitaxi.utils.FirebaseHelper;
import com.makitaxi.utils.NotificationStatus;

import java.util.HashMap;
import java.util.Map;

public class DriverRideManager {
    private static final String TAG = "DriverRideManager";

    private final AppCompatActivity activity;
    private final String driverId;
    private final DriverUIManager uiManager;

    public DriverRideManager(AppCompatActivity activity, String driverId, DriverUIManager uiManager) {
        this.activity = activity;
        this.driverId = driverId;
        this.uiManager = uiManager;
    }

    public void acceptRide(RideRequest request) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", NotificationStatus.ACCEPTED_BY_DRIVER);

        DatabaseReference requestRef = FirebaseHelper.getDriverNotificationRef().child(request.getRequestId());
        requestRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(activity, "Ride accepted", Toast.LENGTH_SHORT).show();
                    uiManager.waitForPassengerConfirmation(request);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(activity, "Failed to accept ride", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error accepting ride: " + e.getMessage());
                });
    }

    public void declineRide(RideRequest request) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", NotificationStatus.CANCELLED_BY_DRIVER);

        DatabaseReference requestRef = FirebaseHelper.getDriverNotificationRef().child(request.getRequestId());
        requestRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(activity, "Ride declined", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(activity, "Failed to decline ride", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error declining ride: " + e.getMessage());
                });
    }
} 