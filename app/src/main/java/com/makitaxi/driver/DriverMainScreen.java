package com.makitaxi.driver;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.makitaxi.R;
import com.makitaxi.model.RideRequest;
import com.makitaxi.utils.FirebaseHelper;

import org.osmdroid.views.MapView;

import java.util.Objects;

public class DriverMainScreen extends AppCompatActivity {

    private static final String TAG = "DriverMainScreen";

    // UI Components
    private MapView mapView;
    private MapDriver map;

    // Managers
    private DriverUIManager uiManager;
    private DriverRideManager rideManager;
    private LocationUpdateService locationUpdateService;

    // Firebase References
    private DatabaseReference rideRequestsRef;
    private String driverId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.driver_main_screen);

        handleSystemBars();
        initializeViews();
        initializeControllers();
        initializeManagers();
    }

    private void initializeControllers() {
        map = new MapDriver(this, mapView);
    }

    private void initializeManagers() {
        rideRequestsRef = FirebaseHelper.getRideRequestsRef();
        driverId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        uiManager = new DriverUIManager(this, driverId);
        uiManager.setMapDriver(map);
        rideManager = new DriverRideManager(this, driverId, uiManager);

        locationUpdateService = new LocationUpdateService(driverId, map);

        uiManager.setStatusChangeListener(new DriverUIManager.OnDriverStatusChangeListener() {
            @Override
            public void onDriverStatusChanged(boolean isOnline) {
                if (isOnline) {
                    locationUpdateService.startUpdates();
                } else {
                    locationUpdateService.stopUpdates();
                }
            }
        });

        uiManager.setRideActionListener(new DriverUIManager.OnRideActionListener() {
            @Override
            public void onRideAccepted(RideRequest request) {
                rideManager.acceptRide(request);
            }

            @Override
            public void onRideDeclined(RideRequest request) {
                rideManager.declineRide(request);
            }
        });

        uiManager.setMapInteractionListener(new DriverUIManager.OnMapInteractionListener() {
            @Override
            public void onZoomIn() {
                map.zoomIn();
            }

            @Override
            public void onZoomOut() {
                map.zoomOut();
            }

            @Override
            public void onMyLocation() {
                map.centerOnCurrentLocation();
            }
        });
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

    private void initializeViews() {
        mapView = findViewById(R.id.mapView);
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

        if (rideRequestsRef != null) {
            // Cleanup handled by managers
        }

        // Clean up map
        if (mapView != null) {
            mapView.onDetach();
        }

        // Clean up managers
        if (uiManager != null) {
            uiManager.cleanup();
        }

        // Stop services
        if (locationUpdateService != null) {
            locationUpdateService.stopUpdates();
        }
    }
}