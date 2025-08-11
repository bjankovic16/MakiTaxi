package com.makitaxi.passenger;

import com.makitaxi.utils.ToastUtils;

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
import com.makitaxi.utils.PreferencesManager;

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

        String cachedUserName = PreferencesManager.getCachedUserName(activity);
        String passengerName = (cachedUserName != null && !cachedUserName.isEmpty()) ? cachedUserName : "Passenger";
        request.setPassengerName(passengerName);
        
        DatabaseReference requestRef = FirebaseHelper.getRideRequestsRef().push();
        String requestId = requestRef.getKey();
        request.setRequestId(requestId);
        uiManager.setCurrentRideRequestId(requestId);
        
        createRideRequestWithCallback(request, requestRef);
    }

    private void createRideRequestWithCallback(RideRequest request, DatabaseReference requestRef) {

        DriverPollingService.MatchingCallback callback = new DriverPollingService.MatchingCallback() {
            @Override
            public void onNoDriversAvailable() {
                Map<String, Object> updates = new HashMap<>();
                updates.put("status", NotificationStatus.NO_AVAILABLE_DRIVERS);

                requestRef.updateChildren(updates).addOnSuccessListener(aVoid -> {
                    ToastUtils.showError(activity, "No drivers available");
                    uiManager.dismissSearchingDialog();
                });
            }

            @Override
            public void onAllDriversDeclined() {
                Map<String, Object> updates = new HashMap<>();
                updates.put("status", NotificationStatus.ALL_DRIVERS_DECLINED);

                requestRef.updateChildren(updates).addOnSuccessListener(aVoid -> {
                    ToastUtils.showError(activity, "No driver has accepted this ride yet");
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
                        ToastUtils.showError(activity, "Failed to create ride request");
                    });
        }
    }
} 