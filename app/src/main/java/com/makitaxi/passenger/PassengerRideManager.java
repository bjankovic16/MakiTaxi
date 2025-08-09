package com.makitaxi.passenger;

import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.makitaxi.model.RideRequest;
import com.makitaxi.model.User;
import com.makitaxi.utils.DriverPollingService;
import com.makitaxi.utils.FirebaseHelper;
import com.makitaxi.utils.NotificationStatus;

import org.osmdroid.util.GeoPoint;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class PassengerRideManager {
    private final AppCompatActivity activity;
    private final PassengerUIManager uiManager;

    public PassengerRideManager(AppCompatActivity activity, PassengerUIManager uiManager) {
        this.activity = activity;
        this.uiManager = uiManager;
    }

    public void createRideRequest(String carType, GeoPoint pickupGeoPoint, GeoPoint destinationGeoPoint, 
                                 String pickupAddress, String destinationAddress, double distance, double duration) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String passengerId = Objects.requireNonNull(auth.getCurrentUser()).getUid();

        RideRequest request = new RideRequest(
                passengerId,
                pickupGeoPoint.getLatitude(),
                pickupGeoPoint.getLongitude(),
                destinationGeoPoint.getLatitude(),
                destinationGeoPoint.getLongitude(),
                pickupAddress,
                destinationAddress,
                carType,
                distance,
                duration
        );

        loadPassengerName(passengerId, passengerName -> {
            request.setPassengerName(passengerName);
            
            DatabaseReference requestRef = FirebaseHelper.getRideRequestsRef().push();
            String requestId = requestRef.getKey();
            request.setRequestId(requestId);
            
            createRideRequestWithCallback(request, requestRef);
        });
    }

    private void loadPassengerName(String passengerId, OnPassengerNameLoadedListener listener) {
        DatabaseReference userReference = FirebaseHelper.getUserRequestsRef();
        
        userReference.child(passengerId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        String passengerName = "Passenger";
                        if (dataSnapshot.exists()) {
                            User passenger = dataSnapshot.getValue(User.class);
                            if (passenger != null) {
                                passengerName = passenger.getFullName();
                            }
                        }
                        listener.onPassengerNameLoaded(passengerName);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        listener.onPassengerNameLoaded("Passenger");
                    }
                });
    }

    private interface OnPassengerNameLoadedListener {
        void onPassengerNameLoaded(String passengerName);
    }

    private void createRideRequestWithCallback(RideRequest request, DatabaseReference requestRef) {

        DriverPollingService.MatchingCallback callback = new DriverPollingService.MatchingCallback() {
            @Override
            public void onNoDriversAvailable() {
                Map<String, Object> updates = new HashMap<>();
                updates.put("status", NotificationStatus.NO_AVAILABLE_DRIVERS);

                requestRef.updateChildren(updates).addOnSuccessListener(aVoid -> {
                    Toast.makeText(activity, "❌ No drivers available", Toast.LENGTH_SHORT).show();
                    uiManager.dismissSearchingDialog();
                });
            }

            @Override
            public void onAllDriversDeclined() {
                Map<String, Object> updates = new HashMap<>();
                updates.put("status", NotificationStatus.ALL_DRIVERS_DECLINED);

                requestRef.updateChildren(updates).addOnSuccessListener(aVoid -> {
                    Toast.makeText(activity, "❌ There is no driver who accepted this ride", Toast.LENGTH_SHORT).show();
                    uiManager.dismissSearchingDialog();
                });
            }

            @Override
            public void onDriverAccepted(String driverId, RideRequest rideRequest) {
                Map<String, Object> updates = new HashMap<>();
                updates.put("status", NotificationStatus.ACCEPTED_BY_DRIVER);

                requestRef.updateChildren(updates).addOnSuccessListener(aVoid -> {
                    uiManager.dismissSearchingDialog();
                    uiManager.showDriverDetailsBottomSheet(driverId, rideRequest);
                });
            }

            @Override
            public void onError(String error) {
                uiManager.dismissSearchingDialog();
            }
        };

        String requestId = requestRef.getKey();
        if (requestId != null) {
            requestRef.setValue(request)
                    .addOnSuccessListener(aVoid -> {
                        DriverPollingService.notifyNearDrivers(request, callback);
                        uiManager.showSearchingForDriverDialog(request);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(activity, "❌ Failed to create ride request", Toast.LENGTH_SHORT).show();
                    });
        }
    }
} 