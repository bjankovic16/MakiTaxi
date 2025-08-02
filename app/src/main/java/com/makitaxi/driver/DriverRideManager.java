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
        DatabaseReference rideRequestRef = FirebaseHelper.getRideRequestsRef().child(request.getRequestId());
        
        rideRequestRef.get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) {
                Toast.makeText(activity, "Ride request not found", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "Ride request not found: " + request.getRequestId());
                return;
            }

            RideRequest currentRequest = snapshot.getValue(RideRequest.class);
            if (currentRequest == null) {
                Toast.makeText(activity, "Failed to read ride request", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Failed to parse ride request from database");
                return;
            }

            if (currentRequest.getStatus() != NotificationStatus.CREATED) {
                Toast.makeText(activity, "Passenger cancelled the ride", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "Attempted to accept ride with status: " + currentRequest.getStatus());
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("status", NotificationStatus.ACCEPTED_BY_DRIVER);

            rideRequestRef.updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        if (request.getNotificationId() != null) {
                            DatabaseReference driverNotificationRef = FirebaseHelper.getDriverNotificationRef().child(request.getNotificationId());
                            driverNotificationRef.updateChildren(updates)
                                    .addOnSuccessListener(aVoid2 -> {
                                        Toast.makeText(activity, "Ride accepted", Toast.LENGTH_SHORT).show();
                                        uiManager.waitForPassengerConfirmation(currentRequest);
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(activity, "Failed to update driver notification", Toast.LENGTH_SHORT).show();
                                        Log.e(TAG, "Error updating driver notification: " + e.getMessage());
                                    });
                        } else {
                            Toast.makeText(activity, "Ride accepted", Toast.LENGTH_SHORT).show();
                            uiManager.waitForPassengerConfirmation(currentRequest);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(activity, "Failed to accept ride", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error accepting ride: " + e.getMessage());
                    });
        }).addOnFailureListener(e -> {
            Toast.makeText(activity, "Failed to fetch ride status", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error fetching ride status: " + e.getMessage());
        });
    }

    public void declineRide(RideRequest request) {
        DatabaseReference rideRequestRef = FirebaseHelper.getRideRequestsRef().child(request.getRequestId());
        
        rideRequestRef.get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) {
                Toast.makeText(activity, "Ride request not found", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "Ride request not found: " + request.getRequestId());
                return;
            }

            RideRequest currentRequest = snapshot.getValue(RideRequest.class);
            if (currentRequest == null) {
                Toast.makeText(activity, "Failed to read ride request", Toast.LENGTH_SHORT).show();
                return;
            }

            if (currentRequest.getStatus() != NotificationStatus.CREATED) {
                Toast.makeText(activity, "Passenger cancelled the ride", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("status", NotificationStatus.CANCELLED_BY_DRIVER);

            rideRequestRef.updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        if (request.getNotificationId() != null) {
                            DatabaseReference driverNotificationRef = FirebaseHelper.getDriverNotificationRef().child(request.getNotificationId());
                            driverNotificationRef.updateChildren(updates)
                                    .addOnSuccessListener(aVoid2 -> {
                                        Toast.makeText(activity, "Ride declined", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(activity, "Failed to update driver notification", Toast.LENGTH_SHORT).show();
                                        Log.e(TAG, "Error updating driver notification: " + e.getMessage());
                                    });
                        } else {
                            Toast.makeText(activity, "Ride declined", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(activity, "Failed to decline ride", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error declining ride: " + e.getMessage());
                    });
        }).addOnFailureListener(e -> {
            Toast.makeText(activity, "Failed to fetch ride status", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error fetching ride status: " + e.getMessage());
        });
    }
} 