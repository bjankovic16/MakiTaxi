package com.makitaxi.driver;

import android.content.Intent;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.makitaxi.utils.ToastUtils;

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

import java.util.List;
import java.util.Locale;

public class DriverUIManager {
    private static final String TAG = "DriverUIManager";

    private final AppCompatActivity activity;
    private final String driverId;

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

    private DatabaseReference driverNotificationRef;
    private ChildEventListener driverNotificationListener;
    private ChildEventListener passengerResponseListener;

    private boolean isDriverOnline = false;
    private Long rideActivationTime;
    private AlertDialog rideRequestDialog;
    
    private CountDownTimer rideRequestTimer;
    private TextView txtTimer;
    private ProgressBar timerProgress;

    private OnDriverStatusChangeListener statusChangeListener;
    private OnRideActionListener rideActionListener;
    private OnMapInteractionListener mapInteractionListener;
    private MapDriver mapDriver;
    private String activeRideRequestId;

    private boolean rideAcceptedByPassenger = false;

    public interface OnDriverStatusChangeListener {
        void onDriverStatusChanged(boolean isOnline);
    }

    public interface OnRideActionListener {
        void onRideAccepted(RideRequest request);
        void onRideDeclined(RideRequest request);
        void onRideFinished(RideRequest request);

        void onRideTimeout(RideRequest request);
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

    public boolean getRideAcceptedByPassenger() {
        return rideAcceptedByPassenger;
    }

    public void resetPassengerAcceptance() {
        rideAcceptedByPassenger = false;
    }

    public void toggleDriverStatus(boolean isOnline) {
        isDriverOnline = isOnline;
        updateDriverStatusUI();

        if (isOnline) {
            rideActivationTime = System.currentTimeMillis();
            listenForRideRequests();
        } else {
            pauseListeningForRideRequests();
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
        rideAcceptedByPassenger = false;
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
                ToastUtils.showError(activity, "Error loading ride requests");
            }
        };

        driverNotificationRef.orderByChild("driverId")
                .equalTo(driverId)
                .addChildEventListener(driverNotificationListener);
    }

    public void pauseListeningForRideRequests() {
        if (driverNotificationListener != null) {
            driverNotificationRef.removeEventListener(driverNotificationListener);
        }
    }

    private void showRideRequestDialog(RideRequest request) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View view = activity.getLayoutInflater().inflate(R.layout.ride_request_dialog, null);
        builder.setView(view);
        
        TextView txtPickupLocation = view.findViewById(R.id.txtPickupLocation);
        TextView txtDropoffLocation = view.findViewById(R.id.txtDropoffLocation);
        TextView txtDistance = view.findViewById(R.id.txtDistance);
        TextView txtDuration = view.findViewById(R.id.txtDuration);
        TextView txtPrice = view.findViewById(R.id.txtPrice);
        Button btnAccept = view.findViewById(R.id.btnAccept);
        Button btnDecline = view.findViewById(R.id.btnDecline);
        
        txtTimer = view.findViewById(R.id.txtTimer);
        timerProgress = view.findViewById(R.id.timerProgress);

        txtPickupLocation.setText(request.getPickupAddress());
        txtDropoffLocation.setText(request.getDropoffAddress());
        txtDistance.setText(String.format(Locale.getDefault(), "%.1f km", request.getDistance()));
        txtDuration.setText(String.format(Locale.getDefault(), "%.0f min", request.getDuration()));
        txtPrice.setText(String.format(Locale.getDefault(), "%.0f din", request.getEstimatedPrice()));
        
        rideRequestDialog = builder.create();
        
        if (rideRequestDialog.getWindow() != null) {
            rideRequestDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        startRideRequestTimer(request);
        
        btnAccept.setOnClickListener(v -> {
            stopRideRequestTimer();
            if (rideActionListener != null) {
                rideActionListener.onRideAccepted(request);
            }
            // Pause listening for new requests while waiting for passenger
            pauseListeningForRideRequests();
            rideRequestDialog.dismiss();
        });
        
        btnDecline.setOnClickListener(v -> {
            stopRideRequestTimer();
            if (rideActionListener != null) {
                rideActionListener.onRideDeclined(request);
            }
            rideRequestDialog.dismiss();
        });
        
        rideRequestDialog.show();
    }

    private void openHamburgerMenu() {
        activity.startActivity(new Intent(activity, MenuMainScreen.class));
    }

    public void waitForPassengerConfirmation(RideRequest rideRequest) {
        DatabaseReference responsesRef = FirebaseHelper.getPassengerResponseRef();
        activeRideRequestId = rideRequest.getRequestId();

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
                ToastUtils.showError(activity, "Error checking passenger response");
            }
        };

        passengerResponse.addChildEventListener(passengerResponseListener);
    }

    private void handlePassengerResponse(PassengerResponse response, RideRequest rideRequest) {
        switch (response.getStatus()) {
            case ACCEPTED_BY_PASSENGER:
                rideAcceptedByPassenger = true;
                updateDriverStatusUI();
                showRideDetailsPanel(rideRequest);
                drawRouteForRide(rideRequest);
                txtStatus.setText("On a ride");
                break;

            case DECLINED_BY_PASSENGER:
                updateDriverStatusUI();
                hideRideDetailsPanel();
                clearRoute();
                ToastUtils.showWarning(activity, "Ride was not confirmed by passenger");
                listenForRideRequests();
                activeRideRequestId = null;
                break;
        }
    }

    private void showRideDetailsPanel(RideRequest ride) {
        activeRideRequestId = ride.getRequestId();
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
        activeRideRequestId = null;
        rideAcceptedByPassenger = false;
    }

    private void adjustMapControls(boolean isPanelVisible) {
        if (mapControls == null) return;

        float translationY = isPanelVisible ? -rideDetailsBottomSheet.getHeight() : 0;
        mapControls.animate().translationY(translationY).setDuration(300).start();
    }

    private void drawRouteForRide(RideRequest rideRequest) {
        if (mapDriver == null) return;

        if (rideRequest.getPickupLatitude() != 0 && rideRequest.getPickupLongitude() != 0 &&
            rideRequest.getDropoffLatitude() != 0 && rideRequest.getDropoffLongitude() != 0) {
            
            GeoPoint pickupPoint = new GeoPoint(rideRequest.getPickupLatitude(), rideRequest.getPickupLongitude());
            GeoPoint dropoffPoint = new GeoPoint(rideRequest.getDropoffLatitude(), rideRequest.getDropoffLongitude());
            
            GeoPoint driverLocation = mapDriver.getCurrentLocation();
            if (driverLocation == null) {
                ToastUtils.showWarning(activity, "Driver location not available");
                return;
            }

            mapDriver.drawDriverRouteToPickup(driverLocation, pickupPoint, new MapDriver.RoutingCallback() {
                @Override
                public void onRouteFound(List<GeoPoint> routePoints, double distance, double duration) {

                    mapDriver.drawPickupToDestinationRoute(pickupPoint, dropoffPoint, new MapDriver.RoutingCallback() {
                        @Override
                        public void onRouteFound(List<GeoPoint> routePoints, double distance, double duration) {
                        }

                        @Override
                        public void onRoutingError(String error) {
                            ToastUtils.showError(activity, "Failed to draw main route: " + error);
                        }
                    });
                }

                @Override
                public void onRoutingError(String error) {
                    ToastUtils.showError(activity, "Failed to draw driver route: " + error);
                }
            });
        } else {
            ToastUtils.showWarning(activity, "Route coordinates not available");
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
            FirebaseHelper.getPassengerResponseRef().removeEventListener(passengerResponseListener);
        }
        if (rideRequestDialog != null && rideRequestDialog.isShowing()) {
            rideRequestDialog.dismiss();
        }
        stopRideRequestTimer();
    }

    public boolean hasActiveRide() {
        return activeRideRequestId != null && !activeRideRequestId.isEmpty();
    }

    public void updateRideStatus(NotificationStatus status) {
        try {
            String requestId = activeRideRequestId;
            if (requestId == null || requestId.isEmpty()) return;
            DatabaseReference ref = FirebaseHelper.getRideRequestsRef().child(requestId);
            java.util.Map<String, Object> updates = new java.util.HashMap<>();
            updates.put("status", status);
            ref.updateChildren(updates);
        } catch (Exception ignored) {}
    }

    private void startRideRequestTimer(RideRequest request) {
        stopRideRequestTimer();

        rideRequestTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsLeft = (int) (millisUntilFinished / 1000);
                int progress = (int) ((millisUntilFinished / 60000.0) * 100);
                
                if (txtTimer != null) {
                    txtTimer.setText(String.valueOf(secondsLeft));
                }
                if (timerProgress != null) {
                    timerProgress.setProgress(progress);
                }
            }

            @Override
            public void onFinish() {
                if (rideRequestDialog != null && rideRequestDialog.isShowing()) {
                    if (rideActionListener != null) {
                        rideActionListener.onRideTimeout(request);
                    }
                    rideRequestDialog.dismiss();
                }
            }
        };
        
        rideRequestTimer.start();
    }

    private void stopRideRequestTimer() {
        if (rideRequestTimer != null) {
            rideRequestTimer.cancel();
            rideRequestTimer = null;
        }
    }
} 