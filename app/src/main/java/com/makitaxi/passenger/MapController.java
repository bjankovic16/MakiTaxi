package com.makitaxi.passenger;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.makitaxi.utils.PreferencesManager;

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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MapController - Handles all map-related functionality
 * 
 * Responsibilities:
 * - Map initialization and configuration
 * - Marker management (start, destination)
 * - Route visualization (polylines)
 * - Real routing API integration (OSRM)
 * - Map overlays and interactions
 * - Map events handling
 */
public class MapController implements MapEventsReceiver {
    
    private static final String TAG = "MapController";
    
    // OSRM routing service endpoint
    private static final String OSRM_BASE_URL = "https://router.project-osrm.org/route/v1/driving/";
    
    private Context context;
    private MapView mapView;
    private IMapController osmMapController;
    private MyLocationNewOverlay myLocationOverlay;
    private MapEventsOverlay mapEventsOverlay;
    private ExecutorService executorService;
    private Handler mainHandler;
    
    // Markers and overlays
    private Marker startMarker;
    private Marker endMarker;
    private Polyline routePolyline;
    
    // Map interaction listener
    private MapInteractionListener mapInteractionListener;
    
    /**
     * Interface for map interaction callbacks
     */
    public interface MapInteractionListener {
        void onMapTap(GeoPoint point);
        void onStartLocationSelected(GeoPoint point);
        void onDestinationSelected(GeoPoint point);
    }
    
    /**
     * Interface for routing callbacks
     */
    public interface RoutingCallback {
        void onRouteFound(List<GeoPoint> routePoints, double distance, double duration);
        void onRoutingError(String error);
    }
    
    public MapController(Context context, MapView mapView) {
        this.context = context;
        this.mapView = mapView;
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        
        initializeOSMDroid();
        setupMapView();
    }
    
    public void setMapInteractionListener(MapInteractionListener listener) {
        this.mapInteractionListener = listener;
    }
    
    /**
     * Initialize OSMDroid configuration
     */
    private void initializeOSMDroid() {
        Log.d(TAG, "Initializing OSMDroid configuration");
        
        // Load OSMDroid configuration from SharedPreferences
        Configuration.getInstance().load(context, PreferencesManager.getSharedPreferences(context));
    }
    
    /**
     * Setup MapView with basic configuration
     */
    private void setupMapView() {
        Log.d(TAG, "Setting up MapView configuration");
        
        // Set tile source - this determines which map tiles to download
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        
        // Enable multi-touch controls (pinch to zoom, two-finger rotate)
        mapView.setMultiTouchControls(true);
        
        // Show built-in zoom controls (+/- buttons)
        mapView.setBuiltInZoomControls(true);
        
        // Get map controller for programmatic control
        osmMapController = mapView.getController();
        
        // Set initial zoom level (1-21, where 21 is most zoomed in)
        osmMapController.setZoom(15.0);
        
        // Set initial center point to Belgrade, Serbia
        GeoPoint belgradeCenter = new GeoPoint(44.7866, 20.4489);
        osmMapController.setCenter(belgradeCenter);
        
        // Setup location overlay
        setupLocationOverlay();
        
        // Setup map events
        setupMapEvents();
        
        Log.d(TAG, "MapView configuration completed");
    }
    
    /**
     * Setup location overlay to show user's current position
     */
    private void setupLocationOverlay() {
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(context), mapView);
        
        // Enable location display (blue dot)
        myLocationOverlay.enableMyLocation();
        
        // Enable follow mode (map centers on user location)
        myLocationOverlay.enableFollowLocation();
        
        // Show accuracy circle around location
        myLocationOverlay.setDrawAccuracyEnabled(true);
        
        // Add location overlay to map
        mapView.getOverlays().add(myLocationOverlay);
    }
    
    /**
     * Setup map events overlay to handle tap events
     */
    private void setupMapEvents() {
        mapEventsOverlay = new MapEventsOverlay(this);
        mapView.getOverlays().add(mapEventsOverlay);
    }
    
    /**
     * Get current location from overlay
     */
    public GeoPoint getCurrentLocation() {
        if (myLocationOverlay != null) {
            return myLocationOverlay.getMyLocation();
        }
        return null;
    }
    
    /**
     * Center map on location
     */
    public void centerMapOn(GeoPoint location) {
        if (osmMapController != null && location != null) {
            osmMapController.animateTo(location);
        }
    }
    
    /**
     * Set map zoom level
     */
    public void setZoom(double zoomLevel) {
        if (osmMapController != null) {
            osmMapController.setZoom(zoomLevel);
        }
    }
    
    /**
     * Add start location marker
     */
    public void addStartMarker(GeoPoint point) {
        Log.d(TAG, "Adding start marker at: " + point.getLatitude() + ", " + point.getLongitude());
        
        // Remove existing start marker if present
        if (startMarker != null) {
            mapView.getOverlays().remove(startMarker);
        }
        
        // Create new start location marker
        startMarker = new Marker(mapView);
        startMarker.setPosition(point);
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        startMarker.setTitle("📍 Start Location");
        startMarker.setSnippet("Your starting point");
        
        // Set marker icon (green marker for start)
        startMarker.setIcon(ContextCompat.getDrawable(context, android.R.drawable.ic_menu_compass));
        
        // Add marker to map
        mapView.getOverlays().add(startMarker);
        mapView.invalidate();
    }
    
    /**
     * Add destination marker
     */
    public void addDestinationMarker(GeoPoint point) {
        Log.d(TAG, "Adding destination marker at: " + point.getLatitude() + ", " + point.getLongitude());
        
        // Remove existing end marker if present
        if (endMarker != null) {
            mapView.getOverlays().remove(endMarker);
        }
        
        // Create new destination marker
        endMarker = new Marker(mapView);
        endMarker.setPosition(point);
        endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        endMarker.setTitle("🎯 Destination");
        endMarker.setSnippet("Your destination");
        
        // Set marker icon (red marker for destination)
        endMarker.setIcon(ContextCompat.getDrawable(context, android.R.drawable.ic_menu_myplaces));
        
        // Add marker to map
        mapView.getOverlays().add(endMarker);
        mapView.invalidate();
    }
    
    /**
     * Draw route between start and destination points using OSRM API
     */
    public void drawRouteBetweenPoints(GeoPoint startPoint, GeoPoint endPoint) {
        drawRouteBetweenPoints(startPoint, endPoint, null);
    }
    
    /**
     * Draw route between start and destination points using OSRM API with callback
     */
    public void drawRouteBetweenPoints(GeoPoint startPoint, GeoPoint endPoint, RoutingCallback externalCallback) {
        Log.d(TAG, "Drawing route from " + startPoint + " to " + endPoint);
        
        // Clear existing route
        clearRoute();
        
        if (startPoint == null || endPoint == null) {
            Log.w(TAG, "Cannot draw route: start or end point is null");
            if (externalCallback != null) {
                externalCallback.onRoutingError("Start or end point is null");
            }
            return;
        }
        
        // Get real route from OSRM API
        getRouteFromOSRM(startPoint, endPoint, new RoutingCallback() {
            @Override
            public void onRouteFound(List<GeoPoint> routePoints, double distance, double duration) {
                mainHandler.post(() -> {
                    displayRealRoute(routePoints, distance, duration);
                    
                    // Notify external callback with real distance and duration
                    if (externalCallback != null) {
                        externalCallback.onRouteFound(routePoints, distance, duration);
                    }
                });
            }
            
            @Override
            public void onRoutingError(String error) {
                Log.w(TAG, "OSRM routing failed: " + error + ". Using fallback route.");
                mainHandler.post(() -> {
                    // Fallback to simple route if API fails
                    List<GeoPoint> fallbackRoute = createSimpleRoute(startPoint, endPoint);
                    displayRealRoute(fallbackRoute, -1, -1);
                    
                    // Notify external callback about the error
                    if (externalCallback != null) {
                        externalCallback.onRoutingError(error);
                    }
                });
            }
        });
    }
    
    /**
     * Get route from OSRM routing service
     */
    private void getRouteFromOSRM(GeoPoint start, GeoPoint end, RoutingCallback callback) {
        executorService.execute(() -> {
            try {
                // Build OSRM URL
                String urlString = OSRM_BASE_URL + 
                    start.getLongitude() + "," + start.getLatitude() + ";" +
                    end.getLongitude() + "," + end.getLatitude() +
                    "?overview=full&geometries=geojson&steps=true";
                
                Log.d(TAG, "OSRM URL: " + urlString);
                
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(15000);
                connection.setRequestProperty("User-Agent", "MakiTaxi/1.0");
                
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    // Parse OSRM response
                    parseOSRMResponse(response.toString(), callback);
                    
                } else {
                    callback.onRoutingError("HTTP error: " + responseCode);
                }
                
                connection.disconnect();
                
            } catch (Exception e) {
                Log.e(TAG, "Error getting route from OSRM", e);
                callback.onRoutingError("Network error: " + e.getMessage());
            }
        });
    }
    
    /**
     * Parse OSRM API response and extract route points
     */
    private void parseOSRMResponse(String jsonResponse, RoutingCallback callback) {
        try {
            JSONObject response = new JSONObject(jsonResponse);
            String code = response.getString("code");
            
            if (!"Ok".equals(code)) {
                callback.onRoutingError("OSRM returned: " + code);
                return;
            }
            
            JSONArray routes = response.getJSONArray("routes");
            if (routes.length() == 0) {
                callback.onRoutingError("No routes found");
                return;
            }
            
            JSONObject route = routes.getJSONObject(0);
            JSONObject geometry = route.getJSONObject("geometry");
            JSONArray coordinates = geometry.getJSONArray("coordinates");
            
            // Extract distance and duration
            double distance = route.getDouble("distance") / 1000.0; // Convert to km
            double duration = route.getDouble("duration") / 60.0; // Convert to minutes
            
            // Convert coordinates to GeoPoints
            List<GeoPoint> routePoints = new ArrayList<>();
            for (int i = 0; i < coordinates.length(); i++) {
                JSONArray coord = coordinates.getJSONArray(i);
                double lon = coord.getDouble(0);
                double lat = coord.getDouble(1);
                routePoints.add(new GeoPoint(lat, lon));
            }
            
            Log.d(TAG, "OSRM route parsed: " + routePoints.size() + " points, " + 
                  String.format("%.1f km, %.1f min", distance, duration));
            
            callback.onRouteFound(routePoints, distance, duration);
            
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing OSRM response", e);
            callback.onRoutingError("Failed to parse route data");
        }
    }
    
    /**
     * Display the actual route with proper styling
     */
    private void displayRealRoute(List<GeoPoint> routePoints, double distance, double duration) {
        Log.d(TAG, "Displaying real route with " + routePoints.size() + " points");
        
        if (routePoints == null || routePoints.isEmpty()) {
            Log.w(TAG, "Route points are null or empty");
            return;
        }
        
        // Create route polyline with better styling
        routePolyline = new Polyline();
        routePolyline.setPoints(routePoints);
        routePolyline.setColor(Color.parseColor("#2196F3")); // Material Blue
        routePolyline.setWidth(10.0f); // Slightly thicker for better visibility
        routePolyline.setGeodesic(false); // Use the actual route points, not geodesic
        
        // Add polyline to map
        mapView.getOverlays().add(routePolyline);
        
        // Auto-zoom to show the entire route
        zoomToShowRoute(routePoints);
        
        // Refresh map display
        mapView.invalidate();
        
        if (distance > 0 && duration > 0) {
            Log.d(TAG, String.format("Route displayed: %.1f km, %.1f minutes", distance, duration));
        } else {
            Log.d(TAG, "Route displayed (fallback mode)");
        }
    }
    
    /**
     * Zoom map to show the entire route
     */
    private void zoomToShowRoute(List<GeoPoint> routePoints) {
        try {
            if (routePoints.size() < 2) return;
            
            // Calculate bounding box for all route points
            double minLat = routePoints.get(0).getLatitude();
            double maxLat = routePoints.get(0).getLatitude();
            double minLon = routePoints.get(0).getLongitude();
            double maxLon = routePoints.get(0).getLongitude();
            
            for (GeoPoint point : routePoints) {
                minLat = Math.min(minLat, point.getLatitude());
                maxLat = Math.max(maxLat, point.getLatitude());
                minLon = Math.min(minLon, point.getLongitude());
                maxLon = Math.max(maxLon, point.getLongitude());
            }
            
            // Add padding (15% of the range)
            double latPadding = (maxLat - minLat) * 0.15;
            double lonPadding = (maxLon - minLon) * 0.15;
            
            org.osmdroid.util.BoundingBox boundingBox = new org.osmdroid.util.BoundingBox(
                maxLat + latPadding, maxLon + lonPadding,
                minLat - latPadding, minLon - lonPadding
            );
            
            // Zoom to bounding box with animation
            mapView.zoomToBoundingBox(boundingBox, true, 100);
            
        } catch (Exception e) {
            Log.w(TAG, "Error zooming to show route", e);
            // Fallback: center on first point
            if (!routePoints.isEmpty()) {
                centerMapOn(routePoints.get(0));
            }
        }
    }
    
    /**
     * Create a simple route between two points
     * TODO: Replace with real routing API call
     */
    private List<GeoPoint> createSimpleRoute(GeoPoint start, GeoPoint end) {
        List<GeoPoint> points = new java.util.ArrayList<>();
        
        // Add start point
        points.add(start);
        
        // Add some intermediate points for a more realistic route
        double latDiff = end.getLatitude() - start.getLatitude();
        double lonDiff = end.getLongitude() - start.getLongitude();
        
        // Add 3 intermediate points to make the route look more natural
        for (int i = 1; i <= 3; i++) {
            double ratio = i / 4.0;
            double intermediateLat = start.getLatitude() + (latDiff * ratio);
            double intermediateLon = start.getLongitude() + (lonDiff * ratio);
            points.add(new GeoPoint(intermediateLat, intermediateLon));
        }
        
        // Add end point
        points.add(end);
        
        return points;
    }
    
    /**
     * Clear route from map
     */
    public void clearRoute() {
        try {
            if (routePolyline != null) {
                mapView.getOverlays().remove(routePolyline);
                routePolyline = null;
            }
        } catch (Exception e) {
            Log.w(TAG, "Error clearing route", e);
        }
    }
    
    /**
     * Clear all markers
     */
    public void clearMarkers() {
        try {
            if (startMarker != null) {
                mapView.getOverlays().remove(startMarker);
                startMarker = null;
            }
            
            if (endMarker != null) {
                mapView.getOverlays().remove(endMarker);
                endMarker = null;
            }
            
            mapView.invalidate();
        } catch (Exception e) {
            Log.w(TAG, "Error clearing markers", e);
        }
    }
    
    /**
     * Clear everything from map
     */
    public void clearAll() {
        clearRoute();
        clearMarkers();
    }
    
    /**
     * Refresh map display
     */
    public void refreshMap() {
        mapView.invalidate();
    }
    
    // MapEventsReceiver implementation
    @Override
    public boolean singleTapConfirmedHelper(GeoPoint p) {
        Log.d(TAG, "Map tapped at: " + p.getLatitude() + ", " + p.getLongitude());
        
        if (mapInteractionListener != null) {
            mapInteractionListener.onMapTap(p);
        }
        
        return false; // Let the listener handle the event
    }
    
    @Override
    public boolean longPressHelper(GeoPoint p) {
        // Handle long press if needed
        return false;
    }
    
    // Lifecycle methods
    public void onResume() {
        mapView.onResume();
        
        if (myLocationOverlay != null) {
            myLocationOverlay.enableMyLocation();
        }
    }
    
    public void onPause() {
        mapView.onPause();
        
        if (myLocationOverlay != null) {
            myLocationOverlay.disableMyLocation();
        }
    }
    
    public void onDestroy() {
        Log.d(TAG, "MapController onDestroy");
        
        if (myLocationOverlay != null) {
            myLocationOverlay.disableMyLocation();
            myLocationOverlay.disableFollowLocation();
        }
        
        // Clean up executor service
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        
        // Clear all overlays
        clearAll();
    }
} 