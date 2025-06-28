package com.makitaxi.passenger;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import org.osmdroid.util.GeoPoint;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * LocationManager - Handles all location-related functionality
 * 
 * Responsibilities:
 * - GPS location tracking
 * - Location permissions
 * - Geocoding (address to coordinates)
 * - Reverse geocoding (coordinates to address)
 */
public class LocationManager implements LocationListener {
    
    private static final String TAG = "LocationManager";
    private static final long MIN_TIME_BETWEEN_UPDATES = 5000; // 5 seconds
    private static final float MIN_DISTANCE_CHANGE_FOR_UPDATES = 10.0f; // 10 meters
    
    private final Context context;
    private final ExecutorService executorService;
    private LocationUpdateListener locationUpdateListener;
    private final android.location.LocationManager androidLocationManager;
    private final Handler handler;
    
    /**
     * Interface for location update callbacks
     */
    public interface LocationUpdateListener {
        void onLocationUpdate(GeoPoint location);
        void onLocationError(String error);
        void onLocationPermissionDenied();
    }
    
    /**
     * Interface for reverse geocoding callbacks
     */
    public interface ReverseGeocodeListener {
        void onReverseGeocodeSuccess(String address);
        void onReverseGeocodeError(String error);
    }

    /**
     * Interface for geocoding callbacks
     */
    public interface GeocodeCallback {
        void onLocationFound(double latitude, double longitude, String address);
        void onLocationNotFound();
        void onError(String error);
    }
    
    public LocationManager(Context context) {
        this.context = context;
        this.executorService = Executors.newSingleThreadExecutor();
        this.handler = new Handler(Looper.getMainLooper());

        androidLocationManager = (android.location.LocationManager)
                context.getSystemService(Context.LOCATION_SERVICE);
    }
    
    public void setLocationUpdateListener(LocationUpdateListener listener) {
        this.locationUpdateListener = listener;
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * Start location updates
     */
    public void startLocationUpdates() {
        Log.d(TAG, "Starting location updates");
        
        if (!hasLocationPermission()) {
            Log.w(TAG, "Location permission not granted");
            if (locationUpdateListener != null) {
                locationUpdateListener.onLocationPermissionDenied();
            }
            return;
        }
        
        try {
            // Request location updates from GPS provider
            if (androidLocationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
                Log.d(TAG, "Requesting GPS location updates");
                androidLocationManager.requestLocationUpdates(
                        android.location.LocationManager.GPS_PROVIDER,
                        MIN_TIME_BETWEEN_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES,
                        this
                );
            }
            
            // Request location updates from Network provider
            if (androidLocationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) {
                Log.d(TAG, "Requesting Network location updates");
                androidLocationManager.requestLocationUpdates(
                        android.location.LocationManager.NETWORK_PROVIDER,
                        MIN_TIME_BETWEEN_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES,
                        this
                );
            }
            
            // Try to get last known location immediately
            getLastKnownLocation();
            
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception when requesting location updates", e);
            if (locationUpdateListener != null) {
                locationUpdateListener.onLocationError("Location permission denied");
            }
        }
    }
    
    /**
     * Stop location updates
     */
    public void stopLocationUpdates() {
        try {
            androidLocationManager.removeUpdates(this);
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception when removing location updates", e);
        }
    }

    /**
     * Convert coordinates to address using reverse geocoding
     */
    public void reverseGeocode(GeoPoint location, ReverseGeocodeListener listener) {
        if (location == null) {
            if (listener != null) {
                listener.onReverseGeocodeError("Location is null");
            }
            return;
        }
        
        Log.d(TAG, "Starting reverse geocoding for: " + location);
        
        if (Geocoder.isPresent()) {
            new Thread(() -> {
                try {
                    Geocoder geocoder = new Geocoder(context, java.util.Locale.getDefault());
                    List<Address> addresses = geocoder.getFromLocation(
                        location.getLatitude(), 
                        location.getLongitude(), 
                        1
                    );
                    
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        String addressText = formatAddress(address);
                        
                        // Return result on main thread
                        ((Activity) context).runOnUiThread(() -> {
                            if (listener != null) {
                                listener.onReverseGeocodeSuccess(addressText);
                            }
                        });
                        
                        Log.d(TAG, "Reverse geocoding successful: " + addressText);
                    } else {
                        ((Activity) context).runOnUiThread(() -> {
                            if (listener != null) {
                                listener.onReverseGeocodeError("No address found");
                            }
                        });
                        Log.w(TAG, "No address found for coordinates");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Reverse geocoding failed", e);
                    ((Activity) context).runOnUiThread(() -> {
                        if (listener != null) {
                            listener.onReverseGeocodeError("Geocoding failed: " + e.getMessage());
                        }
                    });
                }
            }).start();
        } else {
            Log.w(TAG, "Geocoder not available on this device");
            if (listener != null) {
                listener.onReverseGeocodeError("Geocoder not available");
            }
        }
    }

    private String formatAddress(Address address) {
        StringBuilder addressText = new StringBuilder();
        
        // Add street address
        if (address.getThoroughfare() != null) {
            addressText.append(address.getThoroughfare());
            if (address.getSubThoroughfare() != null) {
                addressText.append(" ").append(address.getSubThoroughfare());
            }
        }
        
        // Add locality (city)
        if (address.getLocality() != null) {
            if (addressText.length() > 0) {
                addressText.append(", ");
            }
            addressText.append(address.getLocality());
        }
        
        // Add country if not Serbia (to keep addresses short for local use)
        if (address.getCountryName() != null && 
            !address.getCountryName().equalsIgnoreCase("Serbia") &&
            !address.getCountryName().equalsIgnoreCase("RS")) {
            if (addressText.length() > 0) {
                addressText.append(", ");
            }
            addressText.append(address.getCountryName());
        }
        
        // Fallback to coordinates if no readable address
        if (addressText.length() == 0) {
            addressText.append(String.format("📍 %.4f, %.4f", 
                address.getLatitude(), address.getLongitude()));
        }
        
        return addressText.toString();
    }
    
    // LocationListener implementation
    @Override
    public void onLocationChanged(@NonNull Location location) {
        GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
        if (locationUpdateListener != null) {
            locationUpdateListener.onLocationUpdate(geoPoint);
        }
    }
    
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}
    
    @Override
    public void onProviderEnabled(@NonNull String provider) {
        Toast.makeText(context, provider + " enabled", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onProviderDisabled(@NonNull String provider) {
        Toast.makeText(context, provider + " disabled", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Get last known location
     */
    public void getLastKnownLocation() {
        Log.d(TAG, "Getting last known location");
        
        if (hasLocationPermission()) {
            try {
                Location lastKnownLocation = androidLocationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER);
                if (lastKnownLocation == null) {
                    lastKnownLocation = androidLocationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER);
                }
                
                if (lastKnownLocation != null) {
                    GeoPoint geoPoint = new GeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                    if (locationUpdateListener != null) {
                        locationUpdateListener.onLocationUpdate(geoPoint);
                    }
                }
            } catch (SecurityException e) {
                Log.e(TAG, "Security exception getting last known location", e);
                if (locationUpdateListener != null) {
                    locationUpdateListener.onLocationError("Security exception getting last known location: " + e.getMessage());
                }
            }
        }
    }
    
    // Activity lifecycle methods
    public void onResume() {
        Log.d(TAG, "onResume - restarting location updates if needed");
        if (hasLocationPermission()) {
            startLocationUpdates();
        }
    }
    
    public void onPause() {
        Log.d(TAG, "onPause - stopping location updates");
        stopLocationUpdates();
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        stopLocationUpdates();
        
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    /**
     * Geocode an address string to get coordinates
     */
    public void geocodeAddress(String address, GeocodeCallback callback) {
        if (address == null || address.trim().isEmpty()) {
            if (callback != null) {
                callback.onError("Address is empty");
            }
            return;
        }
        
        Log.d(TAG, "Starting geocoding for address: " + address);
        
        // Use Android's Geocoder for address geocoding
        if (Geocoder.isPresent()) {
            executorService.execute(() -> {
                try {
                    Geocoder geocoder = new Geocoder(context, java.util.Locale.getDefault());
                    List<Address> addresses = geocoder.getFromLocationName(address, 1);
                    
                    if (addresses != null && !addresses.isEmpty()) {
                        Address foundAddress = addresses.get(0);
                        double latitude = foundAddress.getLatitude();
                        double longitude = foundAddress.getLongitude();
                        String formattedAddress = formatAddress(foundAddress);
                        
                        // Return result on main thread
                        handler.post(() -> {
                            if (callback != null) {
                                callback.onLocationFound(latitude, longitude, formattedAddress);
                            }
                        });
                        
                        Log.d(TAG, "Geocoding successful for '" + address + "': " + latitude + ", " + longitude);
                    } else {
                        handler.post(() -> {
                            if (callback != null) {
                                callback.onLocationNotFound();
                            }
                        });
                        Log.w(TAG, "No location found for address: " + address);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Geocoding failed for address: " + address, e);
                    handler.post(() -> {
                        if (callback != null) {
                            callback.onError("Geocoding failed: " + e.getMessage());
                        }
                    });
                }
            });
        } else {
            Log.w(TAG, "Geocoder not available on this device");
            if (callback != null) {
                callback.onError("Geocoder not available");
            }
        }
    }
} 