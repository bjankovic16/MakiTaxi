package com.makitaxi.driver;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.makitaxi.R;
import com.makitaxi.menu.MenuMainScreen;
import com.makitaxi.model.RideRequest;
import com.makitaxi.utils.FirebaseHelper;

import org.osmdroid.views.MapView;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DriverMainScreen extends AppCompatActivity {

    // UI Components
    private MapView mapView;
    private ImageButton btnHamburgerMenu;
    private TextView toggleControls;
    private Button btnZoomIn;
    private Button btnZoomOut;
    private ImageButton btnMyLocation;
    private LinearLayout pickupLocationContainer;
    private LinearLayout destinationLocationContainer;
    private TextView txtPickupLocation;
    private TextView txtDestination;

    private MapDriver map;
    private boolean controlsVisible = true;

    private DatabaseReference rideRequestsRef;
    private String currentDriverId;
    private boolean isDriverOnline = false;
    private ChildEventListener rideRequestListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.driver_main_screen);
        handleSystemBars();
        initializeViews();
        initializeControllers();
        setupUIInteractions();
        rideRequestsRef = FirebaseHelper.getRideRequestsRef();
        currentDriverId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
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
        toggleControls = findViewById(R.id.toggleControls);
        pickupLocationContainer = findViewById(R.id.pickupLocationContainer);
        destinationLocationContainer = findViewById(R.id.destinationLocationContainer);
        txtPickupLocation = findViewById(R.id.txtPickupLocation);
        txtDestination = findViewById(R.id.txtDestination);
        btnZoomIn = findViewById(R.id.btnZoomIn);
        btnZoomOut = findViewById(R.id.btnZoomOut);
        btnMyLocation = findViewById(R.id.btnMyLocation);
    }

    private void setupUIInteractions() {
        toggleControls.setOnClickListener(v -> toggleControls());
        btnZoomIn.setOnClickListener(v -> map.zoomIn());
        btnZoomOut.setOnClickListener(v -> map.zoomOut());
        btnMyLocation.setOnClickListener(v -> map.centerOnCurrentLocation());
        btnHamburgerMenu.setOnClickListener(v -> openHamburgerMenu());
        pickupLocationContainer.setOnClickListener(v -> toggleDriverStatus());
    }

    private void toggleDriverStatus() {
        isDriverOnline = !isDriverOnline;
        if (isDriverOnline) {
            txtPickupLocation.setText("🟢 Online - Ready for rides");
            Toast.makeText(this, "✅ You are now online", Toast.LENGTH_SHORT).show();
            listenForRides();
        } else {
            txtPickupLocation.setText("🔴 Offline - Not accepting rides");
            Toast.makeText(this, "⏸️ You are now offline", Toast.LENGTH_SHORT).show();
            if (rideRequestListener != null && rideRequestsRef != null) {
                rideRequestsRef.removeEventListener(rideRequestListener);
            }
            txtPickupLocation.setText(isDriverOnline ? "🟢 Online - Ready for rides" : "🔴 Offline - Not accepting rides");
            txtDestination.setText("Waiting for ride requests...");
        }
    }

    private void listenForRides() {
        rideRequestListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                RideRequest request = snapshot.getValue(RideRequest.class);
                assert request != null;
                if ("PENDING".equals(request.getStatus())) {
                    if (request.getDeclinedBy() == null || !request.getDeclinedBy().containsKey(currentDriverId)) {
                        showRideRequestDialog(request);
                    }
                }
            }
            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {
                RideRequest request = snapshot.getValue(RideRequest.class);
                assert request != null;
                if ("PENDING".equals(request.getStatus())) {
                    if (request.getDeclinedBy() == null || !request.getDeclinedBy().containsKey(currentDriverId)) {
                        showRideRequestDialog(request);
                    }
                }
            }
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };

        //adding listener to only pending status
        rideRequestsRef.orderByChild("status").equalTo("PENDING").addChildEventListener(rideRequestListener);
    }

    private void showRideRequestDialog(RideRequest request) {
        new android.app.AlertDialog.Builder(this)
            .setTitle("New Ride Request")
            .setMessage(String.format("Pickup: %s\nDrop-off: %s\nDistance: %.1f km\nEstimated: %.0f RSD", request.getPickupAddress(), request.getDropoffAddress(), request.getDistance(), request.getEstimatedPrice()))
            .setPositiveButton("Accept", (dialog, which) -> acceptRide(request))
            .setNegativeButton("Decline", (dialog, which) -> declineRide(request, dialog))
            .setCancelable(false)
            .show();
    }

    private void declineRide(RideRequest request, android.content.DialogInterface dialog) {
        DatabaseReference declinedRef = rideRequestsRef.child(request.getRequestId()).child("declinedBy").child(currentDriverId);
        declinedRef.setValue(true).addOnSuccessListener(aVoid -> {
            dialog.dismiss();
            Toast.makeText(this, "Ride declined.", Toast.LENGTH_SHORT).show();
        });
    }

    private void acceptRide(RideRequest request) {
        DatabaseReference currentRequestRef = rideRequestsRef.child(request.getRequestId());

        currentRequestRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String currentStatus = snapshot.child("status").getValue(String.class);
                    if ("PENDING".equals(currentStatus)) {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("status", "ACCEPTED");
                        updates.put("driverId", currentDriverId);

                        currentRequestRef.updateChildren(updates)
                            .addOnSuccessListener(aVoid -> {
                                isDriverOnline = false;
                                txtPickupLocation.setText("⏳ Waiting for passenger confirmation...");
                                txtDestination.setText(request.getDropoffAddress());
                                Toast.makeText(DriverMainScreen.this, "✅ Offer sent! Waiting for confirmation...", Toast.LENGTH_SHORT).show();
                                waitForPassengerConfirmation(request.getRequestId());
                            })
                            .addOnFailureListener(e -> Toast.makeText(DriverMainScreen.this, "❌ Failed to send offer", Toast.LENGTH_SHORT).show());
                    } else {
                        Toast.makeText(DriverMainScreen.this, "Ride is no longer available.", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DriverMainScreen.this, "Failed to check ride status.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void waitForPassengerConfirmation(String requestId) {
        DatabaseReference requestRef = rideRequestsRef.child(requestId);
        requestRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    RideRequest request = snapshot.getValue(RideRequest.class);
                    if (request != null) {
                        switch (request.getStatus()) {
                            case "CONFIRMED":
                                isDriverOnline = false; // Stay offline for the ride
                                Toast.makeText(DriverMainScreen.this, "✅ Ride Confirmed!", Toast.LENGTH_SHORT).show();
                                txtPickupLocation.setText("🚗 Ride confirmed! Heading to pickup...");
                                txtDestination.setText(request.getDropoffAddress());
                                if (rideRequestListener != null && rideRequestsRef != null) {
                                    rideRequestsRef.removeEventListener(rideRequestListener);
                                }
                                requestRef.removeEventListener(this);
                                break;
                            case "PENDING": // This means passenger declined
                            case "CANCELLED":
                                isDriverOnline = true; // Go back online
                                Toast.makeText(DriverMainScreen.this, "Ride was not confirmed.", Toast.LENGTH_SHORT).show();
                                txtPickupLocation.setText("🟢 Online - Ready for rides");
                                txtDestination.setText("Waiting for ride requests...");
                                listenForRides(); // Listen for new rides
                                requestRef.removeEventListener(this);
                                break;
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DriverMainScreen.this, "Error waiting for confirmation.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void toggleControls() {
        controlsVisible = !controlsVisible;
        float translationY = controlsVisible ? 0f : -100f;
        float alpha = controlsVisible ? 1f : 0.3f;
        View[] views = {pickupLocationContainer, destinationLocationContainer, btnZoomIn, btnZoomOut, btnMyLocation};
        for (View view : views) {
            view.animate().translationY(translationY).alpha(alpha).setDuration(300).start();
        }
        toggleControls.setText(controlsVisible ? "▼" : "▲");
    }

    private void openHamburgerMenu() {
        startActivity(new Intent(this, MenuMainScreen.class));
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (rideRequestListener != null && rideRequestsRef != null) {
            rideRequestsRef.removeEventListener(rideRequestListener);
        }
        if (mapView != null) {
            mapView.onDetach();
        }
    }
}
