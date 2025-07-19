package com.makitaxi.passenger;

import static com.google.android.material.internal.ViewUtils.showKeyboard;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.makitaxi.R;
import com.makitaxi.menu.MenuMainScreen;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


public class PassengerScreen extends AppCompatActivity implements Map.CallbackMapTap {

    // UI Components
    private MapView mapView;
    private AutoCompleteTextView txtPickupLocation;
    private AutoCompleteTextView txtDestination;
    private TextView toggleControls;
    private ImageButton btnHamburgerMenu;
    private Button btnChoseFromMap;
    private Button btnChoseCurrentLocation;
    private Button btnShowRoute;
    private Button btnClearRoute;
    private Button btnZoomIn;
    private Button btnZoomOut;
    private ImageButton btnFilter;

    private TextView iconCloseOverlay;

    private FrameLayout frameMapButton;
    private ImageButton btnMyLocation;
    private LinearLayout pickupLocationContainer;
    private LinearLayout destinationLocationContainer;

    private boolean hasFocusPickup = false;
    private boolean hasFocusDestination = false;

    private boolean shouldCalculateStartOrDestinationFromMap;

    private Map map;

    private LocationService locationService;
    private boolean controlsVisible = true;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final java.util.Map<AutoCompleteTextView, Runnable> debounceMap = new HashMap<>();

    private ImageView pickupLoadingSpinner;
    private ImageView destinationLoadingSpinner;
    private RotateAnimation spinnerAnimation;

    private GeoPoint pickupGeoPoint;
    private  GeoPoint destinationGeoPoint;

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
        iconCloseOverlay = findViewById(R.id.iconCloseOverlay);
        frameMapButton = findViewById(R.id.frameMapButton);

        // Bottom action buttons
        btnShowRoute = findViewById(R.id.btnShowRoute);
        btnClearRoute = findViewById(R.id.btnClearRoute);

        // Map control buttons
        btnFilter = findViewById(R.id.btnFilter);
        btnZoomIn = findViewById(R.id.btnZoomIn);
        btnZoomOut = findViewById(R.id.btnZoomOut);
        btnMyLocation = findViewById(R.id.btnMyLocation);

        pickupLoadingSpinner = findViewById(R.id.pickupLoadingSpinner);
        destinationLoadingSpinner = findViewById(R.id.destinationLoadingSpinner);
    }

    private void setupUIInteractions() {
        spinnerAnimation = new RotateAnimation(0f, 360f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        spinnerAnimation.setDuration(900);
        spinnerAnimation.setRepeatCount(Animation.INFINITE);
        spinnerAnimation.setInterpolator(new LinearInterpolator());

        toggleControls.setOnClickListener(v -> toggleControls());

        txtPickupLocation.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                btnChoseFromMap.setVisibility(View.VISIBLE);
                btnChoseCurrentLocation.setVisibility(View.VISIBLE);
                iconCloseOverlay.setVisibility(View.VISIBLE);
                frameMapButton.setVisibility(View.VISIBLE);
            }
            hasFocusPickup = hasFocus;
        });

        txtDestination.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                btnChoseFromMap.setVisibility(View.VISIBLE);
                btnChoseCurrentLocation.setVisibility(View.VISIBLE);
                iconCloseOverlay.setVisibility(View.VISIBLE);
                frameMapButton.setVisibility(View.VISIBLE);
            }
            hasFocusDestination = hasFocus;
        });

        pickupLocationContainer.setOnClickListener(v -> {
            txtPickupLocation.requestFocus();
        });

        destinationLocationContainer.setOnClickListener(v -> {
            txtDestination.requestFocus();
        });

        btnZoomIn.setOnClickListener(v -> map.zoomIn());

        btnZoomOut.setOnClickListener(v -> map.zoomOut());

        btnMyLocation.setOnClickListener(v -> map.centerOnCurrentLocation());

        btnChoseCurrentLocation.setOnClickListener(v -> choseCurrentLocationAsStartOrDestination());

        btnChoseFromMap.setOnClickListener(v -> choseLocationFromMapAsStartOrDestination());

        btnShowRoute.setOnClickListener(v -> handleShowRoute());

        btnClearRoute.setOnClickListener(v -> map.clearMap());

        btnHamburgerMenu.setOnClickListener(v -> openHamburgerMenu());

        setupDebouncedAutocomplete(txtPickupLocation, true);
        setupDebouncedAutocomplete(txtDestination, false);
    }

    private void handleShowRoute() {
        String pickupLocation = txtPickupLocation.getText().toString().trim();
        String destinationLocation = txtDestination.getText().toString().trim();

        if (pickupLocation.isEmpty() || destinationLocation.isEmpty()) {
            Toast.makeText(this, "❌ Please enter both pickup and destination locations", Toast.LENGTH_SHORT).show();
            return;
        }
        map.clearMarkers();
        AtomicInteger pendingGeocodeCount = new AtomicInteger();

        if (pickupGeoPoint != null && destinationGeoPoint != null) {
            drawRoute();
            return;
        }

        if(pickupGeoPoint == null) {
            pendingGeocodeCount.getAndIncrement();
            geoCodeAddress(pickupLocation, true, () -> {
                pendingGeocodeCount.getAndDecrement();
                checkAndDrawRoute(pendingGeocodeCount.get());
            });
        }

        if(destinationGeoPoint == null) {
            pendingGeocodeCount.getAndIncrement();
            geoCodeAddress(destinationLocation, false, () -> {
                pendingGeocodeCount.getAndDecrement();
                checkAndDrawRoute(pendingGeocodeCount.get());
            });
        }
    }

    private void checkAndDrawRoute(int remainingGeocodeCount) {
        if (remainingGeocodeCount == 0) {
            if (pickupGeoPoint != null && destinationGeoPoint != null) {
                drawRoute();
            } else {
                Toast.makeText(this, "❌ Could not find one or both locations", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void drawRoute() {

        map.drawRouteBetweenPoints(pickupGeoPoint, destinationGeoPoint, new Map.RoutingCallback() {
            @Override
            public void onRouteFound(List<GeoPoint> routePoints, double distanceKm, double durationMinutes) {
                map.clearMarkerTap();
                runOnUiThread(() -> {
                    Toast.makeText(PassengerScreen.this, 
                        String.format("✅ Route found: %.1f km, %.0f min", distanceKm, durationMinutes), 
                        Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onRoutingError(String error) {
                map.clearMarkerTap();
                runOnUiThread(() -> {
                    Toast.makeText(PassengerScreen.this, "❌ Route calculation failed: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void geoCodeAddress(String address, boolean isPickup, Runnable onComplete) {
        locationService.geocode(address, new LocationService.GeocodeListener() {
            @Override
            public void onGeocodeSuccess(GeoPoint geoPoint) {
                runOnUiThread(() -> {
                    if (isPickup) {
                        pickupGeoPoint = geoPoint;
                    } else {
                        destinationGeoPoint = geoPoint;
                    }
                    
                    if (onComplete != null) {
                        onComplete.run();
                    }
                });
            }

            @Override
            public void onGeocodeError(String error) {
                runOnUiThread(() -> {
                    if(isPickup) {
                        Toast.makeText(PassengerScreen.this, "❌ Error finding pickup location: " + error, Toast.LENGTH_SHORT).show();
                        pickupGeoPoint = null;
                    } else {
                        Toast.makeText(PassengerScreen.this, "❌ Error finding destination location: " + error, Toast.LENGTH_SHORT).show();
                        destinationGeoPoint = null;
                    }
                    
                    if (onComplete != null) {
                        onComplete.run();
                    }
                });
            }
        });
    }

    private void setupDebouncedAutocomplete(AutoCompleteTextView field, boolean isPickup) {
        field.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Runnable previous = debounceMap.get(field);
                if (previous != null) {
                    handler.removeCallbacks(previous);
                }

                String input = s.toString();

                Runnable task = () -> {
                    boolean containsSpecialChars = input.matches(".*[\\p{Punct}\\d].*");

                    if (containsSpecialChars) {
                        runOnUiThread(() -> {
                            if (isPickup) showPickupSpinner(false);
                            else showDestinationSpinner(false);
                        });
                        return;
                    }

                    locationService.getPhotonSuggestions(input, new LocationService.LocationSuggestionsListener() {
                        @Override
                        public void onSuggestionsFound(List<String> suggestions) {
                            runOnUiThread(() -> {
                                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                        field.getContext(),
                                        R.layout.drop_down_item,
                                        suggestions
                                );
                                field.setAdapter(adapter);
                                if(isPickup) {
                                    pickupGeoPoint = null;
                                } else {
                                    destinationGeoPoint = null;
                                }
                                if (!suggestions.isEmpty()) {
                                    field.showDropDown();
                                }

                                if (isPickup) showPickupSpinner(false);
                                else showDestinationSpinner(false);
                            });
                        }

                        @Override
                        public void onSuggestionsFoundError(String error) {
                            Log.e("PassengerScreen", "Error getting suggestions: " + error);
                            runOnUiThread(() -> {
                                if (isPickup) showPickupSpinner(false);
                                else showDestinationSpinner(false);
                            });
                        }
                    });
                };

                debounceMap.put(field, task);

                if (isPickup) showPickupSpinner(true);
                else showDestinationSpinner(true);

                handler.postDelayed(task, 800);
            }

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void choseCurrentLocationAsStartOrDestination() {
        if (!hasFocusPickup && !hasFocusDestination) return;
        GeoPoint currentLocation = map.getCurrentLocation();

        locationService.reverseGeocode(currentLocation, new LocationService.ReverseGeocodeListener() {
            @Override
            public void onReverseGeocodeSuccess(String address) {
                if (hasFocusPickup) {
                    txtPickupLocation.setText(address);
                    pickupGeoPoint = currentLocation;
                } else {
                    txtDestination.setText(address);
                    destinationGeoPoint = currentLocation;
                }
            }

            @Override
            public void onReverseGeocodeError(String error) {
                if (hasFocusPickup) {
                    txtPickupLocation.setText(error);
                }else {
                    txtDestination.setText(error);
                }
            }
        });
    }

    private void choseLocationFromMapAsStartOrDestination() {
        if (!hasFocusPickup && !hasFocusDestination) return;
        shouldCalculateStartOrDestinationFromMap = !shouldCalculateStartOrDestinationFromMap;
        if (shouldCalculateStartOrDestinationFromMap) {
            iconCloseOverlay.setText("");
        } else {
            iconCloseOverlay.setText("❌");
        }
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
            iconCloseOverlay.setVisibility(View.GONE);
            frameMapButton.setVisibility(View.GONE);
            shouldCalculateStartOrDestinationFromMap = false;
            map.clearMarkerTap();
            toggleControls.setText("🚕 ▼");
            iconCloseOverlay.setText("❌");
        } else {
            btnShowRoute.setVisibility(View.GONE);
            btnClearRoute.setVisibility(View.GONE);
            pickupLocationContainer.setVisibility(View.GONE);
            destinationLocationContainer.setVisibility(View.GONE);
            btnChoseFromMap.setVisibility(View.GONE);
            iconCloseOverlay.setVisibility(View.GONE);
            btnChoseCurrentLocation.setVisibility(View.GONE);
            frameMapButton.setVisibility(View.GONE);
            shouldCalculateStartOrDestinationFromMap = false;
            map.clearMarkerTap();
            hideKeyboard();
            toggleControls.setText("🚕 ▲");
            iconCloseOverlay.setText("❌");
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
            map.setMapTapMarker(p);
            locationService.reverseGeocode(p, new LocationService.ReverseGeocodeListener() {
                @Override
                public void onReverseGeocodeSuccess(String address) {
                    if (hasFocusPickup) {
                        pickupGeoPoint = p;
                        txtPickupLocation.setText(address);
                    }
                    if (hasFocusDestination) {
                        destinationGeoPoint = p;
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

    private void showPickupSpinner(boolean show) {
        if (pickupLoadingSpinner != null) {
            if (show) {
                pickupLoadingSpinner.setVisibility(View.VISIBLE);
                pickupLoadingSpinner.startAnimation(spinnerAnimation);
            } else {
                pickupLoadingSpinner.clearAnimation();
                pickupLoadingSpinner.setVisibility(View.GONE);
            }
        }
    }

    private void showDestinationSpinner(boolean show) {
        if (destinationLoadingSpinner != null) {
            if (show) {
                destinationLoadingSpinner.setVisibility(View.VISIBLE);
                destinationLoadingSpinner.startAnimation(spinnerAnimation);
            } else {
                destinationLoadingSpinner.clearAnimation();
                destinationLoadingSpinner.setVisibility(View.GONE);
            }
        }
    }

    private void openHamburgerMenu() {
        Intent intent = new Intent(this, MenuMainScreen.class);
        startActivity(intent);

        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
