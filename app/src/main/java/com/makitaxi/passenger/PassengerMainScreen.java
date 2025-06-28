package com.makitaxi.passenger;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.makitaxi.R;
import com.makitaxi.utils.PreferencesManager;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.util.List;

/**
 * PassengerMainScreen - Main screen for passenger functionality
 * 
 * Responsibilities:
 * - Coordinate between different controllers
 * - Handle activity lifecycle
 * - Manage permissions
 * - Handle UI interactions
 */
public class PassengerMainScreen extends AppCompatActivity implements 
        LocationManager.LocationUpdateListener,
        MapController.MapInteractionListener,
        AutocompleteController.AutocompleteListener {

    private static final String TAG = "PassengerMainScreen";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    // Controllers
    private LocationManager locationManager;
    private MapController mapController;
    private AutocompleteController autocompleteController;

    // UI Components
    private MapView mapView;
    private EditText editTextStartLocation;
    private AutoCompleteTextView editTextEndLocation;
    private Button buttonSelectOnMap;
    private Button buttonShowRoute;
    private Button btnLogout;
    private Button btnUseCurrentLocation;
    private Button btnSelectStartOnMap;
    private Button btnClearRoute;

    // State
    private GeoPoint currentLocation;
    private GeoPoint startLocation; // Separate start location (can be different from current)
    private GeoPoint destinationLocation;
    private String startAddress;
    private String destinationAddress;
    
    // Selection mode for map interactions
    private enum SelectionMode {
        NONE,
        START,        // Selecting start location
        DESTINATION   // Selecting destination location
    }
    private SelectionMode selectionMode = SelectionMode.NONE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.d(TAG, "onCreate: Starting PassengerMainScreen");
        
        // Initialize OSMDroid configuration
        initializeOSMDroid();
        
        // Set content view
        setContentView(R.layout.passanger_main_screen);
        
        // Initialize UI components
        initializeViews();
        
        // Initialize controllers
        initializeControllers();
        
        // Setup UI interactions
        setupUIInteractions();
        
        // Check and request permissions
        checkLocationPermissions();
        
        Log.d(TAG, "onCreate: PassengerMainScreen initialization completed");
    }

    /**
     * Initialize OSMDroid configuration
     */
    private void initializeOSMDroid() {
        Log.d(TAG, "Initializing OSMDroid configuration");
        Configuration.getInstance().load(this, PreferencesManager.getSharedPreferences(this));
    }

    /**
     * Initialize UI components
     */
    private void initializeViews() {
        Log.d(TAG, "Initializing UI views");
        
        mapView = findViewById(R.id.mapView);
        editTextStartLocation = findViewById(R.id.editTextStartLocation);
        editTextEndLocation = findViewById(R.id.editTextEndLocation);
        buttonSelectOnMap = findViewById(R.id.btnSelectOnMap);
        buttonShowRoute = findViewById(R.id.btnShowRoute);
        btnLogout = findViewById(R.id.btnLogout);
        btnUseCurrentLocation = findViewById(R.id.btnUseCurrentLocation);
        btnSelectStartOnMap = findViewById(R.id.btnSelectStartOnMap);
        btnClearRoute = findViewById(R.id.btnClearRoute);
        
        if (mapView == null) {
            Log.e(TAG, "MapView not found in layout!");
            return;
        }
        
        if (editTextStartLocation == null) {
            Log.e(TAG, "EditTextStartLocation not found in layout!");
            return;
        }
        
        if (editTextEndLocation == null) {
            Log.e(TAG, "EditTextEndLocation not found in layout!");
            return;
        }
        
        Log.d(TAG, "UI views initialized successfully");
    }

    /**
     * Initialize all controllers
     */
    private void initializeControllers() {
        Log.d(TAG, "Initializing controllers");
        
        // Initialize LocationManager
        locationManager = new LocationManager(this);
        locationManager.setLocationUpdateListener(this);
        
        // Initialize MapController
        mapController = new MapController(this, mapView);
        mapController.setMapInteractionListener(this);
        
        // Initialize AutocompleteController
        autocompleteController = new AutocompleteController(this, editTextEndLocation);
        autocompleteController.setAutocompleteListener(this);
        
        Log.d(TAG, "Controllers initialized successfully");
    }

    /**
     * Setup UI interactions
     */
    private void setupUIInteractions() {
        Log.d(TAG, "Setting up UI interactions");
        
        // Setup button click listeners
        setupButtonListeners();
        
        Log.d(TAG, "UI interactions setup completed");
    }

    /**
     * Setup button click listeners
     */
    private void setupButtonListeners() {
        Log.d(TAG, "Setting up button listeners");
        
        // Use current location button
        if (btnUseCurrentLocation != null) {
            btnUseCurrentLocation.setOnClickListener(v -> handleUseCurrentLocation());
        }
        
        // Select start location on map button
        if (btnSelectStartOnMap != null) {
            btnSelectStartOnMap.setOnClickListener(v -> {
                Log.d(TAG, "Select start on map button clicked");
                selectionMode = SelectionMode.START;
                Toast.makeText(this, "📍 Tap on the map to select start location", Toast.LENGTH_SHORT).show();
            });
        }
        
        // Select destination on map button
        if (buttonSelectOnMap != null) {
            buttonSelectOnMap.setOnClickListener(v -> {
                Log.d(TAG, "Select destination on map button clicked");
                selectionMode = SelectionMode.DESTINATION;
                Toast.makeText(this, "🎯 Tap on the map to select destination", Toast.LENGTH_SHORT).show();
            });
        }
        
        // Show route button
        if (buttonShowRoute != null) {
            buttonShowRoute.setOnClickListener(v -> handleShowRoute());
        }
        
        // Clear route button
        if (btnClearRoute != null) {
            btnClearRoute.setOnClickListener(v -> handleClearRoute());
        }
        
        // Logout button
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> handleLogout());
        }
        
        Log.d(TAG, "Button listeners setup completed");
    }

    /**
     * Check and request location permissions
     */
    private void checkLocationPermissions() {
        Log.d(TAG, "Checking location permissions");
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            
            Log.d(TAG, "Location permission not granted, requesting...");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            Log.d(TAG, "Location permission already granted");
            startLocationServices();
        }
    }

    /**
     * Start location services
     */
    private void startLocationServices() {
        Log.d(TAG, "Starting location services");
        locationManager.startLocationUpdates();
    }

    /**
     * Handle show route button click
     */
    private void handleShowRoute() {
        // Use start location if set, otherwise use current location
        GeoPoint routeStartLocation = (startLocation != null) ? startLocation : currentLocation;
        
        if (routeStartLocation == null) {
            Toast.makeText(this, "❌ Start location not available. Use current location or select on map.", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Cannot show route: start location is null");
            return;
        }
        
        if (destinationLocation == null) {
            Toast.makeText(this, "❌ Please select a destination first", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Cannot show route: destination location is null");
            return;
        }
        
        Log.d(TAG, "Showing route from " + routeStartLocation + " to " + destinationLocation);
        
        // Clear previous route and markers
        mapController.clearAll();
        
        // Add markers immediately
        mapController.addStartMarker(routeStartLocation);
        mapController.addDestinationMarker(destinationLocation);
        
        // Show loading toast
        Toast.makeText(this, "🔄 Calculating route...", Toast.LENGTH_SHORT).show();
        
        // Draw route and get real distance/duration from OSRM
        mapController.drawRouteBetweenPoints(routeStartLocation, destinationLocation, new MapController.RoutingCallback() {
            @Override
            public void onRouteFound(List<GeoPoint> routePoints, double distanceKm, double durationMinutes) {
                // Show route information with real distance and duration
                String startText = (startLocation != null && startAddress != null) ? startAddress : 
                                  (routeStartLocation.equals(currentLocation)) ? "Current Location" : "Selected Start";
                String destText = (destinationAddress != null && !destinationAddress.isEmpty()) ? destinationAddress : "Selected Destination";
                
                String distanceText = String.format("%.1f km", distanceKm);
                String durationText = String.format("%.0f min", durationMinutes);
                
                Toast.makeText(PassengerMainScreen.this, 
                        "🛣️ Route calculated!\n" +
                        "📍 From: " + startText + "\n" +
                        "🎯 To: " + destText + "\n" +
                        "📏 Distance: " + distanceText + "\n" +
                        "⏱️ Duration: " + durationText, 
                        Toast.LENGTH_LONG).show();
                
                Log.d(TAG, "Route calculation completed - Distance: " + distanceText + ", Duration: " + durationText);
            }
            
            @Override
            public void onRoutingError(String error) {
                Log.e(TAG, "Route calculation failed: " + error);
                
                // Fallback to straight-line distance if routing fails
                double straightLineDistance = calculateDistance(routeStartLocation, destinationLocation);
                String distanceText = String.format("%.1f km", straightLineDistance);
                
                String startText = (startLocation != null && startAddress != null) ? startAddress : 
                                  (routeStartLocation.equals(currentLocation)) ? "Current Location" : "Selected Start";
                String destText = (destinationAddress != null && !destinationAddress.isEmpty()) ? destinationAddress : "Selected Destination";
                
                Toast.makeText(PassengerMainScreen.this, 
                        "⚠️ Route service unavailable\n" +
                        "📍 From: " + startText + "\n" +
                        "🎯 To: " + destText + "\n" +
                        "📏 Straight-line distance: " + distanceText, 
                        Toast.LENGTH_LONG).show();
                
                Log.w(TAG, "Using fallback straight-line distance: " + distanceText);
            }
        });
        
        // Reset selection mode
        selectionMode = SelectionMode.NONE;
    }

    /**
     * Handle logout button click
     */
    private void handleLogout() {
        Log.d(TAG, "Handling logout");
        
        // Clear all data
        currentLocation = null;
        startLocation = null;
        destinationLocation = null;
        startAddress = null;
        destinationAddress = null;
        
        // Clear input fields
        if (editTextStartLocation != null) {
            editTextStartLocation.setText("");
        }
        
        // Clear map
        if (mapController != null) {
            mapController.clearAll();
        }
        
        // Clear autocomplete
        if (autocompleteController != null) {
            autocompleteController.clearInput();
        }
        
        // Stop location services
        if (locationManager != null) {
            locationManager.stopLocationUpdates();
        }
        
        // Clear user session
        PreferencesManager.clearUserSession(this);
        
        // Sign out from Firebase
        com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
        
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        
        // Return to login screen
        Intent intent = new Intent(this, com.makitaxi.login.Login.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    /**
     * Handle use current location button click
     */
    private void handleUseCurrentLocation() {
        Log.d(TAG, "Handling use current location");
        
        if (currentLocation == null) {
            Toast.makeText(this, "Current location not available. Please wait...", Toast.LENGTH_SHORT).show();
            
            // Try to get location again
            if (locationManager != null) {
                locationManager.getLastKnownLocation();
            }
            return;
        }
        
        // Set current location as start location
        startLocation = currentLocation;
        startAddress = "Current Location";
        
        // Update start location input field
        if (editTextStartLocation != null) {
            editTextStartLocation.setText(startAddress);
        }
        
        // Use current location as start point
        mapController.addStartMarker(currentLocation);
        mapController.centerMapOn(currentLocation);
        
        Toast.makeText(this, "📍 Current location set as start point", Toast.LENGTH_SHORT).show();
        
        // Try to get actual address for current location
        locationManager.reverseGeocode(currentLocation, new LocationManager.ReverseGeocodeListener() {
            @Override
            public void onReverseGeocodeSuccess(String address) {
                Log.d(TAG, "Current location reverse geocoding successful: " + address);
                startAddress = address;
                
                // Update start location input field with real address
                if (editTextStartLocation != null) {
                    editTextStartLocation.setText(startAddress);
                }
            }
            
            @Override
            public void onReverseGeocodeError(String error) {
                Log.w(TAG, "Current location reverse geocoding failed: " + error);
                // Keep "Current Location" as the address
            }
        });
    }
    
    /**
     * Handle clear route button click
     */
    private void handleClearRoute() {
        Log.d(TAG, "Handling clear route");
        
        // Clear all location data
        startLocation = null;
        destinationLocation = null;
        startAddress = null;
        destinationAddress = null;
        
        // Reset selection mode
        selectionMode = SelectionMode.NONE;
        
        // Clear input fields
        if (editTextStartLocation != null) {
            editTextStartLocation.setText("");
        }
        
        // Clear autocomplete text
        if (autocompleteController != null) {
            autocompleteController.clearInput();
        }
        
        // Clear map overlays
        if (mapController != null) {
            mapController.clearAll();
        }
        
        Toast.makeText(this, "🧹 All locations cleared", Toast.LENGTH_SHORT).show();
    }

    // LocationManager.LocationUpdateListener implementation
    @Override
    public void onLocationUpdate(GeoPoint location) {
        Log.d(TAG, "Location update received: " + location);
        
        currentLocation = location;
        
        // Center map on current location if this is the first update
        if (mapController != null) {
            mapController.centerMapOn(location);
        }
        
        Toast.makeText(this, "Location updated", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLocationError(String error) {
        Log.e(TAG, "Location error: " + error);
        Toast.makeText(this, "Location error: " + error, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLocationPermissionDenied() {
        Log.w(TAG, "Location permission denied");
        Toast.makeText(this, "Location permission is required for this app", Toast.LENGTH_LONG).show();
    }

    // MapController.MapInteractionListener implementation
    @Override
    public void onMapTap(GeoPoint point) {
        Log.d(TAG, "Map tapped at: " + point + ", Selection mode: " + selectionMode);
        
        switch (selectionMode) {
            case START:
                handleStartLocationSelection(point);
                break;
                
            case DESTINATION:
                handleDestinationSelection(point);
                break;
                
            case NONE:
            default:
                // Default behavior - set as destination
                handleDestinationSelection(point);
                break;
        }
    }
    
    /**
     * Handle start location selection from map tap
     */
    private void handleStartLocationSelection(GeoPoint point) {
        Log.d(TAG, "Start location selected: " + point);
        
        // Set as start location
        startLocation = point;
        
        // Start reverse geocoding to get real address
        locationManager.reverseGeocode(point, new LocationManager.ReverseGeocodeListener() {
            @Override
            public void onReverseGeocodeSuccess(String address) {
                Log.d(TAG, "Start location reverse geocoding successful: " + address);
                startAddress = address;
                
                // Update start location input field
                if (editTextStartLocation != null) {
                    editTextStartLocation.setText(startAddress);
                }
                
                Toast.makeText(PassengerMainScreen.this, "📍 Start location set: " + address, Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onReverseGeocodeError(String error) {
                Log.w(TAG, "Start location reverse geocoding failed: " + error);
                // Fallback to coordinates
                startAddress = "📍 Selected location (" + 
                        String.format("%.4f", point.getLatitude()) + ", " + 
                        String.format("%.4f", point.getLongitude()) + ")";
                
                // Update start location input field
                if (editTextStartLocation != null) {
                    editTextStartLocation.setText(startAddress);
                }
                
                Toast.makeText(PassengerMainScreen.this, "📍 Start location selected", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Add start marker immediately
        mapController.clearMarkers(); // Clear previous markers
        mapController.addStartMarker(point);
        
        // Reset selection mode
        selectionMode = SelectionMode.NONE;
    }
    
    /**
     * Handle destination selection from map tap
     */
    private void handleDestinationSelection(GeoPoint point) {
        Log.d(TAG, "Destination selected: " + point);
        
        // Set as destination
        destinationLocation = point;
        
        // Start reverse geocoding to get real address
        locationManager.reverseGeocode(point, new LocationManager.ReverseGeocodeListener() {
            @Override
            public void onReverseGeocodeSuccess(String address) {
                Log.d(TAG, "Destination reverse geocoding successful: " + address);
                destinationAddress = address;
                
                // Update autocomplete text with real address
                autocompleteController.setText(destinationAddress);
                
                Toast.makeText(PassengerMainScreen.this, "🎯 Destination set: " + address, Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onReverseGeocodeError(String error) {
                Log.w(TAG, "Destination reverse geocoding failed: " + error);
                // Fallback to coordinates
                destinationAddress = "📍 Selected location (" + 
                        String.format("%.4f", point.getLatitude()) + ", " + 
                        String.format("%.4f", point.getLongitude()) + ")";
                
                // Update autocomplete text
                autocompleteController.setText(destinationAddress);
                
                Toast.makeText(PassengerMainScreen.this, "🎯 Destination selected", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Add destination marker (keep existing start marker if present)
        mapController.addDestinationMarker(point);
        
        // Reset selection mode
        selectionMode = SelectionMode.NONE;
    }

    // AutocompleteController.AutocompleteListener implementation
    @Override
    public void onSuggestionSelected(String suggestion, GeoPoint coordinates) {
        Log.d(TAG, "Suggestion selected: " + suggestion + " at " + coordinates);
        
        destinationLocation = coordinates;
        destinationAddress = suggestion;
        
        // Add destination marker and center map
        mapController.addDestinationMarker(coordinates);
        mapController.centerMapOn(coordinates);
        
        Toast.makeText(this, "🎯 Destination set: " + suggestion, Toast.LENGTH_SHORT).show();
        
        // Reset selection mode since destination was set via autocomplete
        selectionMode = SelectionMode.NONE;
    }

    @Override
    public void onTextChanged(String text) {
        Log.d(TAG, "Autocomplete text changed: " + text);
        
        // Clear destination if text is empty
        if (text.isEmpty()) {
            destinationLocation = null;
            destinationAddress = null;
            mapController.clearMarkers();
        }
    }

    // Permission handling
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Location permission granted");
                startLocationServices();
            } else {
                Log.w(TAG, "Location permission denied");
                Toast.makeText(this, "Location permission is required for this app", 
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    // Activity lifecycle methods
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        
        if (mapController != null) {
            mapController.onResume();
        }
        
        if (locationManager != null) {
            locationManager.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        
        if (mapController != null) {
            mapController.onPause();
        }
        
        if (locationManager != null) {
            locationManager.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        
        // Cleanup controllers
        if (mapController != null) {
            mapController.onDestroy();
        }
        
        if (locationManager != null) {
            locationManager.cleanup();
        }
        
        if (autocompleteController != null) {
            autocompleteController.cleanup();
        }
    }

    /**
     * Calculate straight-line distance between two points in kilometers
     */
    private double calculateDistance(GeoPoint start, GeoPoint end) {
        final int R = 6371; // Radius of the earth in km
        
        double latDistance = Math.toRadians(end.getLatitude() - start.getLatitude());
        double lonDistance = Math.toRadians(end.getLongitude() - start.getLongitude());
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(start.getLatitude())) * Math.cos(Math.toRadians(end.getLatitude()))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c; // Distance in km
    }

    @Override
    public void onStartLocationSelected(GeoPoint point) {
        // This method is now handled by handleStartLocationSelection
        handleStartLocationSelection(point);
    }

    @Override
    public void onDestinationSelected(GeoPoint point) {
        // This method is now handled by handleDestinationSelection  
        handleDestinationSelection(point);
    }
}