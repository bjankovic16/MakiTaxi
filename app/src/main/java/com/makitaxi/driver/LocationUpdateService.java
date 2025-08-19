package com.makitaxi.driver;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.firebase.database.DatabaseReference;
import com.makitaxi.utils.FirebaseHelper;

import org.osmdroid.util.GeoPoint;

public class LocationUpdateService {
    private static final String TAG = "LocationUpdateService";
    private static final long UPDATE_INTERVAL = 1000;

    private final Handler handler;
    private final GeoFire geoFire;
    private final String driverId;
    private final MapDriver mapDriver;
    private boolean isUpdating = false;
    private Runnable updateRunnable;

    public LocationUpdateService(String driverId, MapDriver mapDriver) {
        this.driverId = driverId;
        this.mapDriver = mapDriver;
        this.handler = new Handler(Looper.getMainLooper());
        
        DatabaseReference ref = FirebaseHelper.getDriverLocationRef();
        this.geoFire = new GeoFire(ref);
        
        setupUpdateRunnable();
    }

    private void setupUpdateRunnable() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isUpdating) {
                    return;
                }

                GeoPoint currentLocation = mapDriver.getCurrentLocation();
                if (currentLocation != null) {
                    updateDriverLocation(currentLocation);
                }

                handler.postDelayed(this, UPDATE_INTERVAL);
            }
        };
    }

    private void updateDriverLocation(GeoPoint location) {
        geoFire.setLocation(driverId,
                new GeoLocation(location.getLatitude(), location.getLongitude()),
                (key, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error updating location: " + error.getMessage());
                    }
                });
        
        mapDriver.updateDriverLocation(location);
    }

    public void startUpdates() {
        isUpdating = true;
        handler.post(updateRunnable);
    }

    public void stopUpdates() {
        isUpdating = false;
        handler.removeCallbacks(updateRunnable);
        geoFire.removeLocation(driverId, (key, error) -> {
            if (error != null) {
                Log.e(TAG, "Error removing location: " + error.getMessage());
            }
        });
    }
} 