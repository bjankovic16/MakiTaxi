package com.makitaxi.passenger;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.makitaxi.R;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

public class PassengerScreen extends AppCompatActivity implements Map.CallbackMapTap {

    // UI Components
    private MapView mapView;
    private EditText txtPickupLocation;
    private EditText txtDestination;
    private TextView toggleControls;
    private ImageButton btnHamburgerMenu;
    private Button btnChoseFromMap;
    private Button btnChoseCurrentLocation;
    private Button btnShowRoute;
    private Button btnClearRoute;
    private Button btnZoomIn;
    private Button btnZoomOut;
    private ImageButton btnFilter;
    private ImageButton btnMyLocation;
    private LinearLayout pickupLocationContainer;
    private LinearLayout destinationLocationContainer;

    private boolean hasFocusPickup = false;
    private boolean hasFocusDestination = false;

    private boolean shouldCalculateStartOrDestinationFromMap;

    private Map map;

    private LocationService locationService;
    private boolean controlsVisible = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.passenger_screen);
        handleSystemBars();
        initializeViews();
        initializeControllers();
        setupUIInteractions();
    }

    private void initializeControllers() {
        map = new Map(this, mapView);
        locationService = new LocationService(this);
        map.initCallbackMapTap(this);
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
        // Map
        mapView = findViewById(R.id.mapView);

        // Header components
        btnHamburgerMenu = findViewById(R.id.btnHamburgerMenu);
        toggleControls = findViewById(R.id.toggleControls);

        // Location input containers
        pickupLocationContainer = findViewById(R.id.pickupLocationContainer);
        destinationLocationContainer = findViewById(R.id.destinationLocationContainer);

        // Location text views
        txtPickupLocation = findViewById(R.id.txtPickupLocation);
        txtDestination = findViewById(R.id.txtDestination);

        // Side buttons
        btnChoseFromMap = findViewById(R.id.btnChoseFromMap);
        btnChoseCurrentLocation = findViewById(R.id.btnChoseCurrentLocation);

        // Bottom action buttons
        btnShowRoute = findViewById(R.id.btnShowRoute);
        btnClearRoute = findViewById(R.id.btnClearRoute);

        // Map control buttons
        btnFilter = findViewById(R.id.btnFilter);
        btnZoomIn = findViewById(R.id.btnZoomIn);
        btnZoomOut = findViewById(R.id.btnZoomOut);
        btnMyLocation = findViewById(R.id.btnMyLocation);
    }

    private void setupUIInteractions() {
        toggleControls.setOnClickListener(v -> toggleControls());

        txtPickupLocation.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                btnChoseFromMap.setVisibility(View.VISIBLE);
                btnChoseCurrentLocation.setVisibility(View.VISIBLE);
            }
            hasFocusPickup = hasFocus;
        });

        txtDestination.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                btnChoseFromMap.setVisibility(View.VISIBLE);
                btnChoseCurrentLocation.setVisibility(View.VISIBLE);
            }
            hasFocusDestination = hasFocus;
        });

        btnZoomIn.setOnClickListener(v -> map.zoomIn());

        btnZoomOut.setOnClickListener(v -> map.zoomOut());

        btnMyLocation.setOnClickListener(v -> map.centerOnCurrentLocation());

        btnChoseCurrentLocation.setOnClickListener(v -> choseCurrentLocationAsStartOrDestination());

        btnChoseFromMap.setOnClickListener(v -> choseLocationFromMapAsStartOrDestination());
    }

    private void choseCurrentLocationAsStartOrDestination() {
        if (!hasFocusPickup && !hasFocusDestination) return;

        locationService.reverseGeocode(map.getCurrentLocation(), new LocationService.ReverseGeocodeListener() {
            @Override
            public void onReverseGeocodeSuccess(String address) {
                if (hasFocusPickup) {
                    txtPickupLocation.setText(address);
                }
                if (hasFocusDestination) {
                    txtDestination.setText(address);
                }
            }

            @Override
            public void onReverseGeocodeError(String error) {
                if (hasFocusPickup) {
                    txtPickupLocation.setText(error);
                }
                if (hasFocusDestination) {
                    txtDestination.setText(error);
                }
            }
        });
    }

    private void choseLocationFromMapAsStartOrDestination() {
        if (!hasFocusPickup && !hasFocusDestination) return;
        shouldCalculateStartOrDestinationFromMap = true;
    }

    private void toggleControls() {
        controlsVisible = !controlsVisible;
        if (controlsVisible) {
            btnShowRoute.setVisibility(View.VISIBLE);
            btnClearRoute.setVisibility(View.VISIBLE);
            pickupLocationContainer.setVisibility(View.VISIBLE);
            destinationLocationContainer.setVisibility(View.VISIBLE);
            btnChoseFromMap.setVisibility(View.GONE);
            btnChoseCurrentLocation.setVisibility(View.GONE);
            shouldCalculateStartOrDestinationFromMap = false;
            toggleControls.setText("🚕 ▼");
        } else {
            btnShowRoute.setVisibility(View.GONE);
            btnClearRoute.setVisibility(View.GONE);
            pickupLocationContainer.setVisibility(View.GONE);
            destinationLocationContainer.setVisibility(View.GONE);
            btnChoseFromMap.setVisibility(View.GONE);
            btnChoseCurrentLocation.setVisibility(View.GONE);
            shouldCalculateStartOrDestinationFromMap = false;
            hideKeyboard();
            toggleControls.setText("🚕 ▲");
        }
    }

    private void hideKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                View currentFocus = getCurrentFocus();
                if (currentFocus != null) {
                    imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                    currentFocus.clearFocus();
                }

                View rootView = findViewById(android.R.id.content);
                if (rootView != null) {
                    imm.hideSoftInputFromWindow(rootView.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                }
            }
        } catch (Exception e) {
            Log.e("PassengerScreen", "Error hiding keyboard: " + e.getMessage());
        }
    }

    @Override
    public void onTap(GeoPoint p) {
        if (shouldCalculateStartOrDestinationFromMap) {
            locationService.reverseGeocode(p, new LocationService.ReverseGeocodeListener() {
                @Override
                public void onReverseGeocodeSuccess(String address) {
                    if (hasFocusPickup) {
                        txtPickupLocation.setText(address);
                    }
                    if (hasFocusDestination) {
                        txtDestination.setText(address);
                    }
                }

                @Override
                public void onReverseGeocodeError(String error) {
                    if (hasFocusPickup) {
                        txtPickupLocation.setText(error);
                    }
                    if (hasFocusDestination) {
                        txtDestination.setText(error);
                    }
                }
            });
        }

    }
}
