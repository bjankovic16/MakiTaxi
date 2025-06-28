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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.osmdroid.util.GeoPoint;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
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
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final long MIN_TIME_BETWEEN_UPDATES = 5000; // 5 seconds
    private static final float MIN_DISTANCE_CHANGE_FOR_UPDATES = 10.0f; // 10 meters
    
    private Context context;
    private android.location.LocationManager systemLocationManager;
    private Geocoder geocoder;
    private ExecutorService executorService;
    private LocationUpdateListener locationUpdateListener;
    private android.location.LocationManager androidLocationManager;
    private Handler handler;
    
    /**
     * Interface for location update callbacks
     */
    public interface LocationUpdateListener {
        void onLocationUpdate(GeoPoint location);
        void onLocationError(String error);
        void onLocationPermissionDenied();
    }
    
    public LocationManager(Context context) {
        this.context = context;
        this.executorService = Executors.newSingleThreadExecutor();
        this.handler = new Handler(Looper.getMainLooper());
        
        // Initialize Android LocationManager
        androidLocationManager = (android.location.LocationManager) 
                context.getSystemService(Context.LOCATION_SERVICE);
        
        this.geocoder = new Geocoder(context, Locale.getDefault());
        
        Log.d(TAG, "LocationManager initialized");
    }
    
    public void setLocationUpdateListener(LocationUpdateListener listener) {
        this.locationUpdateListener = listener;
    }
    
    /**
     * Check if location permissions are granted
     */
    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * Request location permissions
     */
    public void requestLocationPermissions() {
        Log.d(TAG, "Requesting location permissions");
        
        ActivityCompat.requestPermissions((Activity) context,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }
    
    /**
     * Handle permission request results
     */
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                         @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
                if (locationUpdateListener != null) {
                    // Don't call onLocationPermissionDenied when permission is granted
                    Log.d(TAG, "Location permission granted");
                }
            } else {
                if (locationUpdateListener != null) {
                    locationUpdateListener.onLocationPermissionDenied();
                }
            }
        }
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
        Log.d(TAG, "Stopping location updates");
        
        try {
            androidLocationManager.removeUpdates(this);
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception when removing location updates", e);
        }
    }
    
    /**
     * Get current location
     */
    public GeoPoint getCurrentLocation() {
        if (hasLocationPermission()) {
            try {
                Location lastKnownLocation = androidLocationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER);
                if (lastKnownLocation == null) {
                    lastKnownLocation = androidLocationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER);
                }
                
                if (lastKnownLocation != null) {
                    return new GeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                }
            } catch (SecurityException e) {
                Log.e(TAG, "Security exception getting current location", e);
            }
        }
        return null;
    }
    
    /**
     * Geocode address to coordinates
     */
    public void geocodeAddress(String address, GeocodeCallback callback) {
        executorService.execute(() -> {
            try {
                Log.d(TAG, "Geocoding address: " + address);
                
                // Check if Geocoder is available
                if (!Geocoder.isPresent()) {
                    Log.e(TAG, "Geocoder not available on this device");
                    callback.onGeocodeResult(null, "Geocoder not available");
                    return;
                }
                
                // Use Android's Geocoder to convert address to coordinates
                List<Address> addresses = geocoder.getFromLocationName(address, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address location = addresses.get(0);
                    GeoPoint point = new GeoPoint(location.getLatitude(), location.getLongitude());
                    Log.d(TAG, "Geocoded to: " + point.getLatitude() + ", " + point.getLongitude());
                    callback.onGeocodeResult(point, null);
                } else {
                    Log.w(TAG, "No results found for address: " + address);
                    callback.onGeocodeResult(null, "No results found for: " + address);
                }
            } catch (IOException e) {
                Log.e(TAG, "Geocoding failed for: " + address, e);
                callback.onGeocodeResult(null, "Geocoding failed: " + e.getMessage());
            }
        });
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
        
        // Use Android's Geocoder for reverse geocoding
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
    
    /**
     * Format address from Geocoder result
     */
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
    
    /**
     * Interface for reverse geocoding callbacks
     */
    public interface ReverseGeocodeListener {
        void onReverseGeocodeSuccess(String address);
        void onReverseGeocodeError(String error);
    }
    
    // LocationListener implementation
    @Override
    public void onLocationChanged(@NonNull Location location) {
        Log.d(TAG, "Location updated: " + location.getLatitude() + ", " + location.getLongitude());
        
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
     * Callback interface for geocoding results
     */
    public interface GeocodeCallback {
        void onGeocodeResult(GeoPoint result, String error);
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
        Log.d(TAG, "Cleaning up LocationManager");
        
        stopLocationUpdates();
        
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
} 