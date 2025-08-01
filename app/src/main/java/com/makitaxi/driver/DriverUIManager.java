package com.makitaxi.driver;

import android.content.Intent;
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
import org.osmdroid.util.GeoPoint;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DriverUIManager {
    private static final String TAG = "DriverUIManager";

    private final AppCompatActivity activity;
    private final String driverId;

    // UI Components
    private MapView mapView;
    private ImageButton btnHamburgerMenu;
    private Button btnZoomIn;
    private Button btnZoomOut;
    private ImageButton btnMyLocation;
    private TextView txtStatus;
    private SwitchMaterial switchOnline;
    private LinearLayout rideDetailsBottomSheet;
    private BottomSheetBehavior<LinearLayout> bottomSheetBehavior;
    private LinearLayout mapControls;

    // Firebase References
    private DatabaseReference driverNotificationRef;
    private ChildEventListener driverNotificationListener;
    private ChildEventListener passengerResponseListener;

    // State
    private boolean isDriverOnline = false;
    private Long rideActivationTime;
    private AlertDialog rideRequestDialog;

    // Callbacks
    private OnDriverStatusChangeListener statusChangeListener;
    private OnRideActionListener rideActionListener;
    private OnMapInteractionListener mapInteractionListener;
    private MapDriver mapDriver;

    public interface OnDriverStatusChangeListener {
        void onDriverStatusChanged(boolean isOnline);
    }

    public interface OnRideActionListener {
        void onRideAccepted(RideRequest request);
        void onRideDeclined(RideRequest request);
        void onRideFinished(RideRequest request);
    }

    public interface OnMapInteractionListener {
        void onZoomIn();
        void onZoomOut();
        void onMyLocation();
    }

    public DriverUIManager(AppCompatActivity activity, String driverId) {
        this.activity = activity;
        this.driverId = driverId;
        initializeViews();
        setupUIInteractions();
        initializeFirebase();
    }

    public void setMapDriver(MapDriver mapDriver) {
        this.mapDriver = mapDriver;
    }

    private void initializeViews() {
        mapView = activity.findViewById(R.id.mapView);
        btnHamburgerMenu = activity.findViewById(R.id.btnHamburgerMenu);
        btnZoomIn = activity.findViewById(R.id.btnZoomIn);
        btnZoomOut = activity.findViewById(R.id.btnZoomOut);
        btnMyLocation = activity.findViewById(R.id.btnMyLocation);
        txtStatus = activity.findViewById(R.id.txtStatus);
        switchOnline = activity.findViewById(R.id.switchOnline);
        rideDetailsBottomSheet = activity.findViewById(R.id.rideDetailsBottomSheet);
        mapControls = activity.findViewById(R.id.mapControls);

        bottomSheetBehavior = BottomSheetBehavior.from(rideDetailsBottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    private void initializeFirebase() {
        driverNotificationRef = FirebaseHelper.getDriverNotificationRef();
    }

    private void setupUIInteractions() {
        btnZoomIn.setOnClickListener(v -> {
            if (mapInteractionListener != null) mapInteractionListener.onZoomIn();
        });

        btnZoomOut.setOnClickListener(v -> {
            if (mapInteractionListener != null) mapInteractionListener.onZoomOut();
        });

        btnMyLocation.setOnClickListener(v -> {
            if (mapInteractionListener != null) mapInteractionListener.onMyLocation();
        });

        btnHamburgerMenu.setOnClickListener(v -> openHamburgerMenu());

        switchOnline.setOnCheckedChangeListener((buttonView, isChecked) -> {
            toggleDriverStatus(isChecked);
        });
    }

    private void toggleDriverStatus(boolean isOnline) {
        isDriverOnline = isOnline;
        updateDriverStatusUI();

        if (isOnline) {
            rideActivationTime = System.currentTimeMillis();
            listenForRideRequests();
            Toast.makeText(activity, "✅ You are now online", Toast.LENGTH_SHORT).show();
        } else {
            stopListeningForRideRequests();
            Toast.makeText(activity, "⏸️ You are now offline", Toast.LENGTH_SHORT).show();
        }

        if (statusChangeListener != null) {
            statusChangeListener.onDriverStatusChanged(isOnline);
        }
    }

    private void updateDriverStatusUI() {
        txtStatus.setText(isDriverOnline ? "Online" : "Offline");
        switchOnline.setChecked(isDriverOnline);
    }

    public void listenForRideRequests() {
        if (driverNotificationListener != null) {
            driverNotificationRef.removeEventListener(driverNotificationListener);
        }

        driverNotificationListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                DriverNotification request = snapshot.getValue(DriverNotification.class);

                if (request != null) {
                    if (request.getNotificationTimestamp() > rideActivationTime && 
                        NotificationStatus.CREATED.equals(request.getStatus())) {
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
                Toast.makeText(activity, "Error loading ride requests", Toast.LENGTH_SHORT).show();
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
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View view = activity.getLayoutInflater().inflate(R.layout.ride_request_dialog, null);
        builder.setView(view);
        
        // Find all the new views
        TextView txtPickupLocation = view.findViewById(R.id.txtPickupLocation);
        TextView txtDropoffLocation = view.findViewById(R.id.txtDropoffLocation);
        TextView txtDistance = view.findViewById(R.id.txtDistance);
        TextView txtDuration = view.findViewById(R.id.txtDuration);
        TextView txtPrice = view.findViewById(R.id.txtPrice);
        Button btnAccept = view.findViewById(R.id.btnAccept);
        Button btnDecline = view.findViewById(R.id.btnDecline);

        // Populate the enhanced fields
        txtPickupLocation.setText(request.getPickupAddress());
        txtDropoffLocation.setText(request.getDropoffAddress());
        txtDistance.setText(String.format(Locale.getDefault(), "%.1f km", request.getDistance()));
        txtDuration.setText(String.format(Locale.getDefault(), "%.0f min", request.getDuration()));
        txtPrice.setText(String.format(Locale.getDefault(), "%.0f din", request.getEstimatedPrice()));
        
        rideRequestDialog = builder.create();
        
        // Remove dialog padding to eliminate white spaces
        if (rideRequestDialog.getWindow() != null) {
            rideRequestDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        btnAccept.setOnClickListener(v -> {
            if (rideActionListener != null) {
                rideActionListener.onRideAccepted(request);
            }
            rideRequestDialog.dismiss();
        });
        
        btnDecline.setOnClickListener(v -> {
            if (rideActionListener != null) {
                rideActionListener.onRideDeclined(request);
            }
            rideRequestDialog.dismiss();
        });
        
        rideRequestDialog.show();
    }

    private void openHamburgerMenu() {
        activity.startActivity(new Intent(activity, MenuMainScreen.class));
        activity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    public void waitForPassengerConfirmation(RideRequest rideRequest) {
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
                Toast.makeText(activity, "Error checking passenger response", Toast.LENGTH_SHORT).show();
            }
        };

        passengerResponse.addChildEventListener(passengerResponseListener);
    }

    private void handlePassengerResponse(PassengerResponse response, RideRequest rideRequest) {
        switch (response.getStatus()) {
            case ACCEPTED_BY_PASSENGER:
                updateDriverStatusUI();
                showRideDetailsPanel(rideRequest);
                drawRouteForRide(rideRequest);
                Toast.makeText(activity, "✅ Ride Confirmed!", Toast.LENGTH_SHORT).show();
                txtStatus.setText("On a ride");
                break;

            case REJECTED_BY_PASSENGER:
                updateDriverStatusUI();
                hideRideDetailsPanel();
                clearRoute();
                Toast.makeText(activity, "Ride was not confirmed by passenger", Toast.LENGTH_SHORT).show();
                listenForRideRequests();
                break;
        }
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
            if (rideActionListener != null) {
                rideActionListener.onRideFinished(ride);
                hideRideDetailsPanel();
            }
        });
        
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        adjustMapControls(true);
    }
    
    public void hideRideDetailsPanel() {
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        adjustMapControls(false);
        clearRoute();
        isDriverOnline = true;
        updateDriverStatusUI();
        listenForRideRequests();
        Toast.makeText(activity, "✅ Ride finished!", Toast.LENGTH_SHORT).show();
    }

    private void adjustMapControls(boolean isPanelVisible) {
        if (mapControls == null) return;

        float translationY = isPanelVisible ? -rideDetailsBottomSheet.getHeight() : 0;
        mapControls.animate().translationY(translationY).setDuration(300).start();
    }

    private void drawRouteForRide(RideRequest rideRequest) {
        if (mapDriver == null) return;

        // Check if coordinates are available
        if (rideRequest.getPickupLatitude() != 0 && rideRequest.getPickupLongitude() != 0 &&
            rideRequest.getDropoffLatitude() != 0 && rideRequest.getDropoffLongitude() != 0) {
            
            GeoPoint pickupPoint = new GeoPoint(rideRequest.getPickupLatitude(), rideRequest.getPickupLongitude());
            GeoPoint dropoffPoint = new GeoPoint(rideRequest.getDropoffLatitude(), rideRequest.getDropoffLongitude());
            
            // Get driver's current location
            GeoPoint driverLocation = mapDriver.getCurrentLocation();
            if (driverLocation == null) {
                Toast.makeText(activity, "⚠️ Driver location not available", Toast.LENGTH_SHORT).show();
                return;
            }

            // Step 1: Draw route from driver to pickup point
            mapDriver.drawDriverRouteToPickup(driverLocation, pickupPoint, new MapDriver.RoutingCallback() {
                @Override
                public void onRouteFound(List<GeoPoint> routePoints, double distance, double duration) {
                    Toast.makeText(activity, 
                        String.format("✅ Driver route: %.1f km, %.0f min", distance, duration), 
                        Toast.LENGTH_SHORT).show();
                    
                    // Step 2: Draw route from pickup to destination
                    mapDriver.drawPickupToDestinationRoute(pickupPoint, dropoffPoint, new MapDriver.RoutingCallback() {
                        @Override
                        public void onRouteFound(List<GeoPoint> routePoints, double distance, double duration) {
                            Toast.makeText(activity, 
                                String.format("✅ Main route: %.1f km, %.0f min", distance, duration), 
                                Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onRoutingError(String error) {
                            Toast.makeText(activity, "❌ Failed to draw main route: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onRoutingError(String error) {
                    Toast.makeText(activity, "❌ Failed to draw driver route: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(activity, "⚠️ Route coordinates not available", Toast.LENGTH_SHORT).show();
        }
    }

    public void clearRoute() {
        if (mapDriver != null) {
            mapDriver.clearMap();
        }
    }

    public void setStatusChangeListener(OnDriverStatusChangeListener listener) {
        this.statusChangeListener = listener;
    }

    public void setRideActionListener(OnRideActionListener listener) {
        this.rideActionListener = listener;
    }

    public void setMapInteractionListener(OnMapInteractionListener listener) {
        this.mapInteractionListener = listener;
    }

    public boolean isDriverOnline() {
        return isDriverOnline;
    }

    public void cleanup() {
        if (driverNotificationListener != null) {
            driverNotificationRef.removeEventListener(driverNotificationListener);
        }
        if (passengerResponseListener != null) {
            FirebaseHelper.gerPassengerResponse().removeEventListener(passengerResponseListener);
        }
        if (rideRequestDialog != null && rideRequestDialog.isShowing()) {
            rideRequestDialog.dismiss();
        }
    }
} 