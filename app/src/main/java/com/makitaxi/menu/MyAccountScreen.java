package com.makitaxi.menu;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

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
import com.makitaxi.utils.PreferencesManager;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

public class MyAccountScreen extends AppCompatActivity {

    private static final String TAG = "MyAccountScreen";

    // UI Components
    private ImageButton btnBack;
    private ImageButton btnEditPhoto;
    private CircularImageView imgProfilePicture;
    private TextView txtUserName;
    private TextView txtUserEmail;
    private TextView txtUserRole;
    private EditText editTextName;
    private Spinner spinnerGender;
    private LinearLayout layoutBirthday;
    private TextView txtBirthday;
    private EditText editTextPhone;
    private Button btnSave;

    // Driver-specific components
    private LinearLayout driverSection;
    private LinearLayout carTypeContainer;
    private LinearLayout carDetailsContainer;
    private Spinner spinnerCarType;
    private EditText editTextCarDetails;

    // Firebase
    private FirebaseAuth auth;
    private FirebaseDatabase database;
    
    // Data
    private String currentUserId;
    private String currentUserRole;
    private final Calendar birthdayCalendar = Calendar.getInstance();
    
    // Image handling
    private ActivityResultLauncher<String> imagePickerLauncher;
    private ActivityResultLauncher<String> permissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.my_account_screen);

        handleSystemBars();
        
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance("https://makitaxi-e4108-default-rtdb.europe-west1.firebasedatabase.app/");

        setupImageLaunchers();
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

    private void setupImageLaunchers() {
        // Image picker launcher
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    processSelectedImage(uri);
                } else {
                    Toast.makeText(this, "❌ No image selected", Toast.LENGTH_SHORT).show();
                }
            }
        );
        
        // Permission request launcher
        permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    launchImagePicker();
                } else {
                    Toast.makeText(this, "❌ Storage permission is required to select images", Toast.LENGTH_LONG).show();
                }
            }
        );
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        btnEditPhoto = findViewById(R.id.btnEditPhoto);
        imgProfilePicture = findViewById(R.id.imgProfilePicture);
        txtUserName = findViewById(R.id.txtUserName);
        txtUserEmail = findViewById(R.id.txtUserEmail);
        txtUserRole = findViewById(R.id.txtUserRole);
        editTextName = findViewById(R.id.editTextName);
        spinnerGender = findViewById(R.id.spinnerGender);
        layoutBirthday = findViewById(R.id.layoutBirthday);
        txtBirthday = findViewById(R.id.txtBirthday);
        editTextPhone = findViewById(R.id.editTextPhone);
        btnSave = findViewById(R.id.btnSave);
        
        // Initialize driver-specific views
        driverSection = findViewById(R.id.driverSection);
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

        btnEditPhoto.setOnClickListener(v -> showImagePickerDialog());

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

    private void showImagePickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Profile Picture")
                .setMessage("Choose an option")
                .setPositiveButton("Select from Gallery", (dialog, which) -> {
                    checkStoragePermissionAndLaunchPicker();
                })
                .setNeutralButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }
    
    private void checkStoragePermissionAndLaunchPicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED) {
                launchImagePicker();
            } else {
                permissionLauncher.launch(android.Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                launchImagePicker();
            } else {
                permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
    }
    
    private void launchImagePicker() {
        try {
            imagePickerLauncher.launch("image/*");
        } catch (Exception e) {
            Toast.makeText(this, "Error opening image picker: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void processSelectedImage(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
            bitmap = resizeBitmap(bitmap, 300, 300);
            imgProfilePicture.setImageBitmap(bitmap);
            saveProfileImage(bitmap);
        } catch (Exception e) {
            Log.e(TAG, "Error processing selected image", e);
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxWidth;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxHeight;
            width = (int) (height * bitmapRatio);
        }

        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }

    private void saveProfileImage(Bitmap bitmap) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] imageBytes = baos.toByteArray();
            String encodedImage = "data:image/jpeg;base64," + Base64.encodeToString(imageBytes, Base64.DEFAULT);

            if (currentUserId != null) {
                database.getReference("users").child(currentUserId)
                    .child("profilePicture")
                    .setValue(encodedImage)
                    .addOnSuccessListener(aVoid -> {
                        User cachedUser = PreferencesManager.getCachedUser(MyAccountScreen.this);
                        if (cachedUser != null) {
                            cachedUser.setProfilePicture(encodedImage);
                            PreferencesManager.updateCachedUser(MyAccountScreen.this, cachedUser);
                        }
                        Toast.makeText(this, "✅ Profile picture updated", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to save profile picture", e);
                        Toast.makeText(this, "❌ Failed to update picture", Toast.LENGTH_SHORT).show();
                    });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving profile image", e);
            Toast.makeText(this, "❌ Error saving image", Toast.LENGTH_SHORT).show();
        }
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
        // Store user role for later use
        currentUserRole = user.getRole();
        
        // Header info
        if (user.getFullName() != null && !user.getFullName().isEmpty()) {
            txtUserName.setText(user.getFullName());
            editTextName.setText(user.getFullName());
        }
        
        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            txtUserEmail.setText(user.getEmail());
        }

        // User role badge
        if (currentUserRole != null && !currentUserRole.isEmpty()) {
            String displayRole = currentUserRole.substring(0, 1).toUpperCase() + currentUserRole.substring(1).toLowerCase();
            txtUserRole.setText(displayRole);
        } else {
            txtUserRole.setText("Passenger");
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
        } else {
            txtBirthday.setText("Select date");
        }

        // Profile picture
        loadProfileImage(user.getProfilePicture());

        // Driver-specific information
        if ("DRIVER".equals(currentUserRole)) {
            driverSection.setVisibility(View.VISIBLE);
            
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
            driverSection.setVisibility(View.GONE);
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

        if (birthday.equals("Select date")) {
            Toast.makeText(this, "❌ Please select your birthday", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", name);
        updates.put("phone", phone);
        updates.put("gender", gender);
        updates.put("birthday", birthday);
        
        if (driverSection.getVisibility() == View.VISIBLE) {
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
                refreshUserDataCache();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(MyAccountScreen.this, "❌ Failed to update profile", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error updating user", e);
            });
    }
    
    private void refreshUserDataCache() {
        if (currentUserId != null) {
            database.getReference("users").child(currentUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            User user = snapshot.getValue(User.class);
                            if (user != null) {
                                PreferencesManager.cacheUser(MyAccountScreen.this, user);
                                Log.d(TAG, "User object cache refreshed");
                            }
                        }
                    }
                    
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to refresh user data cache: " + error.getMessage());
                    }
                });
        }
    }
} 