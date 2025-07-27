package com.makitaxi.driver;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.makitaxi.R;
import com.makitaxi.menu.MenuMainScreen;
import com.makitaxi.model.DriverNotification;
import com.makitaxi.model.PassengerResponse;
import com.makitaxi.model.RideRequest;
import com.makitaxi.utils.FirebaseHelper;
import com.makitaxi.utils.NotificationStatus;

import org.osmdroid.views.MapView;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class DriverMainScreen extends AppCompatActivity {

    private static final String TAG = "DriverMainScreen";

    // UI Components
    private MapView mapView;
    private ImageButton btnHamburgerMenu;
    private ImageButton btnZoomIn;
    private ImageButton btnZoomOut;
    private ImageButton btnMyLocation;
    private TextView txtStatus;
    private SwitchMaterial switchOnline;
    private LinearLayout rideDetailsBottomSheet;
    private BottomSheetBehavior<LinearLayout> bottomSheetBehavior;
    private LinearLayout mapControls;

    private MapDriver map;

    // Firebase References
    private DatabaseReference rideRequestsRef;
    private DatabaseReference driverNotificationRef;
    private String driverId;
    private boolean isDriverOnline = false;

    // Listeners
    private ChildEventListener rideRequestListener;
    private ChildEventListener driverNotificationListener;
    private ChildEventListener passengerResponseListener;

    // Services
    private LocationUpdateService locationUpdateService;
    private AlertDialog rideRequestDialog;

    private Long rideActivationTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.driver_main_screen);

        handleSystemBars();
        initializeViews();
        initializeControllers();
        setupUIInteractions();
        stopListeningForRideRequests();

        // Initialize Firebase
        rideRequestsRef = FirebaseHelper.getRideRequestsRef();
        driverNotificationRef = FirebaseHelper.getDriverNotificationRef();
        driverId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        // Initialize services
        locationUpdateService = new LocationUpdateService(driverId, map);

        bottomSheetBehavior = BottomSheetBehavior.from(rideDetailsBottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    private void initializeControllers() {
        map = new MapDriver(this, mapView);
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
        btnHamburgerMenu = findViewById(R.id.btnHamburgerMenu);
        btnZoomIn = findViewById(R.id.btnZoomIn);
        btnZoomOut = findViewById(R.id.btnZoomOut);
        btnMyLocation = findViewById(R.id.btnMyLocation);
        txtStatus = findViewById(R.id.txtStatus);
        switchOnline = findViewById(R.id.switchOnline);
        rideDetailsBottomSheet = findViewById(R.id.rideDetailsBottomSheet);
        mapControls = findViewById(R.id.mapControls);
    }

    private void setupUIInteractions() {
        btnZoomIn.setOnClickListener(v -> map.zoomIn());
        btnZoomOut.setOnClickListener(v -> map.zoomOut());
        btnMyLocation.setOnClickListener(v -> map.centerOnCurrentLocation());
        btnHamburgerMenu.setOnClickListener(v -> openHamburgerMenu());
        switchOnline.setOnCheckedChangeListener((buttonView, isChecked) -> {
            toggleDriverStatus(isChecked);
        });
    }

    private void toggleDriverStatus() {
        isDriverOnline = !isDriverOnline;
        updateDriverStatusUI();

        if (isDriverOnline) {
            rideActivationTime = System.currentTimeMillis();
            locationUpdateService.startUpdates();
            listenForRideRequests();
            Toast.makeText(this, "✅ You are now online", Toast.LENGTH_SHORT).show();
        } else {
            locationUpdateService.stopUpdates();
            stopListeningForRideRequests();
            Toast.makeText(this, "⏸️ You are now offline", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleDriverStatus(boolean isOnline) {
        isDriverOnline = isOnline;
        updateDriverStatusUI();

        if (isOnline) {
            rideActivationTime = System.currentTimeMillis();
            locationUpdateService.startUpdates();
            listenForRideRequests();
            Toast.makeText(this, "✅ You are now online", Toast.LENGTH_SHORT).show();
        } else {
            locationUpdateService.stopUpdates();
            stopListeningForRideRequests();
            Toast.makeText(this, "⏸️ You are now offline", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateDriverStatusUI() {
        txtStatus.setText(isDriverOnline ? "Online" : "Offline");
        switchOnline.setChecked(isDriverOnline);
    }

    private void listenForRideRequests() {
        if (driverNotificationListener != null) {
            driverNotificationRef.removeEventListener(driverNotificationListener);
        }

        driverNotificationListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                DriverNotification request = snapshot.getValue(DriverNotification.class);
                if (request != null) {
                     if (request.getNotificationTimestamp() > rideActivationTime && NotificationStatus.CREATED.equals(request.getStatus())) {
                        showRideRequestDialog(request.getRideRequest());
                    }
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Log.d(TAG, "Ride request changed: " + snapshot.getKey());
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "Ride request removed: " + snapshot.getKey());
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Log.d(TAG, "Ride request moved: " + snapshot.getKey());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error listening for ride requests: " + error.getMessage());
                Toast.makeText(DriverMainScreen.this, "Error loading ride requests", Toast.LENGTH_SHORT).show();
            }
        };

        driverNotificationRef.orderByChild("driverId")
                .equalTo(driverId)
                .addChildEventListener(driverNotificationListener);
    }

    private void stopListeningForRideRequests() {
        if (driverNotificationListener != null) {
            driverNotificationRef.removeEventListener(driverNotificationListener);
        }
    }

    private void showRideRequestDialog(RideRequest request) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.ride_request_dialog, null);
        builder.setView(view);
        
        TextView txtRideDetails = view.findViewById(R.id.txtRideDetails);
        Button btnAccept = view.findViewById(R.id.btnAccept);
        Button btnDecline = view.findViewById(R.id.btnDecline);
        
        txtRideDetails.setText(String.format(Locale.getDefault(),
                "Pickup: %s\nDrop-off: %s\nDistance: %.1f km\nEstimated: %.0f RSD",
                request.getPickupAddress(), request.getDropoffAddress(),
                request.getDistance(), request.getEstimatedPrice()));
        
        AlertDialog dialog = builder.create();
        
        btnAccept.setOnClickListener(v -> {
            acceptRide(request);
            dialog.dismiss();
        });
        
        btnDecline.setOnClickListener(v -> {
            declineRide(request);
            dialog.dismiss();
        });
        
        dialog.show();
    }

    private void declineRide(RideRequest request) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", NotificationStatus.CANCELLED_BY_DRIVER);

        driverNotificationRef.child(request.getRequestId()).updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    if (rideRequestDialog != null) {
                        rideRequestDialog.dismiss();
                    }
                    Toast.makeText(this, "Ride declined", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to decline ride", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error declining ride: " + e.getMessage());
                });
    }

    private void acceptRide(RideRequest request) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", NotificationStatus.ACCEPTED_BY_DRIVER);

        driverNotificationRef.child(request.getRequestId()).updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    if (rideRequestDialog != null) {
                        rideRequestDialog.dismiss();
                    }
                    waitForPassengerConfirmation(request);
                    txtStatus.setText("Waiting for passenger");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to accept ride", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error accepting ride: " + e.getMessage());
                });
    }

    private void waitForPassengerConfirmation(RideRequest rideRequest) {
        DatabaseReference responsesRef = FirebaseHelper.gerPassengerResponse();

        if (passengerResponseListener != null) {
            responsesRef.removeEventListener(passengerResponseListener);
        }

        Query passengerResponse = responsesRef
                .orderByChild("driverId_RideRequestId")
                .equalTo(driverId + "_" + rideRequest.getRequestId())
                .limitToLast(1);

        passengerResponseListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                PassengerResponse response = snapshot.getValue(PassengerResponse.class);
                if (response != null) {
                    handlePassengerResponse(response, rideRequest);
                    responsesRef.removeEventListener(this);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Log.d(TAG, "Passenger response changed: " + snapshot.getKey());
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "Passenger response removed: " + snapshot.getKey());
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Log.d(TAG, "Passenger response moved: " + snapshot.getKey());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error listening for passenger response: " + error.getMessage());
                Toast.makeText(DriverMainScreen.this, "Error checking passenger response", Toast.LENGTH_SHORT).show();
            }
        };

        passengerResponse.addChildEventListener(passengerResponseListener);
    }

    private void handlePassengerResponse(PassengerResponse response, RideRequest rideRequest) {
        switch (response.getStatus()) {
            case ACCEPTED_BY_PASSENGER:
                isDriverOnline = false;
                updateDriverStatusUI();
                showRideDetailsPanel(rideRequest);
                Toast.makeText(this, "✅ Ride Confirmed!", Toast.LENGTH_SHORT).show();
                txtStatus.setText("On a ride");
                locationUpdateService.stopUpdates();
                stopListeningForRideRequests();
                break;

            case REJECTED_BY_PASSENGER:
                isDriverOnline = true;
                updateDriverStatusUI();
                hideRideDetailsPanel();
                Toast.makeText(this, "Ride was not confirmed by passenger", Toast.LENGTH_SHORT).show();
                listenForRideRequests();
                break;
        }
    }

    private void openHamburgerMenu() {
        startActivity(new Intent(this, MenuMainScreen.class));
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    private void showRideDetailsPanel(RideRequest ride) {
        TextView txtPickup = rideDetailsBottomSheet.findViewById(R.id.txtPickup);
        TextView txtDestination = rideDetailsBottomSheet.findViewById(R.id.txtDestination);
        TextView txtDistance = rideDetailsBottomSheet.findViewById(R.id.txtDistance);
        TextView txtDuration = rideDetailsBottomSheet.findViewById(R.id.txtDuration);
        TextView txtPrice = rideDetailsBottomSheet.findViewById(R.id.txtPrice);
        Button btnFinishRide = rideDetailsBottomSheet.findViewById(R.id.btnFinishRide);
        
        txtPickup.setText(ride.getPickupAddress());
        txtDestination.setText(ride.getDropoffAddress());
        txtDistance.setText(String.format(Locale.getDefault(), "%.1f Km", ride.getDistance()));
        txtDuration.setText(String.format(Locale.getDefault(), "%.0f min", ride.getDuration()));
        txtPrice.setText(String.format(Locale.getDefault(), "%.0f Din", ride.getEstimatedPrice()));
        
        btnFinishRide.setOnClickListener(v -> {
            // Handle finish ride logic
            hideRideDetailsPanel();
        });
        
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        adjustMapControls(true);
    }
    
    private void hideRideDetailsPanel() {
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        adjustMapControls(false);
    }

    private void adjustMapControls(boolean isPanelVisible) {
        if (mapControls == null) return;

        float translationY = isPanelVisible ? -rideDetailsBottomSheet.getHeight() : 0;
        mapControls.animate().translationY(translationY).setDuration(300).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
        if (isDriverOnline) {
            listenForRideRequests();
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

        // Clean up Firebase listeners
        if (rideRequestListener != null) {
            rideRequestsRef.removeEventListener(rideRequestListener);
        }
        if (driverNotificationListener != null) {
            driverNotificationRef.removeEventListener(driverNotificationListener);
        }
        if (passengerResponseListener != null) {
            FirebaseHelper.gerPassengerResponse().removeEventListener(passengerResponseListener);
        }

        // Clean up map
        if (mapView != null) {
            mapView.onDetach();
        }

        // Clean up dialogs
        if (rideRequestDialog != null && rideRequestDialog.isShowing()) {
            rideRequestDialog.dismiss();
        }

        // Stop services
        if (locationUpdateService != null) {
            locationUpdateService.stopUpdates();
        }
    }
}