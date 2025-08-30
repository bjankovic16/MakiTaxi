package com.makitaxi.menu;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.makitaxi.R;
import com.makitaxi.config.AppConfig;
import com.makitaxi.model.FeedbackRequest;
import com.makitaxi.model.RideRequest;
import com.makitaxi.utils.FirebaseHelper;
import com.makitaxi.utils.ToastUtils;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.List;

public class RideHistoryMapActivity extends AppCompatActivity {

    private static final String TAG = "RideHistoryMapActivity";

    private MapView mapView;
    private ImageButton btnBack;
    private TextView txtTitle;
    private Button btnToggleRoutes;
    private Button btnToggleMarkers;
    private Button btnZoomIn;
    private Button btnZoomOut;
    private LinearLayout loadingContainer;
    private TextView txtLoading;

    private List<FeedbackRequest> rideHistory;
    private List<RideRequest> rideRequests;
    private List<Polyline> routeLines;
    private List<Marker> markers;

    private boolean showRoutes = true;
    private boolean showMarkers = true;
    
    private static final String OSRM_BASE_URL = "https://router.project-osrm.org/route/v1/driving/";
    private static final double MIN_ZOOM = 10.0;
    private static final double MAX_ZOOM = 19.0;
    private ExecutorService executorService;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ride_history_map_activity);

        initializeOSMDroid();
        initializeViews();
        setupUIInteractions();
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        loadRideData();
    }

    private void initializeOSMDroid() {
        try {
            Configuration.getInstance().setUserAgentValue(getPackageName());
            Configuration.getInstance().setOsmdroidBasePath(getCacheDir());
            Configuration.getInstance().setOsmdroidTileCache(getCacheDir());
            Log.d(TAG, "OSMDroid initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing OSMDroid", e);
        }
    }

    private void initializeViews() {
        mapView = findViewById(R.id.mapView);
        btnBack = findViewById(R.id.btnBack);
        txtTitle = findViewById(R.id.txtTitle);
        btnToggleRoutes = findViewById(R.id.btnToggleRoutes);
        btnToggleMarkers = findViewById(R.id.btnToggleMarkers);
        btnZoomIn = findViewById(R.id.btnZoomIn);
        btnZoomOut = findViewById(R.id.btnZoomOut);
        loadingContainer = findViewById(R.id.loadingContainer);
        txtLoading = findViewById(R.id.txtLoading);

        routeLines = new ArrayList<>();
        markers = new ArrayList<>();

        setupMapView();
    }

    private void setupMapView() {
        try {
            mapView.setTileSource(TileSourceFactory.MAPNIK);
            mapView.setMultiTouchControls(true);
            mapView.getZoomController().setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER);
            mapView.setMinZoomLevel(MIN_ZOOM);
            mapView.setMaxZoomLevel(MAX_ZOOM);

            IMapController mapController = mapView.getController();
            mapController.setZoom(12.0);

            Log.d(TAG, "MapView setup completed");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up MapView", e);
            ToastUtils.showError(this, "Error setting up map: " + e.getMessage());
        }
    }

    private void setupUIInteractions() {
        btnBack.setOnClickListener(v -> finish());
        
        btnToggleRoutes.setOnClickListener(v -> {
            showRoutes = !showRoutes;
            updateRouteVisibility();
            btnToggleRoutes.setText(showRoutes ? "Hide Routes" : "Show Routes");
        });
        
        btnToggleMarkers.setOnClickListener(v -> {
            showMarkers = !showMarkers;
            updateMarkerVisibility();
            btnToggleMarkers.setText(showMarkers ? "Hide Markers" : "Show Markers");
        });
        
        btnZoomIn.setOnClickListener(v -> zoomIn());
        btnZoomOut.setOnClickListener(v -> zoomOut());
    }

    private void loadRideData() {
        rideHistory = (List<FeedbackRequest>) getIntent().getSerializableExtra("rideHistory");
        if (rideHistory == null || rideHistory.isEmpty()) {
            ToastUtils.showInfo(this, "No ride history to display");
            finish();
            return;
        }

        txtTitle.setText("Ride History Map (" + rideHistory.size() + " rides)");
        loadRideRequests();
    }

    private void loadRideRequests() {
        List<String> rideRequestIds = new ArrayList<>();
        for (FeedbackRequest feedback : rideHistory) {
            rideRequestIds.add(feedback.getRideRequestId());
        }

        FirebaseHelper.getRideRequestsRef().addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot dataSnapshot) {
                rideRequests = new ArrayList<>();
                for (com.google.firebase.database.DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    RideRequest rideRequest = snapshot.getValue(RideRequest.class);
                    if (rideRequest != null && rideRequestIds.contains(rideRequest.getRequestId())) {
                        rideRequests.add(rideRequest);
                    }
                }
                displayRidesOnMap();
            }

            @Override
            public void onCancelled(com.google.firebase.database.DatabaseError databaseError) {
                Log.e(TAG, "Error loading ride requests: " + databaseError.getMessage());
                ToastUtils.showError(RideHistoryMapActivity.this, "Error loading ride data");
            }
        });
    }

    private void displayRidesOnMap() {
        if (rideRequests.isEmpty()) {
            ToastUtils.showInfo(this, "No ride data available for map display");
            return;
        }

        clearMap();
        showLoadingProgress();
        addRidesToMap();
        centerMapOnRides();
    }

    private void clearMap() {
        for (Polyline line : routeLines) {
            mapView.getOverlays().remove(line);
        }
        routeLines.clear();

        for (Marker marker : markers) {
            mapView.getOverlays().remove(marker);
        }
        markers.clear();
    }

    private void addRidesToMap() {
        final int totalRides = rideRequests.size();
        final int[] completedRides = {0};
        
        for (int i = 0; i < rideRequests.size(); i++) {
            RideRequest rideRequest = rideRequests.get(i);
            addRideToMap(rideRequest, i, () -> {
                completedRides[0]++;
                updateLoadingProgress(completedRides[0], totalRides);
                
                if (completedRides[0] >= totalRides) {
                    hideLoadingProgress();
                }
            });
        }
    }

    private void addRideToMap(RideRequest rideRequest, int index, Runnable onComplete) {
        try {
            GeoPoint pickupPoint = new GeoPoint(rideRequest.getPickupLatitude(), rideRequest.getPickupLongitude());
            GeoPoint dropoffPoint = new GeoPoint(rideRequest.getDropoffLatitude(), rideRequest.getDropoffLongitude());

            if (showMarkers) {
                addMarker(pickupPoint, "Pickup", index, true);
                addMarker(dropoffPoint, "Dropoff", index, false);
            }

            if (showRoutes) {
                getRouteFromOSRM(pickupPoint, dropoffPoint, index, onComplete);
            } else {
                onComplete.run();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error adding ride to map: " + e.getMessage());
            onComplete.run();
        }
    }

    private void addMarker(GeoPoint point, String type, int index, boolean isPickup) {
        try {
            Marker marker = new Marker(mapView);
            marker.setPosition(point);
            marker.setTitle(type + " #" + (index + 1));
            
            String snippet = isPickup ? 
                "Pickup: " + rideRequests.get(index).getPickupAddress() :
                "Dropoff: " + rideRequests.get(index).getDropoffAddress();
            marker.setSnippet(snippet);

            Drawable iconDrawable = ContextCompat.getDrawable(this, 
                isPickup ? R.drawable.ic_location_green : R.drawable.ic_location_red);
            if (iconDrawable != null) {
                marker.setIcon(iconDrawable);
            }

            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            mapView.getOverlays().add(marker);
            markers.add(marker);

        } catch (Exception e) {
            Log.e(TAG, "Error adding marker: " + e.getMessage());
        }
    }

    private void getRouteFromOSRM(GeoPoint start, GeoPoint end, int index, Runnable onComplete) {
        if (start == null || end == null) {
            return;
        }

        String url = OSRM_BASE_URL + start.getLongitude() + "," + start.getLatitude() + ";" +
                end.getLongitude() + "," + end.getLatitude() + "?overview=full&geometries=geojson";

        executorService.execute(() -> {
            try {
                URL urlObj = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    if (jsonResponse.has("routes") && jsonResponse.getJSONArray("routes").length() > 0) {
                        JSONObject route = jsonResponse.getJSONArray("routes").getJSONObject(0);
                        JSONObject geometry = route.getJSONObject("geometry");
                        JSONArray coordinates = geometry.getJSONArray("coordinates");

                        List<GeoPoint> routePoints = new ArrayList<>();
                        for (int i = 0; i < coordinates.length(); i++) {
                            JSONArray coord = coordinates.getJSONArray(i);
                            double lon = coord.getDouble(0);
                            double lat = coord.getDouble(1);
                            routePoints.add(new GeoPoint(lat, lon));
                        }

                        mainHandler.post(() -> {
                            addRealRoute(routePoints, index);
                            onComplete.run();
                        });
                    } else {
                        Log.e(TAG, "No route found for ride " + index);
                        onComplete.run();
                    }
                } else {
                    Log.e(TAG, "HTTP Error: " + responseCode + " for ride " + index);
                    onComplete.run();
                }
                connection.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error getting route for ride " + index + ": " + e.getMessage());
                onComplete.run();
            }
        });
    }

    private void addRealRoute(List<GeoPoint> routePoints, int index) {
        try {
            if (routePoints == null || routePoints.isEmpty()) {
                return;
            }

            Polyline routeLine = new Polyline();
            routeLine.setPoints(routePoints);
            
            int color = getRouteColor(index);
            routeLine.setColor(color);
            routeLine.setWidth(8.0f);
            routeLine.setGeodesic(false);
            
            mapView.getOverlays().add(routeLine);
            routeLines.add(routeLine);
            mapView.invalidate();

        } catch (Exception e) {
            Log.e(TAG, "Error adding real route: " + e.getMessage());
        }
    }

    private int getRouteColor(int index) {
        int[] colors = {
            0xFF2196F3, // Blue
            0xFF4CAF50, // Green
            0xFFFF9800, // Orange
            0xFF9C27B0, // Purple
            0xFFF44336, // Red
            0xFF00BCD4, // Cyan
            0xFF795548, // Brown
            0xFF607D8B  // Blue Grey
        };
        return colors[index % colors.length];
    }

    private void centerMapOnRides() {
        if (rideRequests.isEmpty()) return;

        try {
            double minLat = Double.MAX_VALUE;
            double maxLat = Double.MIN_VALUE;
            double minLon = Double.MAX_VALUE;
            double maxLon = Double.MIN_VALUE;

            for (RideRequest rideRequest : rideRequests) {
                minLat = Math.min(minLat, rideRequest.getPickupLatitude());
                maxLat = Math.max(maxLat, rideRequest.getPickupLatitude());
                minLon = Math.min(minLon, rideRequest.getPickupLongitude());
                maxLon = Math.max(maxLon, rideRequest.getPickupLongitude());

                minLat = Math.min(minLat, rideRequest.getDropoffLatitude());
                maxLat = Math.max(maxLat, rideRequest.getDropoffLatitude());
                minLon = Math.min(minLon, rideRequest.getDropoffLongitude());
                maxLon = Math.max(maxLon, rideRequest.getDropoffLongitude());
            }

            GeoPoint center = new GeoPoint((minLat + maxLat) / 2, (minLon + maxLon) / 2);
            mapView.getController().setCenter(center);

            double latSpan = maxLat - minLat;
            double lonSpan = maxLon - minLon;
            double maxSpan = Math.max(latSpan, lonSpan);

            if (maxSpan > 0) {
                double zoomLevel = Math.min(18.0, Math.max(10.0, 15.0 - Math.log(maxSpan * 100) / Math.log(2)));
                mapView.getController().setZoom(zoomLevel);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error centering map: " + e.getMessage());
        }
    }

    private void updateRouteVisibility() {
        for (Polyline line : routeLines) {
            line.setVisible(showRoutes);
        }
        mapView.invalidate();
    }

    private void updateMarkerVisibility() {
        for (Marker marker : markers) {
            marker.setVisible(showMarkers);
        }
        mapView.invalidate();
    }

    private void showLoadingProgress() {
        mainHandler.post(() -> {
            loadingContainer.setVisibility(View.VISIBLE);
            txtLoading.setText("Loading routes...");
        });
    }

    private void updateLoadingProgress(int completed, int total) {
        mainHandler.post(() -> {
            txtLoading.setText("Loading routes... " + completed + "/" + total);
        });
    }

    private void hideLoadingProgress() {
        mainHandler.post(() -> {
            loadingContainer.setVisibility(View.GONE);
        });
    }

    private void zoomIn() {
        double current = mapView.getZoomLevelDouble();
        double target = Math.min(MAX_ZOOM, current + 1.0);
        if (target > current) {
            mapView.getController().setZoom(target);
        }
    }

    private void zoomOut() {
        double current = mapView.getZoomLevelDouble();
        double target = Math.max(MIN_ZOOM, current - 1.0);
        if (target < current) {
            mapView.getController().setZoom(target);
        }
    }



    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapView != null) {
            mapView.onDetach();
        }
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
