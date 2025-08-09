package com.makitaxi.menu;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.makitaxi.R;
import com.makitaxi.model.FeedbackRequest;

public class FeedbackDetailsScreen extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView txtPickupLocation;
    private TextView txtDropoffLocation;
    private ImageView imgCarType;
    private TextView txtCarType;
    private TextView txtPrice;
    private TextView txtRating;
    private TextView txtComments;
    private TextView txtDriverPassengerLabel;
    private TextView txtDriverPassengerName;
    private String currentUserType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.feedback_details_screen);

        currentUserType = getIntent().getStringExtra("userType");

        initializeViews();
        setupUIInteractions();
        loadFeedbackData();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        txtPickupLocation = findViewById(R.id.txtPickupLocation);
        txtDropoffLocation = findViewById(R.id.txtDropoffLocation);
        imgCarType = findViewById(R.id.imgCarType);
        txtCarType = findViewById(R.id.txtCarType);
        txtPrice = findViewById(R.id.txtPrice);
        txtRating = findViewById(R.id.txtRating);
        txtComments = findViewById(R.id.txtComments);
        txtDriverPassengerLabel = findViewById(R.id.txtDriverPassengerLabel);
        txtDriverPassengerName = findViewById(R.id.txtDriverPassengerName);
    }

    private void setupUIInteractions() {
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadFeedbackData() {
        FeedbackRequest feedback = (FeedbackRequest) getIntent().getSerializableExtra("feedback");
        
        if (feedback != null) {
            txtPickupLocation.setText(feedback.getPickupAddress());
            txtDropoffLocation.setText(feedback.getDropoffAddress());
            
            // Set car type and icon based on car type
            String carType = feedback.getCarType();
            txtCarType.setText(carType);
            setCarTypeIcon(carType);
            
            txtPrice.setText(String.format("%.0f din", feedback.getPrice()));
            
            int rating = feedback.getRating();
            txtRating.setText(rating + " out of 5");
            
            // Set driver/passenger name and label based on current user type
            String driverName = feedback.getDriverName();
            String passengerName = feedback.getPassengerName();
            
            if ("passenger".equals(currentUserType)) {
                // If logged in as passenger, show driver name
                txtDriverPassengerLabel.setText("Driver");
                if (driverName != null && !driverName.trim().isEmpty()) {
                    txtDriverPassengerName.setText(driverName);
                } else {
                    txtDriverPassengerName.setText("Unknown");
                }
            } else {
                // If logged in as driver, show passenger name
                txtDriverPassengerLabel.setText("Passenger");
                if (passengerName != null && !passengerName.trim().isEmpty()) {
                    txtDriverPassengerName.setText(passengerName);
                } else {
                    txtDriverPassengerName.setText("Unknown");
                }
            }
            
            String comments = feedback.getComment();
            if (comments != null && !comments.trim().isEmpty()) {
                txtComments.setText(comments);
            } else {
                txtComments.setText("No comments provided");
            }
        }
    }
    
    private void setCarTypeIcon(String carType) {
        if (carType == null) {
            imgCarType.setImageResource(R.drawable.basic_car);
            return;
        }
        
        switch (carType.toLowerCase()) {
            case "luxury":
            case "lux":
                imgCarType.setImageResource(R.drawable.lux_car_background);
                break;
            case "transport":
                imgCarType.setImageResource(R.drawable.transport_car_background);
                break;
            case "basic":
            default:
                imgCarType.setImageResource(R.drawable.basic_car_background);
                break;
        }
    }
} 