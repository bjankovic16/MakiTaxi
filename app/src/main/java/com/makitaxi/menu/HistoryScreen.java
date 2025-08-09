package com.makitaxi.menu;


import static com.google.android.material.internal.ViewUtils.dpToPx;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.makitaxi.R;
import com.makitaxi.model.FeedbackRequest;
import com.makitaxi.model.User;
import com.makitaxi.utils.FirebaseHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HistoryScreen extends AppCompatActivity {

    private static final String TAG = "HistoryScreen";

    private ImageButton btnBack;
    private LinearLayout rideHistoryContainer;
    private LinearLayout emptyStateContainer;


    private FirebaseAuth auth;
    private FirebaseDatabase database;
    private FirebaseUser currentUser;
    private String userType;
    private Animation pulseAnimation;

    private List<FeedbackRequest> feedbackHistory = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.history_screen);

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance("https://makitaxi-e4108-default-rtdb.europe-west1.firebasedatabase.app/");
        currentUser = auth.getCurrentUser();
        pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse);

        initializeViews();
        setupUIInteractions();
        determineUserType();
        loadRideHistory();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        rideHistoryContainer = findViewById(R.id.rideHistoryContainer);
        emptyStateContainer = findViewById(R.id.emptyStateContainer);

    }

    private void setupUIInteractions() {
        btnBack.setOnClickListener(v -> finish());
    }

    private void determineUserType() {
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Check if user is a driver or passenger by looking at their profile
        database.getReference("users").child(currentUser.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            User user = dataSnapshot.getValue(User.class);
                            if (user != null) {
                                userType = user.getUserType(); // "passenger" or "driver"
                                loadRideHistory();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e(TAG, "Error determining user type: " + databaseError.getMessage());
                    }
                });
    }

    private void loadRideHistory() {
        if (currentUser == null || userType == null) return;

        String userId = currentUser.getUid();

        // Load from feedback_requests collection and filter by user
        FirebaseHelper.getFeedbackRequestsRef()
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        feedbackHistory.clear();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            FeedbackRequest feedback = snapshot.getValue(FeedbackRequest.class);
                            if (feedback != null) {
                                // Filter by user type
                                if (userType.equals("driver") && userId.equals(feedback.getDriverId())) {
                                    feedbackHistory.add(feedback);
                                } else if (userType.equals("passenger") && userId.equals(feedback.getPassengerId())) {
                                    feedbackHistory.add(feedback);
                                }
                            }
                        }
                        displayRideHistory();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e(TAG, "Error loading feedback history: " + databaseError.getMessage());
                    }
                });
    }

    private void displayRideHistory() {
        rideHistoryContainer.removeAllViews();

        if (feedbackHistory.isEmpty()) {
            emptyStateContainer.setVisibility(View.VISIBLE);
            rideHistoryContainer.setVisibility(View.GONE);
            return;
        }

        emptyStateContainer.setVisibility(View.GONE);
        rideHistoryContainer.setVisibility(View.VISIBLE);


        // Group feedback requests by date
        Map<String, List<FeedbackRequest>> feedbackByDate = new HashMap<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd / MM / yyyy", Locale.getDefault());

        for (FeedbackRequest feedback : feedbackHistory) {
            String date = dateFormat.format(new Date(feedback.getTimestamp()));
            if (!feedbackByDate.containsKey(date)) {
                feedbackByDate.put(date, new ArrayList<>());
            }
            feedbackByDate.get(date).add(feedback);
        }

        // Display feedback requests grouped by date
        for (Map.Entry<String, List<FeedbackRequest>> entry : feedbackByDate.entrySet()) {
            String date = entry.getKey();
            List<FeedbackRequest> feedbackForDate = entry.getValue();

            TextView dateHeader = new TextView(this);
            dateHeader.setText(date);
            dateHeader.setTextColor(getResources().getColor(android.R.color.black));
            dateHeader.setTextSize(16);
            dateHeader.setTypeface(dateHeader.getTypeface(), android.graphics.Typeface.BOLD);
            dateHeader.setPadding(4, 0, 0, 12);

            LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            headerParams.setMargins(0, 24, 0, 8);
            dateHeader.setLayoutParams(headerParams);

            rideHistoryContainer.addView(dateHeader);

            for (FeedbackRequest feedback : feedbackForDate) {
                View feedbackItemView = createFeedbackHistoryItem(feedback);
                rideHistoryContainer.addView(feedbackItemView);
            }
        }
    }

    private View createFeedbackHistoryItem(FeedbackRequest feedback) {
        View itemView = LayoutInflater.from(this).inflate(R.layout.ride_history_item, null);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        // Add spacing between items (convert dp to pixels)
        int marginInDp = 16;
        int marginInPx = (int) (marginInDp * getResources().getDisplayMetrics().density);
        layoutParams.setMargins(0, 0, 0, marginInPx);
        itemView.setLayoutParams(layoutParams);

        // Find all the new views
        TextView txtTripDate = itemView.findViewById(R.id.txtTripDate);
        TextView txtTripPrice = itemView.findViewById(R.id.txtTripPrice);
        TextView txtPickupLocation = itemView.findViewById(R.id.txtPickupLocation);
        TextView txtDropoffLocation = itemView.findViewById(R.id.txtDropoffLocation);
        ImageView starSingleItem = itemView.findViewById(R.id.starSingleItem);
        TextView txtRating = itemView.findViewById(R.id.txtRating);
        Button btnAction = itemView.findViewById(R.id.btnAction);

        // Set trip date and time
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault());
        String formattedDateTime = dateTimeFormat.format(new Date(feedback.getTimestamp()));
        txtTripDate.setText(formattedDateTime);

        // Set trip price (you might need to add this field to FeedbackRequest)
        if (feedback.getPrice() > 0) {
            txtTripPrice.setText(String.format("%.0f din", feedback.getPrice()));
        } else {
            txtTripPrice.setText("N/A");
        }

        // Set addresses
        txtPickupLocation.setText(feedback.getPickupAddress());
        txtDropoffLocation.setText(feedback.getDropoffAddress());

        // Handle rating display and button actions
        if (feedback.isSubmitted()) {
            // Show rating for submitted feedback
            txtRating.setText(String.valueOf(feedback.getRating()));
            starSingleItem.setVisibility(View.VISIBLE);
            btnAction.setText("VIEW FEEDBACK");
            btnAction.setEnabled(true);
            btnAction.setAlpha(1.0f);
            btnAction.setOnClickListener(v -> openFeedbackDetails(feedback));
        } else {
            // Handle unsubmitted feedback
            if ("driver".equals(userType)) {
                txtRating.setText("Pending");
                starSingleItem.setVisibility(View.GONE);
                btnAction.setText("AWAITING FEEDBACK");
                btnAction.setEnabled(false);
                btnAction.setAlpha(0.6f);
            } else {
                txtRating.setText("Rate");
                starSingleItem.setVisibility(View.GONE);
                btnAction.setText("GIVE FEEDBACK");
                btnAction.setEnabled(true);
                btnAction.setAlpha(1.0f);
                btnAction.setOnClickListener(v -> showRatingDialog(feedback));
            }
        }

        return itemView;
    }

    private void showRatingDialog(FeedbackRequest feedback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.trip_rating_dialog, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        
        // Make dialog background transparent to show rounded corners
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        dialog.show();

        ImageView[] stars = {
                dialogView.findViewById(R.id.star1),
                dialogView.findViewById(R.id.star2),
                dialogView.findViewById(R.id.star3),
                dialogView.findViewById(R.id.star4),
                dialogView.findViewById(R.id.star5)
        };

        final int[] selectedRating = {0};
        final boolean[] isAnimating = {false};

        Animation pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse);
        pulseAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                isAnimating[0] = true;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                isAnimating[0] = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        TextView txtRatingTitle = dialogView.findViewById(R.id.txtRatingTitle);
        txtRatingTitle.setText("How was your trip with " + feedback.getDriverName() + "?");

        EditText etComments = dialogView.findViewById(R.id.etComments);
        Button btnSubmitReview = dialogView.findViewById(R.id.btnSubmitReview);

        for (int i = 0; i < stars.length; i++) {
            final int starIndex = i + 1;
            stars[i].setOnClickListener(v -> {
                selectedRating[0] = starIndex;
                updateStarDisplay(stars, starIndex);
                if (isAnimating[0]) {
                    for (ImageView star : stars) {
                        star.clearAnimation();
                    }
                }
            });
        }

        btnSubmitReview.setOnClickListener(v -> {
            String comment = etComments.getText().toString().trim();
            
            if (selectedRating[0] <= 0) {
                // No rating selected - animate stars
                for (ImageView star : stars) {
                    star.clearAnimation();
                    star.startAnimation(pulseAnimation);
                }
                return;
            }
            
            if (comment.length() < 20) {
                // Comment too short - show error message
                Toast.makeText(HistoryScreen.this, 
                    String.format("❌ Comment must be at least 20 characters. Current: %d characters", comment.length()), 
                    Toast.LENGTH_LONG).show();
                etComments.requestFocus();
                return;
            }
            
            // All validations passed - submit feedback
            submitFeedback(feedback, selectedRating[0], comment);
            dialog.dismiss();
        });
    }

    private void updateStarDisplay(ImageView[] stars, int selectedRating) {
        for (int i = 0; i < stars.length; i++) {
            if (i < selectedRating) {
                stars[i].setColorFilter(getResources().getColor(android.R.color.holo_orange_light));
            } else {
                stars[i].setColorFilter(getResources().getColor(android.R.color.darker_gray));
            }
        }
    }

    private void submitFeedback(FeedbackRequest feedback, int rating, String comment) {
        // Update the feedback request with the rating and comment
        feedback.setRating(rating);
        feedback.setComment(comment);
        feedback.setSubmitted(true);

        // Save updated feedback to Firebase
        Log.d(TAG, "Submitting feedback: " + feedback.getFeedbackId() + " with rating: " + rating);
        
        FirebaseHelper.getFeedbackRequestsRef().child(feedback.getFeedbackId()).setValue(feedback)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Feedback submitted successfully");
                    
                    updateUserStatisticsAfterFeedback(feedback, rating);
                    
                    Toast.makeText(this, "✅ Feedback submitted successfully!", Toast.LENGTH_SHORT).show();
                    displayRideHistory(); // Refresh the display
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to submit feedback: " + e.getMessage());
                    Toast.makeText(this, "❌ Failed to submit feedback: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUserStatisticsAfterFeedback(FeedbackRequest feedback, int rating) {
        // Logic: Both passenger and driver traveled the same distance
        // Only drivers get ratings from passengers
        
        if ("passenger".equals(userType)) {
            // Passenger is giving feedback to driver
            updatePassengerStatistics(feedback.getPassengerId(), feedback); // Passenger gets ride count + distance
            updateDriverStatistics(feedback.getDriverId(), rating, feedback); // Driver gets rating + ride count + distance
        } else {
            // Driver is giving feedback to passenger  
            updateDriverStatistics(feedback.getDriverId(), 0, feedback); // Driver gets ride count + distance (no rating from passenger)
            updatePassengerStatistics(feedback.getPassengerId(), feedback); // Passenger gets ride count + distance
        }
    }

    private void updatePassengerStatistics(String passengerId, FeedbackRequest feedback) {
        database.getReference("users").child(passengerId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            User user = dataSnapshot.getValue(User.class);
                            if (user != null) {
                                // Get actual distance from feedback request
                                double feedbackDistance = feedback.getDistance();
                                int actualDistance = (int) Math.round(feedbackDistance);
                                double tripPrice = feedback.getPrice();
                                
                                // If distance is 0 or not set, calculate from price as fallback
                                if (actualDistance <= 0) {
                                    actualDistance = Math.max(1, (int) (tripPrice / 50)); // Minimum 1 km
                                    Log.d(TAG, "Distance was 0, calculated from price: " + actualDistance + " km");
                                }
                                
                                Log.d(TAG, "Feedback submission - passenger (ride count and distance already updated on completion)");
                                
                                database.getReference("users").child(passengerId).setValue(user)
                                        .addOnSuccessListener(aVoid -> 
                                            Log.d(TAG, "Passenger statistics updated successfully"))
                                        .addOnFailureListener(e -> 
                                            Log.e(TAG, "Failed to update passenger statistics: " + e.getMessage()));
                            }
                        }
                    }
                    
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e(TAG, "Error loading passenger for statistics update: " + databaseError.getMessage());
                    }
                });
    }

    private void updateDriverStatistics(String driverId, int rating, FeedbackRequest feedback) {
        database.getReference("users").child(driverId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            User user = dataSnapshot.getValue(User.class);
                            if (user != null) {
                                // Get actual distance from feedback request
                                double feedbackDistance = feedback.getDistance();
                                int actualDistance = (int) Math.round(feedbackDistance);
                                double tripPrice = feedback.getPrice();
                                
                                // If distance is 0 or not set, calculate from price as fallback
                                if (actualDistance <= 0) {
                                    actualDistance = Math.max(1, (int) (tripPrice / 50)); // Minimum 1 km
                                    Log.d(TAG, "Distance was 0, calculated from price: " + actualDistance + " km");
                                }
                                
                                Log.d(TAG, "Feedback submission - driver stats, Rating: " + rating);
                                
                                // Update driver statistics: only rating (ride count, distance, money already updated on completion)
                                if (rating > 0) {
                                    // Driver gets rating from passenger (don't update ride count/distance again)
                                    user.updateRatingOnly(rating);
                                }
                                
                                database.getReference("users").child(driverId).setValue(user)
                                        .addOnSuccessListener(aVoid -> 
                                            Log.d(TAG, "Driver statistics updated successfully"))
                                        .addOnFailureListener(e -> 
                                            Log.e(TAG, "Failed to update driver statistics: " + e.getMessage()));
                            }
                        }
                    }
                    
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e(TAG, "Error loading driver for statistics update: " + databaseError.getMessage());
                    }
                });
    }



    private void openFeedbackDetails(FeedbackRequest feedback) {
        Intent intent = new Intent(this, FeedbackDetailsScreen.class);
        intent.putExtra("feedback", feedback);
        intent.putExtra("userType", userType);
        startActivity(intent);
    }
} 