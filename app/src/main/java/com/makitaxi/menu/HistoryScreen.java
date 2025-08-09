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
    private TextView txtDateHeader;

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
        //txtDateHeader = findViewById(R.id.txtDateHeader);
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
            dateHeader.setTextColor(getResources().getColor(R.color.background));
            dateHeader.setTextSize(17);
            dateHeader.setPadding(0, 8, 0, 18);

            LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            headerParams.setMargins(0, 25, 0, 0);
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

        int marginInDp = 12;
        float scale = getResources().getDisplayMetrics().density;
        int marginInPx = (int) (marginInDp * scale + 0.5f);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(0, 0, 0, marginInPx);
        itemView.setLayoutParams(layoutParams);

        TextView txtPickupLocation = itemView.findViewById(R.id.txtPickupLocation);
        TextView txtDropoffLocation = itemView.findViewById(R.id.txtDropoffLocation);
        ImageView starSingleItem = itemView.findViewById(R.id.starSingleItem);
        TextView txtRating = itemView.findViewById(R.id.txtRating);
        Button btnAction = itemView.findViewById(R.id.btnAction);

        txtPickupLocation.setText(feedback.getPickupAddress());
        txtDropoffLocation.setText(feedback.getDropoffAddress());

        // Check user type and feedback status
        if ("driver".equals(userType)) {
            if (feedback.isSubmitted()) {
                txtRating.setText(String.valueOf(feedback.getRating()));
                btnAction.setText("VIEW FEEDBACK");
                starSingleItem.setVisibility(View.VISIBLE);
                btnAction.setOnClickListener(v -> openFeedbackDetails(feedback));
            } else {
                btnAction.setText("NO FEEDBACK YET");
                btnAction.setEnabled(false);
                btnAction.setAlpha(0.5f);
            }
        } else {
            if (feedback.isSubmitted()) {
                txtRating.setText(String.valueOf(feedback.getRating()));
                starSingleItem.setVisibility(View.VISIBLE);
                btnAction.setText("VIEW FEEDBACK");
                btnAction.setOnClickListener(v -> openFeedbackDetails(feedback));
            } else {
                btnAction.setText("GIVE FEEDBACK");
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
            if (selectedRating[0] > 0) {
                submitFeedback(feedback, selectedRating[0], etComments.getText().toString());
                dialog.dismiss();
            } else {
                for (ImageView star : stars) {
                    star.clearAnimation();
                    star.startAnimation(pulseAnimation);
                }
            }
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
                    
                    // Update user statistics
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
        String userId = currentUser.getUid();
        
        // Update both passenger and driver statistics
        updateUserStatistics(feedback.getPassengerId(), rating, 0); // Passenger gets ride count
        updateUserStatistics(feedback.getDriverId(), rating, 15); // Driver gets ride count + estimated distance
    }

    private void updateUserStatistics(String userId, int rating, int distance) {
        database.getReference("users").child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            User user = dataSnapshot.getValue(User.class);
                            if (user != null) {
                                // Update statistics based on user role
                                if (currentUser.getUid().equals(userId)) {
                                    // If it's the current user (passenger giving feedback)
                                    user.incrementRideCount();
                                    if (distance > 0) {
                                        user.addDistance(distance);
                                    }
                                } else {
                                    // If it's the driver receiving feedback
                                    user.updateStatisticsAfterRide(rating, distance);
                                }
                                
                                // Save updated user back to Firebase
                                database.getReference("users").child(userId).setValue(user)
                                        .addOnSuccessListener(aVoid -> 
                                            Log.d(TAG, "User statistics updated successfully"))
                                        .addOnFailureListener(e -> 
                                            Log.e(TAG, "Failed to update user statistics: " + e.getMessage()));
                            }
                        }
                    }
                    
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e(TAG, "Error loading user for statistics update: " + databaseError.getMessage());
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