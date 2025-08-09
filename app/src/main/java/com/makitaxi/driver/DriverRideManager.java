package com.makitaxi.driver;

import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.makitaxi.model.FeedbackRequest;
import com.makitaxi.model.RideRequest;
import com.makitaxi.model.User;
import com.makitaxi.utils.FirebaseHelper;
import com.makitaxi.utils.NotificationStatus;
import com.makitaxi.utils.PreferencesManager;
import com.makitaxi.config.AppConfig;

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
        String cachedUserName = PreferencesManager.getCachedUserName(activity);
        String driverName = (cachedUserName != null && !cachedUserName.isEmpty()) ? cachedUserName : "Driver";
        
        request.setDriverName(driverName);
        request.setDriverId(driverId);

        DatabaseReference rideRequestRef = FirebaseHelper.getRideRequestsRef().child(request.getRequestId());
        Map<String, Object> updates = new HashMap<>();
        updates.put("driverId", driverId);
        updates.put("driverName", driverName);
        
        rideRequestRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    handleRideDecision(request, NotificationStatus.ACCEPTED_BY_DRIVER, "Ride accepted", true);
                })
                .addOnFailureListener(e -> {
                    showToast("Failed to update ride request with driver info");
                    Log.e(TAG, "Error updating ride request: " + e.getMessage());
                });
    }

    public void declineRide(RideRequest request) {
        handleRideDecision(request, NotificationStatus.CANCELLED_BY_DRIVER, "Ride declined", false);
    }

    public void timeoutRide(RideRequest request) {
        handleRideDecision(request, NotificationStatus.TIMEOUT, "Ride timeout", false);
    }

    public void finishRide(RideRequest request) {
        DatabaseReference rideRequestRef = FirebaseHelper.getRideRequestsRef().child(request.getRequestId());
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", NotificationStatus.FINISHED);

        rideRequestRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    updateRideStatisticsOnCompletion(request);
                    
                    createFeedbackRequest(request);
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

    private void updateRideStatisticsOnCompletion(RideRequest request) {
        updateUserRideStatistics(request.getPassengerId(), request.getDistance(), request.getEstimatedPrice(), false); // Passenger
        updateUserRideStatistics(request.getDriverId(), request.getDistance(), request.getEstimatedPrice(), true); // Driver
    }

    private void updateUserRideStatistics(String userId, double distance, double price, boolean isDriver) {
        FirebaseHelper.getUserRequestsRef().child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            User user = dataSnapshot.getValue(User.class);
                            if (user != null) {
                                int actualDistance = (int) Math.round(distance);
                                
                                if (actualDistance <= 0) {
                                    actualDistance = 1;
                                }
                                
                                Log.d(TAG, "Updating ride completion stats for " + (isDriver ? "driver" : "passenger") + 
                                      " - Distance: " + distance + " -> " + actualDistance + " km, Price: " + price);
                                
                                user.incrementRideCount();
                                user.addDistance(actualDistance);
                                
                                if (isDriver) {
                                    user.addMoneyEarned(price);
                                } else {
                                    user.addMoneySpent(price);
                                }
                                
                                FirebaseHelper.getUserRequestsRef().child(userId).setValue(user)
                                        .addOnSuccessListener(aVoid -> 
                                            Log.d(TAG, "Ride completion statistics updated successfully for " + (isDriver ? "driver" : "passenger")))
                                        .addOnFailureListener(e -> 
                                            Log.e(TAG, "Failed to update ride completion statistics: " + e.getMessage()));
                            }
                        }
                    }
                    
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e(TAG, "Error loading user for ride completion statistics: " + databaseError.getMessage());
                    }
                });
    }

    private void createFeedbackRequest(RideRequest request) {
        String feedbackId = request.getRequestId() + "_feedback";
        
        FeedbackRequest feedbackRequest = new FeedbackRequest(
                feedbackId,
                request.getRequestId(),
                request.getPassengerId(),
                request.getDriverId(),
                request.getPassengerName(),
                request.getDriverName(),
                request.getPickupAddress(),
                request.getDropoffAddress(),
                request.getEstimatedPrice(),
                request.getCarType(),
                request.getDistance(),
                System.currentTimeMillis()
        );

        FirebaseHelper.getFeedbackRequestsRef().child(feedbackId).setValue(feedbackRequest)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Feedback request created successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating feedback request: " + e.getMessage());
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