package com.makitaxi.passenger;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.makitaxi.R;
import com.makitaxi.utils.PreferencesManager;

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
    private Button btnUseCurrentLocation;
    private Button btnUseCurrentEndLocation;
    private Button btnSelectStartOnMap;
    private Button btnClearRoute;
    private TextView txtToggleControls;
    private LinearLayout locationControlsLayout;
    private LinearLayout actionButtonsLayout;
    
    // Bottom navigation buttons
    private Button btnAccount;
    private Button btnGPS;
    private Button btnZoomIn;
    private Button btnZoomOut;

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
    private boolean controlsVisible = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.passanger_main_screen);
        handleSystemBars();
        initializeViews();
        initializeControllers();
        setupUIInteractions();
        checkLocationPermissions();
    }

    private void handleSystemBars() {
        View rootView = findViewById(android.R.id.content);
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(
                    androidx.core.view.WindowInsetsCompat.Type.systemBars()
            );
            v.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    systemBars.bottom
            );
            return insets;
        });
    }
    /**
     * Initialize UI components
     */
    private void initializeViews() {
        mapView = findViewById(R.id.mapView);
        editTextStartLocation = findViewById(R.id.editTextStartLocation);
        editTextEndLocation = findViewById(R.id.editTextEndLocation);
        buttonSelectOnMap = findViewById(R.id.btnSelectOnMap);
        buttonShowRoute = findViewById(R.id.btnShowRoute);
        btnUseCurrentLocation = findViewById(R.id.btnUseCurrentLocation);
        btnUseCurrentEndLocation = findViewById(R.id.btnUseCurrentEndLocation);
        btnSelectStartOnMap = findViewById(R.id.btnSelectStartOnMap);
        btnClearRoute = findViewById(R.id.btnClearRoute);
        txtToggleControls = findViewById(R.id.toggleControls);
        locationControlsLayout = findViewById(R.id.location_controls_layout);
        actionButtonsLayout = findViewById(R.id.action_buttons_layout);
        btnAccount = findViewById(R.id.btnAccount);
        btnGPS = findViewById(R.id.btnGPS);
        btnZoomIn = findViewById(R.id.btnZoomIn);
        btnZoomOut = findViewById(R.id.btnZoomOut);
    }

    /**
     * Initialize all controllers
     */
    private void initializeControllers() {
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
        
        // Setup start location text input handling
        setupStartLocationTextWatcher();
        
        // Setup destination text input handling
        setupDestinationTextWatcher();
        
        // Toggle controls TextView
        if (txtToggleControls != null) {
            txtToggleControls.setOnClickListener(v -> toggleControls());
        }
        
        // Bottom navigation button listeners
        if (btnAccount != null) {
            btnAccount.setOnClickListener(v -> handleAccount());
        }
        
        if (btnGPS != null) {
            btnGPS.setOnClickListener(v -> handleGPS());
        }
        
        if (btnZoomIn != null) {
            btnZoomIn.setOnClickListener(v -> handleZoomIn());
        }
        
        if (btnZoomOut != null) {
            btnZoomOut.setOnClickListener(v -> handleZoomOut());
        }
        
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
        
        // Use current location for end location button
        if (btnUseCurrentEndLocation != null) {
            btnUseCurrentEndLocation.setOnClickListener(v -> handleUseCurrentEndLocation());
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
                Toast.makeText(this, " Tap on the map to select destination", Toast.LENGTH_SHORT).show();
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
        
        Log.d(TAG, "Button listeners setup completed");
    }

    /**
     * Set up text watcher for start location input
     */
    private void setupStartLocationTextWatcher() {
        if (editTextStartLocation != null) {
            editTextStartLocation.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    String text = s.toString().trim();
                    if (text.isEmpty()) {
                        // Clear start location if text is empty
                        startLocation = null;
                        startAddress = null;
                        Log.d(TAG, "Start location cleared - text empty");
                    }
                }
            });
        }
    }

    /**
     * Set up text watcher for destination input to handle manual typing
     */
    private void setupDestinationTextWatcher() {
        if (autocompleteController != null) {
            // Get the AutoCompleteTextView from the controller
            AutoCompleteTextView destTextView = autocompleteController.getAutoCompleteTextView();
            if (destTextView != null) {
                destTextView.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        String text = s.toString().trim();
                        if (text.isEmpty()) {
                            // Clear destination location if text is empty
                            destinationLocation = null;
                            destinationAddress = null;
                            Log.d(TAG, "Destination location cleared - text empty");
                        }
                    }
                });
            }
        }
    }
    private void geocodeStartAddress(String address) {
        Log.d(TAG, "Geocoding start address: " + address);
        
        if (locationManager != null) {
            locationManager.geocodeAddress(address, new LocationManager.GeocodeCallback() {
                @Override
                public void onLocationFound(double latitude, double longitude, String foundAddress) {
                    startLocation = new GeoPoint(latitude, longitude);
                    startAddress = foundAddress;
                    
                    runOnUiThread(() -> {
                        if (editTextStartLocation != null) {
                            editTextStartLocation.setText(foundAddress);
                        }
                        Toast.makeText(PassengerMainScreen.this, "✅ Start location found: " + foundAddress, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Start location geocoded successfully: " + startLocation);
                    });
                }
                
                @Override
                public void onLocationNotFound() {
                    runOnUiThread(() -> {
                        Toast.makeText(PassengerMainScreen.this, "❌ Could not find start location: " + address, Toast.LENGTH_SHORT).show();
                        Log.w(TAG, "Start location geocoding failed for: " + address);
                    });
                }
                
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(PassengerMainScreen.this, "❌ Error finding start location: " + error, Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Start location geocoding error: " + error);
                    });
                }
            });
        }
    }

    /**
     * Geocode destination address to coordinates
     */
    private void geocodeDestinationAddress(String address) {
        Log.d(TAG, "Geocoding destination address: " + address);
        
        if (locationManager != null) {
            locationManager.geocodeAddress(address, new LocationManager.GeocodeCallback() {
                @Override
                public void onLocationFound(double latitude, double longitude, String foundAddress) {
                    destinationLocation = new GeoPoint(latitude, longitude);
                    destinationAddress = foundAddress;
                    
                    runOnUiThread(() -> {
                        if (autocompleteController != null) {
                            autocompleteController.setText(foundAddress);
                        }
                        Toast.makeText(PassengerMainScreen.this, "✅ Destination found: " + foundAddress, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Destination location geocoded successfully: " + destinationLocation);
                    });
                }
                
                @Override
                public void onLocationNotFound() {
                    runOnUiThread(() -> {
                        Toast.makeText(PassengerMainScreen.this, "❌ Could not find destination: " + address, Toast.LENGTH_SHORT).show();
                        Log.w(TAG, "Destination location geocoding failed for: " + address);
                    });
                }
                
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(PassengerMainScreen.this, "❌ Error finding destination: " + error, Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Destination location geocoding error: " + error);
                    });
                }
            });
        }
    }

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
        Log.d(TAG, "Show route button clicked");
        
        // Check start location
        GeoPoint routeStartLocation = (startLocation != null) ? startLocation : currentLocation;
        String startText = editTextStartLocation != null ? editTextStartLocation.getText().toString().trim() : "";
        String destText = autocompleteController != null ? autocompleteController.getText() : "";
        
        Log.d(TAG, "Route validation - Start location: " + routeStartLocation + ", Destination: " + destinationLocation);
        Log.d(TAG, "Text inputs - Start: '" + startText + "', Destination: '" + destText + "'");
        
        // Validate start location
        if (routeStartLocation == null) {
            if (!startText.isEmpty()) {
                Toast.makeText(this, "⏳ Finding start location: " + startText + "\nPlease wait...", Toast.LENGTH_SHORT).show();
                geocodeStartAddress(startText);
                return;
            } else {
                Toast.makeText(this, "❌ Please enter a start location or use current location", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "Cannot show route: no start location");
                return;
            }
        }
        
        // Validate destination location
        if (destinationLocation == null) {
            if (!destText.isEmpty()) {
                Toast.makeText(this, "⏳ Finding destination: " + destText + "\nPlease wait...", Toast.LENGTH_SHORT).show();
                geocodeDestinationAddress(destText);
                return;
            } else {
                Toast.makeText(this, "❌ Please enter a destination", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "Cannot show route: no destination");
                return;
            }
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
                String finalStartText = (startLocation != null && startAddress != null) ? startAddress : 
                                       (routeStartLocation.equals(currentLocation)) ? "Current Location" : startText;
                String finalDestText = (destinationAddress != null && !destinationAddress.isEmpty()) ? destinationAddress : destText;
                
                String distanceText = String.format("%.1f km", distanceKm);
                String durationText = String.format("%.0f min", durationMinutes);
                
                Toast.makeText(PassengerMainScreen.this, 
                        "🛣️ Route calculated!\n" +
                        "📍 From: " + finalStartText + "\n" +
                        "🎯 To: " + finalDestText + "\n" +
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
                
                String finalStartText = (startLocation != null && startAddress != null) ? startAddress : 
                                       (routeStartLocation.equals(currentLocation)) ? "Current Location" : startText;
                String finalDestText = (destinationAddress != null && !destinationAddress.isEmpty()) ? destinationAddress : destText;
                
                Toast.makeText(PassengerMainScreen.this, 
                        "⚠️ Route service unavailable\n" +
                        "📍 From: " + finalStartText + "\n" +
                        "🎯 To: " + finalDestText + "\n" +
                        "📏 Straight-line distance: " + distanceText, 
                        Toast.LENGTH_LONG).show();
                
                Log.w(TAG, "Using fallback straight-line distance: " + distanceText);
            }
        });
        
        // Reset selection mode
        selectionMode = SelectionMode.NONE;
    }

    /**
     * Handle use current location button click
     */
    private void handleUseCurrentLocation() {
        Log.d(TAG, "Handling use current location");
        
        if (currentLocation == null) {
            Toast.makeText(this, "Current location not available. Please wait...", Toast.LENGTH_SHORT).show();
            
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
     * Handle use current location for end location button click
     */
    private void handleUseCurrentEndLocation() {
        Log.d(TAG, "Handling use current location for end location");
        
        if (currentLocation == null) {
            Toast.makeText(this, "Current location not available. Please wait...", Toast.LENGTH_SHORT).show();
            
            if (locationManager != null) {
                locationManager.getLastKnownLocation();
            }
            return;
        }
        
        // Set current location as destination location
        destinationLocation = currentLocation;
        destinationAddress = "Current Location";
        
        // Update destination location input field
        if (autocompleteController != null) {
            autocompleteController.setText(destinationAddress);
        }
        
        // Use current location as destination point
        mapController.addDestinationMarker(currentLocation);
        mapController.centerMapOn(currentLocation);
        
        Toast.makeText(this, "📍 Current location set as drop-off point", Toast.LENGTH_SHORT).show();
        
        // Try to get actual address for current location
        locationManager.reverseGeocode(currentLocation, new LocationManager.ReverseGeocodeListener() {
            @Override
            public void onReverseGeocodeSuccess(String address) {
                Log.d(TAG, "Current location reverse geocoding successful for end location: " + address);
                destinationAddress = address;
                
                // Update destination location input field with real address
                if (autocompleteController != null) {
                    autocompleteController.setText(destinationAddress);
                }
            }
            
            @Override
            public void onReverseGeocodeError(String error) {
                Log.w(TAG, "Current location reverse geocoding failed for end location: " + error);
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

    private void toggleControls() {
        controlsVisible = !controlsVisible;
        if (locationControlsLayout != null && actionButtonsLayout != null && txtToggleControls != null) {
            if (controlsVisible) {
                locationControlsLayout.setVisibility(View.VISIBLE);
                actionButtonsLayout.setVisibility(View.VISIBLE);
                txtToggleControls.setText("\uD83D\uDE95 Reserve a ride ▼");
                Log.d(TAG, "Route controls shown");
            } else {
                locationControlsLayout.setVisibility(View.GONE);
                actionButtonsLayout.setVisibility(View.GONE);
                txtToggleControls.setText("\uD83D\uDE95 Reserve a ride ▲");
                Log.d(TAG, "Route controls hidden");
            }
        }
    }

    private void handleAccount() {
        Log.d(TAG, "Account button clicked");
        Toast.makeText(this, "👤 Account settings coming soon", Toast.LENGTH_SHORT).show();
        // TODO: Implement account/profile screen navigation
    }

    private void handleGPS() {
        Log.d(TAG, "GPS button clicked");
        if (currentLocation != null) {
            mapController.centerMapOn(currentLocation);
            Toast.makeText(this, "📍 Centered on your location", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "📍 Getting your location...", Toast.LENGTH_SHORT).show();
            if (locationManager != null) {
                locationManager.getLastKnownLocation();
            }
        }
    }

    private void handleZoomIn() {
        Log.d(TAG, "Zoom in button clicked");
        if (mapController != null) {
            mapController.zoomIn();
        }
    }

    private void handleZoomOut() {
        Log.d(TAG, "Zoom out button clicked");
        if (mapController != null) {
            mapController.zoomOut();
        }
    }
}