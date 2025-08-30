package com.makitaxi.menu;


import static com.google.android.material.internal.ViewUtils.dpToPx;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.makitaxi.utils.ToastUtils;
import com.makitaxi.config.AppConfig;

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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class HistoryScreen extends AppCompatActivity {

    private static final String TAG = "HistoryScreen";

    private ImageButton btnBack;
    private LinearLayout rideHistoryContainer;
    private LinearLayout emptyStateContainer;
    private LinearLayout statisticsContainer;
    private TextView txtTotalEarnings;
    private TextView txtTotalDistance;
    private TextView txtEarningsLabel;
    private TextView txtUserLabel;
    private TextView txtUserInfo;
    private Button btnDateFrom;
    private Button btnDateTo;
    private Spinner spinnerUserFilter;
    private Button btnApplyFilter;
    private Button btnClearFilter;

    private FirebaseAuth auth;
    private FirebaseDatabase database;
    private FirebaseUser currentUser;
    private String userType;
    private Animation pulseAnimation;

    private List<FeedbackRequest> feedbackHistory = new ArrayList<>();
    private List<FeedbackRequest> filteredHistory = new ArrayList<>();
    private List<User> userList = new ArrayList<>();
    
    private long dateFrom = 0;
    private long dateTo = 0;
    private String selectedUserId = null;
    private boolean isFilterActive = false;
    private boolean userListLoaded = false;
    private int lastSelectedSpinnerPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.history_screen);

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance(AppConfig.FIREBASE_DATABASE_URL);
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
        statisticsContainer = findViewById(R.id.statisticsContainer);
        txtTotalEarnings = findViewById(R.id.txtTotalEarnings);
        txtTotalDistance = findViewById(R.id.txtTotalDistance);
        txtEarningsLabel = findViewById(R.id.txtEarningsLabel);
        txtUserLabel = findViewById(R.id.txtUserLabel);
        txtUserInfo = findViewById(R.id.txtUserInfo);
        btnDateFrom = findViewById(R.id.btnDateFrom);
        btnDateTo = findViewById(R.id.btnDateTo);
        spinnerUserFilter = findViewById(R.id.spinnerUserFilter);
        btnApplyFilter = findViewById(R.id.btnApplyFilter);
        btnClearFilter = findViewById(R.id.btnClearFilter);
    }

    private void setupUIInteractions() {
        btnBack.setOnClickListener(v -> finish());
        
        btnDateFrom.setOnClickListener(v -> showDatePicker(true));
        btnDateTo.setOnClickListener(v -> showDatePicker(false));
        btnApplyFilter.setOnClickListener(v -> {
            applyFilters();
            refreshUserList();
        });
        btnClearFilter.setOnClickListener(v -> clearFilters());
    }

    private void showDatePicker(boolean isFromDate) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, selectedYear, selectedMonth, selectedDay) -> {
                Calendar selectedCalendar = Calendar.getInstance();
                selectedCalendar.set(selectedYear, selectedMonth, selectedDay);
                
                if (isFromDate) {
                    dateFrom = selectedCalendar.getTimeInMillis();
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    btnDateFrom.setText(sdf.format(selectedCalendar.getTime()));
                } else {
                    dateTo = selectedCalendar.getTimeInMillis();
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    btnDateTo.setText(sdf.format(selectedCalendar.getTime()));
                }
            },
            year, month, day
        );
        
        datePickerDialog.show();
    }

    private void loadUserList() {
        if (currentUser == null || userType == null) return;

        FirebaseHelper.getFeedbackRequestsRef().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Set<String> interactingUserIds = new HashSet<>();
                String currentUserId = currentUser.getUid();
                
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    FeedbackRequest feedback = snapshot.getValue(FeedbackRequest.class);
                    if (feedback != null) {
                        if (userType.equals("driver")) {
                            if (currentUserId.equals(feedback.getDriverId())) {
                                interactingUserIds.add(feedback.getPassengerId());
                            }
                        } else {
                            if (currentUserId.equals(feedback.getPassengerId())) {
                                interactingUserIds.add(feedback.getDriverId());
                            }
                        }
                    }
                }
                
                loadInteractingUsers(interactingUserIds);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error loading feedback requests: " + databaseError.getMessage());
            }
        });
    }

    private void loadInteractingUsers(Set<String> interactingUserIds) {
        if (interactingUserIds.isEmpty()) {
            List<String> userNames = new ArrayList<>();
            userNames.add("All users");
            setupUserSpinner(userNames);
            
            restoreSpinnerSelection();
            
            ToastUtils.showInfo(this, "No ride interactions found. Filter will show all rides.");
            userListLoaded = true;
            return;
        }

        database.getReference("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userList.clear();
                List<String> userNames = new ArrayList<>();
                userNames.add("All users");
                
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String userId = snapshot.getKey();
                    User user = snapshot.getValue(User.class);
                    
                    if (user != null && interactingUserIds.contains(userId)) {
                        userList.add(user);
                        userNames.add(user.getFullName());
                    }
                }
                
                setupUserSpinner(userNames);
                restoreSpinnerSelection();
                userListLoaded = true;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error loading interacting users: " + databaseError.getMessage());
            }
        });
    }

    private void setupUserSpinner(List<String> userNames) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this, 
            R.layout.drop_down_item, 
            userNames
        );
        adapter.setDropDownViewResource(R.layout.drop_down_item);
        spinnerUserFilter.setAdapter(adapter);
        
                        spinnerUserFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        lastSelectedSpinnerPosition = position;
                        if (position == 0) {
                            selectedUserId = null;
                        } else {
                            User selectedUser = userList.get(position - 1);
                            selectedUserId = selectedUser.getFullName();
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        selectedUserId = null;
                    }
                });
    }

    private void applyFilters() {
        rememberSpinnerSelection();
        
        isFilterActive = true;
        filterHistory();
        updateStatistics();
        updateEarningsLabel();
        updateUserLabels();
        displayRideHistory();
    }

    private void clearFilters() {
        isFilterActive = false;
        dateFrom = 0;
        dateTo = 0;
        selectedUserId = null;
        lastSelectedSpinnerPosition = 0;
        
        btnDateFrom.setText("From");
        btnDateTo.setText("To");
        spinnerUserFilter.setSelection(0);
        updateEarningsLabel();
        updateUserLabels();
        
        filteredHistory.clear();
        displayRideHistory();
    }

    private void filterHistory() {
        filteredHistory.clear();
        
        for (FeedbackRequest feedback : feedbackHistory) {
            boolean passesDateFilter = true;
            boolean passesUserFilter = true;
            
            if (dateFrom > 0 || dateTo > 0) {
                long feedbackTime = feedback.getTimestamp();
                
                if (dateFrom > 0 && feedbackTime < dateFrom) {
                    passesDateFilter = false;
                }
                
                if (dateTo > 0) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(dateTo);
                    calendar.set(Calendar.HOUR_OF_DAY, 23);
                    calendar.set(Calendar.MINUTE, 59);
                    calendar.set(Calendar.SECOND, 59);
                    long endOfDay = calendar.getTimeInMillis();
                    
                    if (feedbackTime > endOfDay) {
                        passesDateFilter = false;
                    }
                }
            }
            
            if (selectedUserId != null) {
                if (userType.equals("driver")) {
                    if (!selectedUserId.equals(feedback.getPassengerName())) {
                        passesUserFilter = false;
                    }
                } else {
                    if (!selectedUserId.equals(feedback.getDriverName())) {
                        passesUserFilter = false;
                    }
                }
            }
            
            if (passesDateFilter && passesUserFilter) {
                filteredHistory.add(feedback);
            }
        }
    }

    private void updateStatistics() {
        List<FeedbackRequest> statsList = isFilterActive ? filteredHistory : feedbackHistory;
        
        if (statsList.isEmpty()) {
            statisticsContainer.setVisibility(View.GONE);
            return;
        }
        
        double totalEarnings = 0;
        double totalDistance = 0;
        
        for (FeedbackRequest feedback : statsList) {
            totalEarnings += feedback.getPrice();
            totalDistance += feedback.getDistance();
        }
        
        txtTotalEarnings.setText(String.format("%.0f din", totalEarnings));
        txtTotalDistance.setText(String.format("%.1f km", totalDistance));
        statisticsContainer.setVisibility(View.VISIBLE);
    }

    private void updateEarningsLabel() {
        if (userType != null) {
            if (userType.equals("driver")) {
                txtEarningsLabel.setText("Total Earnings");
            } else {
                txtEarningsLabel.setText("Total Spent");
            }
        }
    }

    private void updateUserLabels() {
        if (userType != null) {
            if (userType.equals("driver")) {
                txtUserLabel.setText("Passenger:");
                txtUserInfo.setText("ℹ️ Shows only passengers you've driven.");
            } else {
                txtUserLabel.setText("Driver:");
                txtUserInfo.setText("ℹ️ Shows only drivers who drove you.");
            }
        }
    }

    private void refreshUserList() {
        rememberSpinnerSelection();
        
        userListLoaded = false;
        userList.clear();
        loadUserList();
        ToastUtils.showInfo(this, "User list refreshed");
    }

    private void restoreSpinnerSelection() {
        if (lastSelectedSpinnerPosition >= 0 && lastSelectedSpinnerPosition < spinnerUserFilter.getCount()) {
            spinnerUserFilter.setSelection(lastSelectedSpinnerPosition);
        }
    }

    private void rememberSpinnerSelection() {
        int currentPosition = spinnerUserFilter.getSelectedItemPosition();
        if (currentPosition >= 0) {
            lastSelectedSpinnerPosition = currentPosition;
        }
    }

    private void determineUserType() {
        if (currentUser == null) {
            ToastUtils.showError(this, "❌ User not authenticated");
            finish();
            return;
        }

        database.getReference("users").child(currentUser.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            User user = dataSnapshot.getValue(User.class);
                            if (user != null) {
                                userType = user.getUserType();
                                updateEarningsLabel();
                                updateUserLabels();
                                loadRideHistory();
                                if (!userListLoaded) {
                                    loadUserList();
                                }
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

        FirebaseHelper.getFeedbackRequestsRef()
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        feedbackHistory.clear();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            FeedbackRequest feedback = snapshot.getValue(FeedbackRequest.class);
                            if (feedback != null) {
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

        List<FeedbackRequest> displayList = isFilterActive ? filteredHistory : feedbackHistory;

        if (displayList.isEmpty()) {
            emptyStateContainer.setVisibility(View.VISIBLE);
            rideHistoryContainer.setVisibility(View.GONE);
            statisticsContainer.setVisibility(View.GONE);
            return;
        }

        emptyStateContainer.setVisibility(View.GONE);
        rideHistoryContainer.setVisibility(View.VISIBLE);
        
        updateStatistics();

        Map<String, List<FeedbackRequest>> feedbackByDate = new HashMap<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd / MM / yyyy", Locale.getDefault());

        for (FeedbackRequest feedback : displayList) {
            String date = dateFormat.format(new Date(feedback.getTimestamp()));
            if (!feedbackByDate.containsKey(date)) {
                feedbackByDate.put(date, new ArrayList<>());
            }
            feedbackByDate.get(date).add(feedback);
        }

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
        int marginInDp = 16;
        int marginInPx = (int) (marginInDp * getResources().getDisplayMetrics().density);
        layoutParams.setMargins(0, 0, 0, marginInPx);
        itemView.setLayoutParams(layoutParams);

        TextView txtTripDate = itemView.findViewById(R.id.txtTripDate);
        TextView txtTripPrice = itemView.findViewById(R.id.txtTripPrice);
        TextView txtPickupLocation = itemView.findViewById(R.id.txtPickupLocation);
        TextView txtDropoffLocation = itemView.findViewById(R.id.txtDropoffLocation);
        ImageView starSingleItem = itemView.findViewById(R.id.starSingleItem);
        TextView txtRating = itemView.findViewById(R.id.txtRating);
        Button btnAction = itemView.findViewById(R.id.btnAction);

        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault());
        String formattedDateTime = dateTimeFormat.format(new Date(feedback.getTimestamp()));
        txtTripDate.setText(formattedDateTime);

        if (feedback.getPrice() > 0) {
            txtTripPrice.setText(String.format("%.0f din", feedback.getPrice()));
        } else {
            txtTripPrice.setText("N/A");
        }

        txtPickupLocation.setText(feedback.getPickupAddress());
        txtDropoffLocation.setText(feedback.getDropoffAddress());

        if (feedback.isSubmitted()) {
            txtRating.setText(String.valueOf(feedback.getRating()));
            starSingleItem.setVisibility(View.VISIBLE);
            btnAction.setText("VIEW FEEDBACK");
            btnAction.setEnabled(true);
            btnAction.setAlpha(1.0f);
            btnAction.setOnClickListener(v -> openFeedbackDetails(feedback));
        } else {
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
                for (ImageView star : stars) {
                    star.clearAnimation();
                    star.startAnimation(pulseAnimation);
                }
                return;
            }
            
            if (comment.length() < AppConfig.MIN_FEEDBACK_CHARACTERS) {
                ToastUtils.showError(HistoryScreen.this, 
                    String.format("Comment must be at least %d characters. Current: %d characters",
                            AppConfig.MIN_FEEDBACK_CHARACTERS, comment.length()));
                etComments.requestFocus();
                return;
            }
            
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
        feedback.setRating(rating);
        feedback.setComment(comment);
        feedback.setSubmitted(true);

        Log.d(TAG, "Submitting feedback: " + feedback.getFeedbackId() + " with rating: " + rating);
        
        FirebaseHelper.getFeedbackRequestsRef().child(feedback.getFeedbackId()).setValue(feedback)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Feedback submitted successfully");
                    
                    updateUserStatisticsAfterFeedback(feedback, rating);
                    
                    ToastUtils.showSuccess(this, "Feedback submitted successfully!");
                    displayRideHistory();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to submit feedback: " + e.getMessage());
                    ToastUtils.showError(this, "Failed to submit feedback. " + e.getMessage());
                });
    }

    private void updateUserStatisticsAfterFeedback(FeedbackRequest feedback, int rating) {
        if ("passenger".equals(userType)) {
            updatePassengerStatistics(feedback.getPassengerId(), feedback);
            updateDriverStatistics(feedback.getDriverId(), rating, feedback);
        } else {
            updateDriverStatistics(feedback.getDriverId(), 0, feedback);
            updatePassengerStatistics(feedback.getPassengerId(), feedback);
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
                                double feedbackDistance = feedback.getDistance();
                                int actualDistance = (int) Math.round(feedbackDistance);
                                double tripPrice = feedback.getPrice();
                                
                                if (actualDistance <= 0) {
                                    actualDistance = Math.max(1, (int) (tripPrice / 50));
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
                                double feedbackDistance = feedback.getDistance();
                                int actualDistance = (int) Math.round(feedbackDistance);
                                double tripPrice = feedback.getPrice();
                                
                                if (actualDistance <= 0) {
                                    actualDistance = Math.max(1, (int) (tripPrice / 50));
                                    Log.d(TAG, "Distance was 0, calculated from price: " + actualDistance + " km");
                                }
                                
                                Log.d(TAG, "Feedback submission - driver stats, Rating: " + rating);
                                
                                if (rating > 0) {
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