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
        handleRideDecision(request, NotificationStatus.ACCEPTED_BY_DRIVER, "Ride accepted", true);
    }

    public void declineRide(RideRequest request) {
        handleRideDecision(request, NotificationStatus.CANCELLED_BY_DRIVER, "Ride declined", false);
    }

    public void finisRide(RideRequest request) {
        DatabaseReference rideRequestRef = FirebaseHelper.getRideRequestsRef().child(request.getRequestId());
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", NotificationStatus.FINISHED);

        rideRequestRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    showToast("Ride finished");
                    uiManager.hideRideDetailsPanel();
                    uiManager.clearRoute();
                    uiManager.listenForRideRequests();
                })
                .addOnFailureListener(e -> {
                    showToast("Failed to update ride request");
                    Log.e(TAG, "Error updating ride request: " + e.getMessage());
                });

    }

    private void handleRideDecision(RideRequest request, NotificationStatus newStatus, String successMessage, boolean waitForPassenger) {
        DatabaseReference rideRequestRef = FirebaseHelper.getRideRequestsRef().child(request.getRequestId());

        rideRequestRef.get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) {
                showToast("Ride request not found");
                Log.w(TAG, "Ride request not found: " + request.getRequestId());
                updateDriverNotificationWithCancelledByPassenger(request);
                return;
            }

            RideRequest currentRequest = snapshot.getValue(RideRequest.class);
            if (currentRequest == null) {
                showToast("Failed to read ride request");
                Log.e(TAG, "Failed to parse ride request from database");
                updateDriverNotificationWithCancelledByPassenger(request);
                return;
            }

            if (currentRequest.getStatus() != NotificationStatus.CREATED) {
                showToast("Passenger cancelled the ride");
                Log.w(TAG, "Attempted to update ride with status: " + currentRequest.getStatus());
                updateDriverNotificationWithCancelledByPassenger(request);
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("status", newStatus);

            rideRequestRef.updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        updateDriverNotification(request, updates, successMessage);
                        if (waitForPassenger) {
                            uiManager.waitForPassengerConfirmation(currentRequest);
                        }
                    })
                    .addOnFailureListener(e -> {
                        showToast("Failed to update ride request");
                        Log.e(TAG, "Error updating ride request: " + e.getMessage());
                    });

        }).addOnFailureListener(e -> {
            showToast("Failed to fetch ride status");
            Log.e(TAG, "Error fetching ride status: " + e.getMessage());
        });
    }

    private void updateDriverNotification(RideRequest request, Map<String, Object> updates, String successMessage) {
        if (request.getNotificationId() != null) {
            DatabaseReference driverNotificationRef = FirebaseHelper.getDriverNotificationRef().child(request.getNotificationId());
            driverNotificationRef.updateChildren(updates)
                    .addOnSuccessListener(aVoid -> showToast(successMessage))
                    .addOnFailureListener(e -> {
                        showToast("Failed to update driver notification");
                        Log.e(TAG, "Error updating driver notification: " + e.getMessage());
                    });
        } else {
            showToast(successMessage);
        }
    }

    private void updateDriverNotificationWithCancelledByPassenger(RideRequest request) {
        if (request.getNotificationId() != null) {
            DatabaseReference driverNotificationRef = FirebaseHelper.getDriverNotificationRef().child(request.getNotificationId());
            
            // Update the top-level status
            Map<String, Object> statusUpdates = new HashMap<>();
            statusUpdates.put("status", NotificationStatus.CANCELLED_BY_PASSENGER);
            
            driverNotificationRef.updateChildren(statusUpdates)
                    .addOnSuccessListener(aVoid -> {
                        DatabaseReference rideRequestStatusRef = driverNotificationRef.child("rideRequest").child("status");
                        rideRequestStatusRef.setValue(NotificationStatus.CANCELLED_BY_PASSENGER)
                                .addOnSuccessListener(aVoid2 -> {
                                    Log.d(TAG, "Driver notification and rideRequest status updated with CANCELLED_BY_PASSENGER");
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error updating rideRequest status: " + e.getMessage());
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error updating driver notification status: " + e.getMessage());
                    });
        }
    }

    private void showToast(String message) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
    }

} 