package com.makitaxi.menu;

import android.app.DatePickerDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.makitaxi.R;
import com.makitaxi.model.User;
import com.makitaxi.utils.CircularImageView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

public class MyAccountScreen extends AppCompatActivity {

    private static final String TAG = "MyAccountScreen";

    // UI Components
    private ImageButton btnBack;
    private CircularImageView imgProfilePicture;
    private TextView txtUserName;
    private TextView txtUserEmail;
    private EditText editTextName;
    private Spinner spinnerGender;
    private LinearLayout layoutBirthday;
    private TextView txtBirthday;
    private EditText editTextPhone;
    private Button btnSave;

    // Add these fields to the class variables section
    private LinearLayout carTypeContainer;
    private LinearLayout carDetailsContainer;
    private Spinner spinnerCarType;
    private EditText editTextCarDetails;

    // Firebase
    private FirebaseAuth auth;
    private FirebaseDatabase database;
    
    // Data
    private String currentUserId;
    private final Calendar birthdayCalendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.my_account_screen);

        handleSystemBars();
        
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance("https://makitaxi-e4108-default-rtdb.europe-west1.firebasedatabase.app/");

        initializeViews();
        setupGenderSpinner();
        setupUIInteractions();
        loadUserInfo();
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
        btnBack = findViewById(R.id.btnBack);
        imgProfilePicture = findViewById(R.id.imgProfilePicture);
        txtUserName = findViewById(R.id.txtUserName);
        txtUserEmail = findViewById(R.id.txtUserEmail);
        editTextName = findViewById(R.id.editTextName);
        spinnerGender = findViewById(R.id.spinnerGender);
        layoutBirthday = findViewById(R.id.layoutBirthday);
        txtBirthday = findViewById(R.id.txtBirthday);
        editTextPhone = findViewById(R.id.editTextPhone);
        btnSave = findViewById(R.id.btnSave);
        
        // Initialize car-related views
        carTypeContainer = findViewById(R.id.carTypeContainer);
        carDetailsContainer = findViewById(R.id.carDetailsContainer);
        spinnerCarType = findViewById(R.id.spinnerCarType);
        editTextCarDetails = findViewById(R.id.editTextCarDetails);
    }

    private void setupGenderSpinner() {
        String[] genderOptions = {"Male", "Female", "Other", "Prefer not to say"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, genderOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGender.setAdapter(adapter);
    }

    private void setupCarTypeSpinner() {
        String[] carTypes = {"BASIC", "LUXURY", "TRANSPORT"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, carTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCarType.setAdapter(adapter);
    }

    private void setupUIInteractions() {
        btnBack.setOnClickListener(v -> finish());

        layoutBirthday.setOnClickListener(v -> showDatePicker());

        btnSave.setOnClickListener(v -> saveUserInfo());
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    birthdayCalendar.set(Calendar.YEAR, year);
                    birthdayCalendar.set(Calendar.MONTH, month);
                    birthdayCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateBirthdayDisplay();
                },
                birthdayCalendar.get(Calendar.YEAR),
                birthdayCalendar.get(Calendar.MONTH),
                birthdayCalendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void updateBirthdayDisplay() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
        txtBirthday.setText(dateFormat.format(birthdayCalendar.getTime()));
    }

    private void loadUserInfo() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            
            database.getReference("users").child(currentUserId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        User user = dataSnapshot.getValue(User.class);
                        if (user != null) {
                            populateUserData(user);
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e(TAG, "Failed to load user info", databaseError.toException());
                    Toast.makeText(MyAccountScreen.this, "❌ Failed to load user info", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            finish();
        }
    }

    private void populateUserData(User user) {
        // Header info
        if (user.getFullName() != null && !user.getFullName().isEmpty()) {
            txtUserName.setText(user.getFullName());
            editTextName.setText(user.getFullName());
        }
        
        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            txtUserEmail.setText(user.getEmail());
        }

        // Phone
        if (user.getPhone() != null && !user.getPhone().isEmpty()) {
            editTextPhone.setText(user.getPhone());
        }

        // Gender
        if (user.getGender() != null && !user.getGender().isEmpty()) {
            setSpinnerSelection(spinnerGender, user.getGender());
        }

        // Birthday
        if (user.getBirthday() != null && !user.getBirthday().isEmpty()) {
            txtBirthday.setText(user.getBirthday());
        }

        // Profile picture
        loadProfileImage(user.getProfilePicture());

        if ("DRIVER".equals(user.getRole())) {
            carTypeContainer.setVisibility(View.VISIBLE);
            carDetailsContainer.setVisibility(View.VISIBLE);
            
            setupCarTypeSpinner();
            
            if (user.getCarType() != null && !user.getCarType().isEmpty()) {
                setSpinnerSelection(spinnerCarType, user.getCarType());
            }
            
            String carDetails = String.format("%s - %s (%s)",
                user.getCarModel() != null ? user.getCarModel() : "",
                user.getCarColor() != null ? user.getCarColor() : "",
                user.getCarPlateNumber() != null ? user.getCarPlateNumber() : ""
            ).trim();
            
            if (!carDetails.equals("- ()")) {
                editTextCarDetails.setText(carDetails);
            }
        } else {
            carTypeContainer.setVisibility(View.GONE);
            carDetailsContainer.setVisibility(View.GONE);
        }
    }

    private void setSpinnerSelection(Spinner spinner, String value) {
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).toString().equalsIgnoreCase(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private void loadProfileImage(String imageUrl) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            if (imageUrl.startsWith("data:image/")) {
                try {
                    String base64Data = imageUrl.substring(imageUrl.indexOf(",") + 1);
                    byte[] decodedBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                    if (bitmap != null) {
                        imgProfilePicture.setImageBitmap(bitmap);
                    } else {
                        imgProfilePicture.setImageResource(R.drawable.taxi_logo);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error decoding Base64 image", e);
                    imgProfilePicture.setImageResource(R.drawable.taxi_logo);
                }
            } else {
                // Handle regular URL
                Glide.with(this)
                        .load(imageUrl)
                        .placeholder(R.drawable.taxi_logo)
                        .error(R.drawable.taxi_logo)
                        .into(imgProfilePicture);
            }
        } else {
            imgProfilePicture.setImageResource(R.drawable.taxi_logo);
        }
    }

    private void saveUserInfo() {
        if (currentUserId == null) {
            Toast.makeText(this, "❌ User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = editTextName.getText().toString().trim();
        String phone = editTextPhone.getText().toString().trim();
        String gender = spinnerGender.getSelectedItem().toString();
        String birthday = txtBirthday.getText().toString();
        if (name.isEmpty()) {
            editTextName.setError("Name is required");
            return;
        }

        if (phone.isEmpty()) {
            editTextPhone.setError("Phone number is required");
            return;
        }

        if (birthday.equals("Select your birthday")) {
            Toast.makeText(this, "❌ Please select your birthday", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", name);
        updates.put("phone", phone);
        updates.put("gender", gender);
        updates.put("birthday", birthday);
        
        if (carTypeContainer.getVisibility() == View.VISIBLE) {
            String carType = spinnerCarType.getSelectedItem().toString();
            String carDetailsText = editTextCarDetails.getText().toString().trim();
            
            if (carDetailsText.isEmpty()) {
                editTextCarDetails.setError("Please enter car details");
                return;
            }

            try {
                String[] parts = carDetailsText.split(" - ");
                String model = parts[0].trim();
                String[] colorAndPlate = parts[1].split("\\(");
                String color = colorAndPlate[0].trim();
                String plate = colorAndPlate[1].replace(")", "").trim();

                updates.put("carType", carType);
                updates.put("carModel", model);
                updates.put("carColor", color);
                updates.put("carPlateNumber", plate);
            } catch (Exception e) {
                editTextCarDetails.setError("Please use format: Model - Color (Plate)");
                return;
            }
        }

        database.getReference("users")
            .child(currentUserId)
            .updateChildren(updates)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(MyAccountScreen.this, "✅ Profile updated successfully", Toast.LENGTH_SHORT).show();
                finish();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(MyAccountScreen.this, "❌ Failed to update profile", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error updating user", e);
            });
    }
} 