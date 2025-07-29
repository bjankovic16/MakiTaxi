package com.makitaxi.passenger;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.makitaxi.R;
import com.makitaxi.menu.MenuMainScreen;
import com.makitaxi.model.PassengerResponse;
import com.makitaxi.model.RideRequest;
import com.makitaxi.model.User;
import com.makitaxi.utils.CircularImageView;
import com.makitaxi.utils.FirebaseHelper;
import com.makitaxi.utils.NotificationStatus;

import org.osmdroid.util.GeoPoint;
import java.util.HashMap;
import java.util.Map;

public class PassengerUIManager {
    private final AppCompatActivity activity;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<AutoCompleteTextView, Runnable> debounceMap = new HashMap<>();

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
    private TextView btnRide;
    private TextView iconCloseOverlay;
    private FrameLayout frameMapButton;
    private ImageButton btnMyLocation;
    private LinearLayout pickupLocationContainer;
    private LinearLayout destinationLocationContainer;
    private AlertDialog waitForDriverDialog;
    private View bottomSheetDriverDetailsView;
    private BottomSheetDialog bottomSheetDriverDetailsDialog;
    private ImageView pickupLoadingSpinner;
    private ImageView destinationLoadingSpinner;
    private RotateAnimation spinnerAnimation;
    private boolean controlsVisible = true;

    // Callbacks
    private OnRouteRequestListener routeRequestListener;
    private OnLocationSelectedListener locationSelectedListener;
    private OnMapInteractionListener mapInteractionListener;

    public interface OnRouteRequestListener {
        void onShowRouteRequested();
        void onClearRouteRequested();
        void onRideRequested();
    }

    public interface OnLocationSelectedListener {
        void onCurrentLocationSelected();
        void onMapLocationSelected();
    }

    public interface OnMapInteractionListener {
        void onZoomIn();
        void onZoomOut();
        void onMyLocation();
    }

    public PassengerUIManager(AppCompatActivity activity) {
        this.activity = activity;
        initializeViews();
        setupAnimations();
        setupUIInteractions();
    }

    private void initializeViews() {
        txtPickupLocation = activity.findViewById(R.id.txtPickupLocation);
        txtDestination = activity.findViewById(R.id.txtDestination);
        toggleControls = activity.findViewById(R.id.toggleControls);
        btnHamburgerMenu = activity.findViewById(R.id.btnHamburgerMenu);
        btnChoseFromMap = activity.findViewById(R.id.btnChoseFromMap);
        btnChoseCurrentLocation = activity.findViewById(R.id.btnChoseCurrentLocation);
        btnShowRoute = activity.findViewById(R.id.btnShowRoute);
        btnClearRoute = activity.findViewById(R.id.btnClearRoute);
        btnZoomIn = activity.findViewById(R.id.btnZoomIn);
        btnZoomOut = activity.findViewById(R.id.btnZoomOut);
        btnRide = activity.findViewById(R.id.btnRide);
        iconCloseOverlay = activity.findViewById(R.id.iconCloseOverlay);
        frameMapButton = activity.findViewById(R.id.frameMapButton);
        btnMyLocation = activity.findViewById(R.id.btnMyLocation);
        pickupLocationContainer = activity.findViewById(R.id.pickupLocationContainer);
        destinationLocationContainer = activity.findViewById(R.id.destinationLocationContainer);
        pickupLoadingSpinner = activity.findViewById(R.id.pickupLoadingSpinner);
        destinationLoadingSpinner = activity.findViewById(R.id.destinationLoadingSpinner);

        // Initialize ride button state
        btnRide.setEnabled(false);
        btnRide.setAlpha(0.5f);
    }

    private void setupAnimations() {
        spinnerAnimation = new RotateAnimation(0f, 360f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        spinnerAnimation.setDuration(900);
        spinnerAnimation.setRepeatCount(Animation.INFINITE);
        spinnerAnimation.setInterpolator(new LinearInterpolator());
    }

    private void setupUIInteractions() {
        toggleControls.setOnClickListener(v -> toggleControls());

        txtPickupLocation.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                btnChoseFromMap.setVisibility(View.VISIBLE);
                btnChoseCurrentLocation.setVisibility(View.VISIBLE);
                iconCloseOverlay.setVisibility(View.VISIBLE);
                frameMapButton.setVisibility(View.VISIBLE);
            }
        });

        txtDestination.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                btnChoseFromMap.setVisibility(View.VISIBLE);
                btnChoseCurrentLocation.setVisibility(View.VISIBLE);
                iconCloseOverlay.setVisibility(View.VISIBLE);
                frameMapButton.setVisibility(View.VISIBLE);
            }
        });

        pickupLocationContainer.setOnClickListener(v -> txtPickupLocation.requestFocus());
        destinationLocationContainer.setOnClickListener(v -> txtDestination.requestFocus());

        btnZoomIn.setOnClickListener(v -> {
            if (mapInteractionListener != null) mapInteractionListener.onZoomIn();
        });

        btnZoomOut.setOnClickListener(v -> {
            if (mapInteractionListener != null) mapInteractionListener.onZoomOut();
        });

        btnMyLocation.setOnClickListener(v -> {
            if (mapInteractionListener != null) mapInteractionListener.onMyLocation();
        });

        btnChoseCurrentLocation.setOnClickListener(v -> {
            if (locationSelectedListener != null) locationSelectedListener.onCurrentLocationSelected();
        });

        btnChoseFromMap.setOnClickListener(v -> {
            if (locationSelectedListener != null) locationSelectedListener.onMapLocationSelected();
        });

        btnShowRoute.setOnClickListener(v -> {
            if (routeRequestListener != null) routeRequestListener.onShowRouteRequested();
        });

        btnClearRoute.setOnClickListener(v -> {
            if (routeRequestListener != null) routeRequestListener.onClearRouteRequested();
        });

        btnHamburgerMenu.setOnClickListener(v -> openHamburgerMenu());

        btnRide.setOnClickListener(v -> {
            if (btnRide.isEnabled() && routeRequestListener != null) {
                routeRequestListener.onRideRequested();
            }
        });

        setupDebouncedAutocomplete(txtPickupLocation, true);
        setupDebouncedAutocomplete(txtDestination, false);
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
                        activity.runOnUiThread(() -> {
                            if (isPickup) showPickupSpinner(false);
                            else showDestinationSpinner(false);
                        });
                        return;
                    }

                    // This would typically call a location service for suggestions
                    // For now, we'll just hide the spinner
                    activity.runOnUiThread(() -> {
                        if (isPickup) showPickupSpinner(false);
                        else showDestinationSpinner(false);
                    });
                };

                debounceMap.put(field, task);

                if (isPickup) showPickupSpinner(true);
                else showDestinationSpinner(true);

                handler.postDelayed(task, 800);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
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
            toggleControls.setText("▼");
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
            hideKeyboard();
            toggleControls.setText("▲");
            iconCloseOverlay.setText("❌");
        }
    }

    private void hideKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                View currentFocus = activity.getCurrentFocus();
                if (currentFocus != null) {
                    imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                    currentFocus.clearFocus();
                }

                View rootView = activity.findViewById(android.R.id.content);
                if (rootView != null) {
                    imm.hideSoftInputFromWindow(rootView.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                }
            }
        } catch (Exception e) {
            Log.e("PassengerUIManager", "Error hiding keyboard: " + e.getMessage());
        }
    }

    private void openHamburgerMenu() {
        Intent intent = new Intent(activity, MenuMainScreen.class);
        activity.startActivity(intent);
        activity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
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

    public void setRouteRequestListener(OnRouteRequestListener listener) {
        this.routeRequestListener = listener;
    }

    public void setLocationSelectedListener(OnLocationSelectedListener listener) {
        this.locationSelectedListener = listener;
    }

    public void setMapInteractionListener(OnMapInteractionListener listener) {
        this.mapInteractionListener = listener;
    }

    public void enableRideButton() {
        btnRide.setEnabled(true);
        btnRide.setAlpha(1.0f);
        android.view.animation.Animation animation = android.view.animation.AnimationUtils.loadAnimation(activity, R.anim.fade_in_scale_up);
        btnRide.startAnimation(animation);
        startPulsingAnimation();
    }

    public void disableRideButton() {
        btnRide.setEnabled(false);
        btnRide.setAlpha(0.5f);
        android.view.animation.Animation animation = android.view.animation.AnimationUtils.loadAnimation(activity, R.anim.fade_out_scale_down);
        btnRide.startAnimation(animation);
        btnRide.clearAnimation();
    }

    private void startPulsingAnimation() {
        android.view.animation.ScaleAnimation pulseAnim = new android.view.animation.ScaleAnimation(
                1f, 1.1f, 1f, 1.1f,
                android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
                android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f
        );
        pulseAnim.setDuration(1000);
        pulseAnim.setRepeatCount(android.view.animation.Animation.INFINITE);
        pulseAnim.setRepeatMode(android.view.animation.Animation.REVERSE);
        pulseAnim.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        btnRide.startAnimation(pulseAnim);
    }

    public void showSearchingForDriverDialog(RideRequest request) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View dialogView = activity.getLayoutInflater().inflate(R.layout.searching_driver_dialog, null);
        builder.setView(dialogView);
        builder.setCancelable(false);

        waitForDriverDialog = builder.create();
        if (waitForDriverDialog.getWindow() != null) {
            waitForDriverDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        Button btnCancel = dialogView.findViewById(R.id.btnCancelSearch);
        DatabaseReference requestRef = FirebaseHelper.getRideRequestsRef().child(request.getRequestId());

        btnCancel.setOnClickListener(v -> {
            Map<String, Object> updates = new HashMap<>();
            updates.put("status", NotificationStatus.CANCELLED_BY_PASSENGER);
            request.setStatus(NotificationStatus.CANCELLED_BY_PASSENGER);
            requestRef.updateChildren(updates).addOnSuccessListener(aVoid -> {
                waitForDriverDialog.dismiss();
            });
        });
        waitForDriverDialog.show();
    }

    public void dismissSearchingDialog() {
        if (waitForDriverDialog != null) {
            waitForDriverDialog.dismiss();
        }
    }

    public void showDriverDetailsBottomSheet(String driverId, RideRequest rideRequest) {
        bottomSheetDriverDetailsView = activity.getLayoutInflater().inflate(R.layout.driver_details_bottom_sheet, null);
        bottomSheetDriverDetailsDialog = new BottomSheetDialog(activity);
        bottomSheetDriverDetailsDialog.setContentView(bottomSheetDriverDetailsView);
        bottomSheetDriverDetailsDialog.setCancelable(false);

        DatabaseReference driverRef = FirebaseHelper.getUserRequestsRef().child(driverId);
        driverRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User driver = snapshot.getValue(User.class);
                assert driver != null;
                populateDriverView(driver, rideRequest, driverId);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                bottomSheetDriverDetailsDialog.dismiss();
                Toast.makeText(activity, "❌ Error loading driver details", Toast.LENGTH_SHORT).show();
            }
        });

        bottomSheetDriverDetailsDialog.show();
    }

    private void populateDriverView(User driver, RideRequest rideRequest, String driverId) {
        CircularImageView imgDriverProfile = bottomSheetDriverDetailsView.findViewById(R.id.imgDriverProfile);
        TextView txtDriverName = bottomSheetDriverDetailsView.findViewById(R.id.txtDriverName);
        TextView txtDriverRating = bottomSheetDriverDetailsView.findViewById(R.id.txtDriverRating);
        TextView txtCarType = bottomSheetDriverDetailsView.findViewById(R.id.txtCarType);
        TextView txtRidePrice = bottomSheetDriverDetailsView.findViewById(R.id.txtRidePrice);
        TextView txtRideTime = bottomSheetDriverDetailsView.findViewById(R.id.txtRideTime);
        Button btnProceed = bottomSheetDriverDetailsView.findViewById(R.id.btnProceed);
        Button btnDecline = bottomSheetDriverDetailsView.findViewById(R.id.btnDecline);
        ImageButton btnCallDriver = bottomSheetDriverDetailsView.findViewById(R.id.btnCallDriver);
        ImageButton btnMessageDriver = bottomSheetDriverDetailsView.findViewById(R.id.btnMessageDriver);

        Glide.with(activity)
                .load(driver.getProfilePicture())
                .placeholder(R.drawable.taxi_logo)
                .error(R.drawable.taxi_logo)
                .into(imgDriverProfile);

        txtDriverName.setText(driver.getFullName());
        txtDriverRating.setText(String.valueOf(driver.getRating()));
        txtCarType.setText(rideRequest.getCarType());
        txtRidePrice.setText(String.format("%.0f din", rideRequest.getEstimatedPrice()));
        txtRideTime.setText(String.format("%.0f min", rideRequest.getDuration()));

        btnCallDriver.setOnClickListener(v -> {
            if (driver.getPhone() != null && !driver.getPhone().isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + driver.getPhone()));
                activity.startActivity(intent);
            } else {
                Toast.makeText(activity, "Driver's phone number is not available.", Toast.LENGTH_SHORT).show();
            }
        });

        btnMessageDriver.setOnClickListener(v -> {
            if (driver.getPhone() != null && !driver.getPhone().isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + driver.getPhone()));
                activity.startActivity(intent);
            } else {
                Toast.makeText(activity, "Driver's phone number is not available.", Toast.LENGTH_SHORT).show();
            }
        });

        btnProceed.setOnClickListener(v -> {
            PassengerResponse response = new PassengerResponse(
                    driverId,
                    rideRequest.getPassengerId(),
                    rideRequest.getRequestId(),
                    System.currentTimeMillis(),
                    NotificationStatus.ACCEPTED_BY_PASSENGER
            );

            DatabaseReference requestRef = FirebaseHelper.gerPassengerResponse().push();
            requestRef.setValue(response)
                    .addOnSuccessListener(aVoid -> {
                        bottomSheetDriverDetailsDialog.dismiss();
                        Toast.makeText(activity, "✅ Ride confirmed!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(activity, "❌ Failed to confirm ride.", Toast.LENGTH_SHORT).show();
                    });
        });

        btnDecline.setOnClickListener(v -> {
            PassengerResponse response = new PassengerResponse(
                    driverId,
                    rideRequest.getPassengerId(),
                    rideRequest.getRequestId(),
                    System.currentTimeMillis(),
                    NotificationStatus.REJECTED_BY_PASSENGER
            );

            DatabaseReference requestRef = FirebaseHelper.gerPassengerResponse().push();
            requestRef.setValue(response)
                    .addOnSuccessListener(aVoid -> {
                        bottomSheetDriverDetailsDialog.dismiss();
                        Toast.makeText(activity, "Ride rejected!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(activity, "❌ Failed to reject ride.", Toast.LENGTH_SHORT).show();
                    });
        });
    }
} 