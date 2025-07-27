package com.makitaxi.utils;

import android.util.Log;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.makitaxi.model.DriverNotification;
import com.makitaxi.model.RideRequest;
import com.makitaxi.passenger.PassengerScreen;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.List;

public class DriverPollingService {

    private static final String TAG = "DriverPollingService";
    private static final int MAX_DRIVERS = 10;
    private static final double INITIAL_RADIUS_KM = 2;
    private static final double MAX_RADIUS_KM = 10;
    private static final double RADIUS_INCREMENT_KM = 2;
    private static final GeoFire geoFire = new GeoFire(FirebaseHelper.getDriverLocationRef());
    private static ValueEventListener driverResponseListener;
    private static int currentDriverIndex = 0;
    private static final List<String> nearbyDrivers = new ArrayList<>();
    private static MatchingCallback callback;
    private static GeoQuery geoQuery;
    private static double currentRadius = INITIAL_RADIUS_KM;
    private static int foundDrivers = 0;

    public interface MatchingCallback {
        void onNoDriversAvailable();

        void onAllDriversDeclined();

        void onDriverAccepted(String driverId, RideRequest rideRequest);

        void onError(String error);
    }

    public static void notifyNearDrivers(RideRequest request, MatchingCallback cBack) {
        reinitData();
        GeoPoint pickupGeoPoint = new GeoPoint(request.getPickupLatitude(), request.getPickupLongitude());
        startGeoQuery(pickupGeoPoint, request);
        callback = cBack;
    }

    private static void reinitData() {
        currentDriverIndex = 0;
        nearbyDrivers.clear();
        foundDrivers = 0;
        currentRadius = INITIAL_RADIUS_KM;
    }

    public static void continueWithDriverNotification(RideRequest rideRequest) {
        currentDriverIndex++;
        startDriverNotification(rideRequest);
    }

    public static void stopNotifyingDrivers() {
        if (geoQuery != null) {
            geoQuery.removeAllListeners();
        }
    }

    private static void startGeoQuery(GeoPoint location, RideRequest request) {
        if (geoQuery != null) {
            geoQuery.removeAllListeners();
        }

        geoQuery = geoFire.queryAtLocation(new GeoLocation(location.getLatitude(), location.getLongitude()), currentRadius);

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!nearbyDrivers.contains(key)) {
                    nearbyDrivers.add(key);
                    foundDrivers++;

                    if (foundDrivers >= MAX_DRIVERS) {
                        geoQuery.removeAllListeners();
                        startDriverNotification(request);
                    }
                }
            }

            @Override
            public void onKeyExited(String key) {
                nearbyDrivers.remove(key);
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
            }

            @Override
            public void onGeoQueryReady() {
                if (foundDrivers == 0 && currentRadius < MAX_RADIUS_KM) {
                    currentRadius += RADIUS_INCREMENT_KM;
                    startGeoQuery(location, request);
                } else if (foundDrivers == 0) {
                    callback.onNoDriversAvailable();
                } else {
                    startDriverNotification(request);
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                callback.onError("Error finding nearby drivers: " + error.getMessage());
            }
        });
    }

    private static void startDriverNotification(RideRequest request) {
        if (currentDriverIndex >= nearbyDrivers.size()) {
            if (driverResponseListener != null) {
                DatabaseReference ref = FirebaseHelper.getDriverNotificationRef().child(request.getRequestId());
                ref.removeEventListener(driverResponseListener);
            }
            callback.onAllDriversDeclined();
            return;
        }

        String driverId = nearbyDrivers.get(currentDriverIndex);

        DriverNotification driverNotification = new DriverNotification(driverId, System.currentTimeMillis(), request);

        DatabaseReference requestRef = FirebaseHelper.getDriverNotificationRef().push();
        String requestId = requestRef.getKey();
        request.setRequestId(requestId);

        if (requestId != null) {
            requestRef.setValue(driverNotification).addOnSuccessListener(aVoid -> {
                waitForRiderResponse(requestId, driverId);
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Failed to notify driver");
            });
        }
    }

    private static void waitForRiderResponse(String requestId, String driverId) {
        driverResponseListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                DriverNotification request = snapshot.getValue(DriverNotification.class);
                if (request != null) {
                    if (NotificationStatus.CANCELLED_BY_DRIVER.equals(request.getStatus())) {
                        currentDriverIndex++;
                        startDriverNotification(request.getRideRequest());
                    } else if (NotificationStatus.ACCEPTED_BY_DRIVER.equals(request.getStatus())) {
                        callback.onDriverAccepted(driverId, request.getRideRequest());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        DatabaseReference ref = FirebaseHelper.getDriverNotificationRef().child(requestId);
        ref.addValueEventListener(driverResponseListener);
    }
} 