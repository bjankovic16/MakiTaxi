package com.makitaxi.passenger;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.makitaxi.R;
import com.makitaxi.login.Login;
import com.makitaxi.utils.PreferencesManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * PassengerMainScreen - Comprehensive OpenStreetMap implementation for taxi passenger app
 * 
 * This class demonstrates:
 * 1. OSMDroid MapView setup and configuration
 * 2. Location services integration (GPS)
 * 3. Map interaction (tap to select destination)
 * 4. Geocoding (address ↔ coordinates conversion)
 * 5. Route planning with OSRM API
 * 6. Polyline drawing for route visualization
 * 7. Custom markers for start/end points
 * 8. Map overlays and event handling
 */
public class PassengerMainScreen extends AppCompatActivity implements LocationListener, MapEventsReceiver {

    private static final String TAG = "PassengerMainScreen";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    
    // ========== OSMDroid Core Components ==========
    /**
     * MapView - The main OpenStreetMap component
     * This is the central widget that displays the map tiles and handles user interaction
     */
    private MapView mapView;
    
    /**
     * IMapController - Controls map operations like zoom, center, animation
     * Used to programmatically control the map view
     */
    private IMapController mapController;
    
    /**
     * MyLocationNewOverlay - Handles current location display and following
     * Shows user's current position with a blue dot and accuracy circle
     */
    private MyLocationNewOverlay myLocationOverlay;
    
    // ========== Location Services ==========
    private LocationManager locationManager;
    private Geocoder geocoder; // Converts addresses ↔ coordinates
    
    // ========== HTTP Client for Routing ==========
    private OkHttpClient httpClient; // For API calls to routing service
    private ExecutorService executorService; // Background thread management
    
    // ========== UI Components ==========
    private EditText editTextStartLocation;
    private EditText editTextEndLocation;
    private Button btnUseCurrentLocation;
    private Button btnSelectOnMap;
    private Button btnShowRoute;
    private Button btnClearRoute;
    private LinearLayout routeInfoPanel;
    private TextView textViewRouteDetails;
    
    // ========== Map State Variables ==========
    private GeoPoint startPoint;    // Starting location coordinates
    private GeoPoint endPoint;      // Destination coordinates
    private boolean selectingDestination = false; // Flag for map tap mode
    
    // ========== Map Overlays (Visual Elements) ==========
    private Marker startMarker;     // Green marker for start location
    private Marker endMarker;       // Red marker for destination
    private Polyline routePolyline; // Blue line showing the route
    private MapEventsOverlay mapEventsOverlay; // Handles map tap events

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.passanger_main_screen);

        // Handle system bars (status bar and navigation bar)
        handleSystemBars();

        // Handle back button press with modern approach
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Move app to background instead of going back to login
                moveTaskToBack(true);
            }
        });

        // Initialize all components
        initializeOSMDroid();
        initializeUIComponents();
        initializeLocationServices();
        initializeMapFeatures();
        requestLocationPermissions();
    }

    /**
     * Handle System Bars (Status Bar and Navigation Bar)
     * This ensures content is not covered by system UI elements
     */
    private void handleSystemBars() {
        // Get the root view
        View rootView = findViewById(android.R.id.content);
        
        // Set up window insets listener to handle system bars
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            // Get system bars insets (status bar, navigation bar)
            androidx.core.graphics.Insets systemBars = insets.getInsets(
                androidx.core.view.WindowInsetsCompat.Type.systemBars()
            );
            
            // Apply padding to avoid content being covered by system bars
            // Top padding for status bar, bottom padding for navigation bar
            v.setPadding(
                systemBars.left,    // Left padding (usually 0)
                systemBars.top,     // Top padding (status bar)
                systemBars.right,   // Right padding (usually 0)
                systemBars.bottom   // Bottom padding (navigation bar)
            );
            
            return insets;
        });
    }

    /**
     * STEP 1: Initialize OSMDroid Configuration
     * This is crucial for OSMDroid to work properly
     */
    private void initializeOSMDroid() {
        Log.d(TAG, "Initializing OSMDroid configuration");
        
        // Load OSMDroid configuration from SharedPreferences
        // This handles tile caching, user agent, and other settings
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferencesManager.getSharedPreferences(ctx));
        
        // Initialize HTTP client for routing API calls
        httpClient = new OkHttpClient();
        executorService = Executors.newFixedThreadPool(2);
        
        // Initialize geocoder for address conversion
        geocoder = new Geocoder(this, Locale.getDefault());
    }

    /**
     * STEP 2: Initialize UI Components
     * Connect Java variables to XML layout elements
     */
    private void initializeUIComponents() {
        Log.d(TAG, "Initializing UI components");
        
        // Map view - the main OpenStreetMap component
        mapView = findViewById(R.id.mapView);
        
        // Location input controls
        editTextStartLocation = findViewById(R.id.editTextStartLocation);
        editTextEndLocation = findViewById(R.id.editTextEndLocation);
        btnUseCurrentLocation = findViewById(R.id.btnUseCurrentLocation);
        btnSelectOnMap = findViewById(R.id.btnSelectOnMap);
        
        // Action buttons
        btnShowRoute = findViewById(R.id.btnShowRoute);
        btnClearRoute = findViewById(R.id.btnClearRoute);
        
        // Route information panel
        routeInfoPanel = findViewById(R.id.route_info_panel);
        textViewRouteDetails = findViewById(R.id.textViewRouteDetails);
        
        // Logout button
        Button btnLogout = findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(PassengerMainScreen.this, Login.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
        
        // Set up button click listeners
        setupButtonListeners();
    }

    /**
     * STEP 3: Configure MapView and Basic Map Settings
     */
    private void initializeMapFeatures() {
        Log.d(TAG, "Setting up MapView configuration");
        
        // ========== Basic Map Configuration ==========
        
        // Set tile source - this determines which map tiles to download
        // MAPNIK is the standard OpenStreetMap tile source
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        
        // Enable multi-touch controls (pinch to zoom, two-finger rotate)
        mapView.setMultiTouchControls(true);
        
        // Show built-in zoom controls (+/- buttons)
        mapView.setBuiltInZoomControls(true);
        
        // Get map controller for programmatic control
        mapController = mapView.getController();
        
        // Set initial zoom level (1-21, where 21 is most zoomed in)
        mapController.setZoom(15.0);
        
        // Set initial center point to Belgrade, Serbia
        GeoPoint belgradeCenter = new GeoPoint(44.7866, 20.4489);
        mapController.setCenter(belgradeCenter);
        
        // ========== Location Overlay Setup ==========
        
        // Create location overlay to show user's current position
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mapView);
        
        // Enable location display (blue dot)
        myLocationOverlay.enableMyLocation();
        
        // Enable follow mode (map centers on user location)
        myLocationOverlay.enableFollowLocation();
        
        // Show accuracy circle around location
        myLocationOverlay.setDrawAccuracyEnabled(true);
        
        // Add location overlay to map
        mapView.getOverlays().add(myLocationOverlay);
        
        // ========== Map Events Setup ==========
        
        // Create map events overlay to handle tap events
        mapEventsOverlay = new MapEventsOverlay(this);
        mapView.getOverlays().add(mapEventsOverlay);
        
        Log.d(TAG, "MapView configuration completed");
    }

    /**
     * STEP 4: Set up button click listeners
     */
    private void setupButtonListeners() {
        // Current location button - fills start location with GPS coordinates
        btnUseCurrentLocation.setOnClickListener(v -> useCurrentLocationAsStart());
        
        // Map selection button - enables destination selection by tapping map
        btnSelectOnMap.setOnClickListener(v -> enableDestinationSelection());
        
        // Show route button - calculates and displays route
        btnShowRoute.setOnClickListener(v -> calculateAndShowRoute());
        
        // Clear route button - removes route and markers from map
        btnClearRoute.setOnClickListener(v -> clearRoute());
    }

    /**
     * STEP 5: Initialize Location Services
     */
    private void initializeLocationServices() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    /**
     * STEP 6: Request Location Permissions
     * Required for GPS functionality
     */
    private void requestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            startLocationServices();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationServices();
            } else {
                Toast.makeText(this, "Location permission is required for this feature", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * STEP 7: Start Location Services
     */
    private void startLocationServices() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Request location updates from GPS and Network providers
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, this);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3000, 10, this);
            
            // Try to get last known location for immediate display
            Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastKnownLocation == null) {
                lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            
            if (lastKnownLocation != null) {
                onLocationChanged(lastKnownLocation);
            }
        }
    }

    // ========== LOCATION HANDLING ==========

    @Override
    public void onLocationChanged(@NonNull Location location) {
        Log.d(TAG, "Location updated: " + location.getLatitude() + ", " + location.getLongitude());
        
        // Convert Android Location to OSMDroid GeoPoint
        GeoPoint currentLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
        
        // Center map on current location (only on first location fix)
        if (mapView.getZoomLevelDouble() == 15.0) {
            mapController.animateTo(currentLocation);
            mapController.setZoom(16.0);
        }
        
        // Refresh map display
        mapView.invalidate();
    }

    /**
     * Use Current Location as Start Point
     * This method gets the user's current GPS location and sets it as the starting point
     */
    private void useCurrentLocationAsStart() {
        if (myLocationOverlay.getMyLocation() != null) {
            GeoPoint currentPos = myLocationOverlay.getMyLocation();
            startPoint = currentPos;
            
            Log.d(TAG, "Using current location as start: " + currentPos.getLatitude() + ", " + currentPos.getLongitude());
            
            // Use reverse geocoding to convert coordinates to address
            executorService.execute(() -> {
                try {
                    // Geocoding: Convert coordinates to human-readable address
                    List<Address> addresses = geocoder.getFromLocation(
                        currentPos.getLatitude(), 
                        currentPos.getLongitude(), 
                        1
                    );
                    
                    // Update UI on main thread
                    runOnUiThread(() -> {
                        if (addresses != null && !addresses.isEmpty()) {
                            Address address = addresses.get(0);
                            String addressText = address.getAddressLine(0);
                            editTextStartLocation.setText(addressText);
                        } else {
                            editTextStartLocation.setText("Current Location");
                        }
                        Toast.makeText(this, "Current location set as start point", Toast.LENGTH_SHORT).show();
                    });
                } catch (IOException e) {
                    Log.e(TAG, "Reverse geocoding failed", e);
                    runOnUiThread(() -> {
                        editTextStartLocation.setText("Current Location");
                        Toast.makeText(this, "Current location set as start point", Toast.LENGTH_SHORT).show();
                    });
                }
            });
        } else {
            Toast.makeText(this, "Current location not available", Toast.LENGTH_SHORT).show();
        }
    }

    // ========== MAP INTERACTION ==========

    /**
     * Enable Destination Selection Mode
     * When activated, user can tap on map to select destination
     */
    private void enableDestinationSelection() {
        selectingDestination = true;
        Toast.makeText(this, "Tap on the map to select destination", Toast.LENGTH_LONG).show();
        btnSelectOnMap.setText("🎯 Selecting...");
        btnSelectOnMap.setEnabled(false);
    }

    /**
     * MapEventsReceiver Implementation - Handle Map Tap Events
     */
    @Override
    public boolean singleTapConfirmedHelper(GeoPoint p) {
        Log.d(TAG, "Map tapped at: " + p.getLatitude() + ", " + p.getLongitude());
        
        if (selectingDestination) {
            // User tapped to select destination
            endPoint = p;
            selectingDestination = false;
            
            // Reset button state
            btnSelectOnMap.setText("🗺️ Map");
            btnSelectOnMap.setEnabled(true);
            
            // Reverse geocode the tapped location
            executorService.execute(() -> {
                try {
                    List<Address> addresses = geocoder.getFromLocation(p.getLatitude(), p.getLongitude(), 1);
                    
                    runOnUiThread(() -> {
                        if (addresses != null && !addresses.isEmpty()) {
                            Address address = addresses.get(0);
                            String addressText = address.getAddressLine(0);
                            editTextEndLocation.setText(addressText);
                        } else {
                            editTextEndLocation.setText("Selected Location");
                        }
                        
                        // Add temporary marker at selected location
                        addDestinationMarker(p);
                        Toast.makeText(this, "Destination selected", Toast.LENGTH_SHORT).show();
                    });
                } catch (IOException e) {
                    Log.e(TAG, "Reverse geocoding failed for tapped location", e);
                    runOnUiThread(() -> {
                        editTextEndLocation.setText("Selected Location");
                        addDestinationMarker(p);
                        Toast.makeText(this, "Destination selected", Toast.LENGTH_SHORT).show();
                    });
                }
            });
            
            return true; // Event consumed
        }
        
        return false; // Event not consumed
    }

    @Override
    public boolean longPressHelper(GeoPoint p) {
        // Handle long press if needed
        return false;
    }

    /**
     * Add Destination Marker to Map
     * Creates a red marker at the selected destination point
     */
    private void addDestinationMarker(GeoPoint point) {
        // Remove existing end marker if present
        if (endMarker != null) {
            mapView.getOverlays().remove(endMarker);
        }
        
        // Create new destination marker
        endMarker = new Marker(mapView);
        endMarker.setPosition(point);
        endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        endMarker.setTitle("Destination");
        
        // Set marker icon (you can customize this with your own drawable)
        endMarker.setIcon(ContextCompat.getDrawable(this, android.R.drawable.ic_menu_myplaces));
        
        // Add marker to map
        mapView.getOverlays().add(endMarker);
        mapView.invalidate();
    }

    // ========== ROUTE PLANNING ==========

    /**
     * Calculate and Show Route
     * Main method for route planning and visualization
     */
    private void calculateAndShowRoute() {
        try {
            String startLocationText = editTextStartLocation.getText().toString().trim();
            String endLocationText = editTextEndLocation.getText().toString().trim();

            if (startLocationText.isEmpty() || endLocationText.isEmpty()) {
                Toast.makeText(this, "Please enter both start and end locations", Toast.LENGTH_SHORT).show();
                return;
            }

            // Show loading state
            btnShowRoute.setEnabled(false);
            btnShowRoute.setText("Calculating...");
            Toast.makeText(this, "Calculating route...", Toast.LENGTH_SHORT).show();

            // Simple route calculation - use current location if available
            if (startLocationText.equalsIgnoreCase("current location") && myLocationOverlay.getMyLocation() != null) {
                startPoint = myLocationOverlay.getMyLocation();
                Log.d(TAG, "Using current location as start point");
            }

            // For demo purposes, if we don't have coordinates, create simple test route
            if (startPoint == null || endPoint == null) {
                // Create a simple demo route in Belgrade
                startPoint = new GeoPoint(44.7866, 20.4489); // Belgrade center
                endPoint = new GeoPoint(44.8125, 20.4612);   // Belgrade north
                
                runOnUiThread(() -> {
                    editTextStartLocation.setText("Belgrade Center");
                    editTextEndLocation.setText("Belgrade North");
                    createSimpleRoute();
                });
                return;
            }

            // If we have both points, try to get real route
            getRouteFromOSRM(startPoint, endPoint);
            
        } catch (Exception e) {
            Log.e(TAG, "Error in calculateAndShowRoute", e);
            runOnUiThread(() -> {
                Toast.makeText(this, "Error calculating route", Toast.LENGTH_SHORT).show();
                resetRouteButton();
            });
        }
    }

    /**
     * Display Route on Map - Simplified and safer
     */
    private void displayRoute(List<GeoPoint> routePoints, double distance, double duration) {
        try {
            Log.d(TAG, "Displaying route on map");
            
            // Validate input parameters
            if (routePoints == null || routePoints.isEmpty()) {
                Log.w(TAG, "Route points are null or empty");
                Toast.makeText(this, "No route data available", Toast.LENGTH_SHORT).show();
                resetRouteButton();
                return;
            }
            
            // Clear any existing route first
            clearRouteElements();
            
            // Create route polyline
            routePolyline = new Polyline();
            routePolyline.setPoints(routePoints);
            routePolyline.setColor(Color.BLUE);
            routePolyline.setWidth(8.0f);
            
            // Add polyline to map
            mapView.getOverlays().add(routePolyline);
            
            // Create start marker only if startPoint is not null
            if (startPoint != null) {
                try {
                    startMarker = new Marker(mapView);
                    startMarker.setPosition(startPoint);
                    startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    startMarker.setTitle("Start");
                    mapView.getOverlays().add(startMarker);
                    Log.d(TAG, "Start marker created at: " + startPoint.getLatitude() + ", " + startPoint.getLongitude());
                } catch (Exception e) {
                    Log.e(TAG, "Error creating start marker", e);
                }
            } else {
                // Use first route point as start if startPoint is null
                if (!routePoints.isEmpty()) {
                    try {
                        startMarker = new Marker(mapView);
                        startMarker.setPosition(routePoints.get(0));
                        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                        startMarker.setTitle("Start");
                        mapView.getOverlays().add(startMarker);
                        Log.d(TAG, "Start marker created from route points");
                    } catch (Exception e) {
                        Log.e(TAG, "Error creating start marker from route points", e);
                    }
                }
            }
            
            // Create end marker only if endPoint is not null
            if (endPoint != null) {
                try {
                    endMarker = new Marker(mapView);
                    endMarker.setPosition(endPoint);
                    endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    endMarker.setTitle("Destination");
                    mapView.getOverlays().add(endMarker);
                    Log.d(TAG, "End marker created at: " + endPoint.getLatitude() + ", " + endPoint.getLongitude());
                } catch (Exception e) {
                    Log.e(TAG, "Error creating end marker", e);
                }
            } else {
                // Use last route point as end if endPoint is null
                if (!routePoints.isEmpty()) {
                    try {
                        endMarker = new Marker(mapView);
                        endMarker.setPosition(routePoints.get(routePoints.size() - 1));
                        endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                        endMarker.setTitle("Destination");
                        mapView.getOverlays().add(endMarker);
                        Log.d(TAG, "End marker created from route points");
                    } catch (Exception e) {
                        Log.e(TAG, "Error creating end marker from route points", e);
                    }
                }
            }
            
            // Show route information
            String routeInfo = String.format(Locale.getDefault(), 
                "Distance: %.1f km | Time: %.0f min", distance, duration);
            
            if (textViewRouteDetails != null) {
                textViewRouteDetails.setText(routeInfo);
            }
            
            if (routeInfoPanel != null) {
                routeInfoPanel.setVisibility(View.VISIBLE);
            }
            
            // Auto-zoom to show route
            try {
                if (routePolyline.getBounds() != null) {
                    mapView.zoomToBoundingBox(routePolyline.getBounds(), false);
                }
            } catch (Exception e) {
                Log.w(TAG, "Error zooming to route bounds", e);
            }
            
            // Refresh map display
            mapView.invalidate();
            
            Toast.makeText(this, "Route: " + routeInfo, Toast.LENGTH_LONG).show();
            
            Log.d(TAG, "Route display completed successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error displaying route", e);
            Toast.makeText(this, "Error displaying route", Toast.LENGTH_SHORT).show();
            resetRouteButton();
        }
    }

    /**
     * Create a simple demo route for testing
     */
    private void createSimpleRoute() {
        try {
            Log.d(TAG, "Creating simple demo route");
            
            // Ensure we have valid start and end points
            if (startPoint == null) {
                startPoint = new GeoPoint(44.7866, 20.4489); // Belgrade center
                Log.d(TAG, "Set default start point: Belgrade center");
            }
            
            if (endPoint == null) {
                endPoint = new GeoPoint(44.8125, 20.4612); // Belgrade north
                Log.d(TAG, "Set default end point: Belgrade north");
            }
            
            // Create simple route points
            List<GeoPoint> routePoints = new ArrayList<>();
            routePoints.add(startPoint);
            
            // Add intermediate points for a curved route
            double latDiff = (endPoint.getLatitude() - startPoint.getLatitude()) / 3;
            double lonDiff = (endPoint.getLongitude() - startPoint.getLongitude()) / 3;
            
            routePoints.add(new GeoPoint(
                startPoint.getLatitude() + latDiff, 
                startPoint.getLongitude() + lonDiff
            ));
            routePoints.add(new GeoPoint(
                startPoint.getLatitude() + 2 * latDiff, 
                startPoint.getLongitude() + 2 * lonDiff
            ));
            routePoints.add(endPoint);
            
            // Calculate approximate distance and time
            double distance = startPoint.distanceToAsDouble(endPoint) / 1000; // km
            double duration = distance * 2; // Assume 30 km/h average speed (2 min per km)
            
            Log.d(TAG, "Simple route created with " + routePoints.size() + " points");
            
            displayRoute(routePoints, distance, duration);
            resetRouteButton();
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating simple route", e);
            Toast.makeText(this, "Error creating route", Toast.LENGTH_SHORT).show();
            resetRouteButton();
        }
    }

    /**
     * Get Route from OSRM API - Simplified version
     */
    private void getRouteFromOSRM(GeoPoint start, GeoPoint end) {
        try {
            // Build OSRM API URL
            String url = String.format(Locale.US, 
                "https://router.project-osrm.org/route/v1/driving/%f,%f;%f,%f?overview=full&geometries=geojson",
                start.getLongitude(), start.getLatitude(),
                end.getLongitude(), end.getLatitude()
            );

            Log.d(TAG, "Requesting route from OSRM: " + url);

            // Create HTTP request with timeout
            Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", "MakiTaxi/1.0")
                .build();

            // Execute request with timeout handling
            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "OSRM request failed, falling back to simple route", e);
                    // Fallback to simple route
                    runOnUiThread(() -> createSimpleRoute());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {
                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            String responseBody = response.body().string();
                            Log.d(TAG, "OSRM response received");
                            parseAndDisplayRoute(responseBody);
                        } else {
                            Log.w(TAG, "OSRM response not successful, using simple route");
                            runOnUiThread(() -> createSimpleRoute());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing OSRM response, using simple route", e);
                        runOnUiThread(() -> createSimpleRoute());
                    } finally {
                        try {
                            if (response.body() != null) {
                                response.body().close();
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "Error closing response body", e);
                        }
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error creating OSRM request, using simple route", e);
            runOnUiThread(() -> createSimpleRoute());
        }
    }

    /**
     * Parse OSRM Response and Display Route - Simplified
     */
    private void parseAndDisplayRoute(String jsonResponse) {
        try {
            Log.d(TAG, "Parsing OSRM response");
            
            JSONObject json = new JSONObject(jsonResponse);
            
            if (!json.has("routes")) {
                Log.w(TAG, "No routes in response, using simple route");
                runOnUiThread(() -> createSimpleRoute());
                return;
            }
            
            JSONArray routes = json.getJSONArray("routes");
            
            if (routes.length() > 0) {
                JSONObject route = routes.getJSONObject(0);
                JSONObject geometry = route.getJSONObject("geometry");
                JSONArray coordinates = geometry.getJSONArray("coordinates");
                
                double distance = route.getDouble("distance") / 1000; // Convert to km
                double duration = route.getDouble("duration") / 60; // Convert to minutes
                
                List<GeoPoint> routePoints = new ArrayList<>();
                for (int i = 0; i < coordinates.length(); i++) {
                    JSONArray coord = coordinates.getJSONArray(i);
                    double lon = coord.getDouble(0);
                    double lat = coord.getDouble(1);
                    routePoints.add(new GeoPoint(lat, lon));
                }
                
                Log.d(TAG, "Route parsed successfully");
                
                runOnUiThread(() -> {
                    displayRoute(routePoints, distance, duration);
                    resetRouteButton();
                });
            } else {
                Log.w(TAG, "No routes available, using simple route");
                runOnUiThread(() -> createSimpleRoute());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing OSRM response, using simple route", e);
            runOnUiThread(() -> createSimpleRoute());
        }
    }

    /**
     * Clear route elements safely
     */
    private void clearRouteElements() {
        try {
            if (routePolyline != null) {
                mapView.getOverlays().remove(routePolyline);
                routePolyline = null;
            }
            
            if (startMarker != null) {
                mapView.getOverlays().remove(startMarker);
                startMarker = null;
            }
            
            if (endMarker != null) {
                mapView.getOverlays().remove(endMarker);
                endMarker = null;
            }
        } catch (Exception e) {
            Log.w(TAG, "Error clearing route elements", e);
        }
    }

    /**
     * Clear Route from Map - Simplified
     */
    private void clearRoute() {
        try {
            Log.d(TAG, "Clearing route from map");
            
            clearRouteElements();
            
            // Hide route info panel
            if (routeInfoPanel != null) {
                routeInfoPanel.setVisibility(View.GONE);
            }
            
            // Clear location points
            startPoint = null;
            endPoint = null;
            
            // Clear text fields
            if (editTextStartLocation != null) {
                editTextStartLocation.setText("");
            }
            if (editTextEndLocation != null) {
                editTextEndLocation.setText("");
            }
            
            // Refresh map
            mapView.invalidate();
            
        } catch (Exception e) {
            Log.e(TAG, "Error clearing route", e);
        }
    }

    /**
     * Reset route button to normal state
     */
    private void resetRouteButton() {
        try {
            if (btnShowRoute != null) {
                btnShowRoute.setEnabled(true);
                btnShowRoute.setText("🗺️ Show Route");
            }
        } catch (Exception e) {
            Log.w(TAG, "Error resetting route button", e);
        }
    }

    /**
     * Geocoding: Convert Address to Coordinates - Simplified version
     * This method converts a text address to latitude/longitude coordinates
     */
    private GeoPoint geocodeAddress(String address) {
        try {
            Log.d(TAG, "Geocoding address: " + address);
            
            // Check if Geocoder is available
            if (!Geocoder.isPresent()) {
                Log.e(TAG, "Geocoder not available on this device");
                return null;
            }
            
            // Use Android's Geocoder to convert address to coordinates
            List<Address> addresses = geocoder.getFromLocationName(address, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address location = addresses.get(0);
                GeoPoint point = new GeoPoint(location.getLatitude(), location.getLongitude());
                Log.d(TAG, "Geocoded to: " + point.getLatitude() + ", " + point.getLongitude());
                return point;
            } else {
                Log.w(TAG, "No results found for address: " + address);
            }
        } catch (Exception e) {
            Log.e(TAG, "Geocoding failed for: " + address, e);
        }
        return null;
    }

    // ========== LIFECYCLE METHODS ==========

    @Override
    protected void onResume() {
        super.onResume();
        // Resume map rendering
        mapView.onResume();
        
        // Re-enable location overlay
        if (myLocationOverlay != null) {
            myLocationOverlay.enableMyLocation();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Pause map rendering to save battery
        mapView.onPause();
        
        // Disable location overlay
        if (myLocationOverlay != null) {
            myLocationOverlay.disableMyLocation();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Stop location updates
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
        
        // Shutdown executor service
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        
        // Clean up OSMDroid resources
        if (mapView != null) {
            mapView.onDetach();
        }
        
        Log.d(TAG, "PassengerMainScreen destroyed");
    }

    // ========== UNUSED LOCATION LISTENER METHODS ==========
    
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        Toast.makeText(this, provider + " enabled", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        Toast.makeText(this, provider + " disabled", Toast.LENGTH_SHORT).show();
    }
}