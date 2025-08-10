package com.makitaxi.menu;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.makitaxi.R;
import com.makitaxi.login.Login;
import com.makitaxi.utils.CircularImageView;
import com.makitaxi.utils.ImageUploadHelper;
import com.makitaxi.utils.PreferencesManager;
import com.makitaxi.model.User;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class MenuMainScreen extends AppCompatActivity {

    private static final String TAG = "MenuMainScreen";

    // UI Components
    private ImageButton btnCloseMenu;
    private CircularImageView imgProfilePicture;
    private ImageButton btnEditProfilePicture;
    private ProgressBar progressBarUpload;
    private TextView txtUserName;
    private TextView txtUserRole;
    private TextView txtTotalRides;
    private TextView txtRating;
    private TextView txtTotalDistance;
    private LinearLayout layoutRating;
    private LinearLayout layoutHistory;
    private LinearLayout layoutMyAccount;
    private LinearLayout layoutLogOut;

    private FirebaseAuth auth;
    private FirebaseDatabase database;
    private ImageUploadHelper imageUploadHelper;
    
    // Image picker launcher
    private ActivityResultLauncher<String> imagePickerLauncher;
    // Permission request launcher
    private ActivityResultLauncher<String> permissionLauncher;
    
    private String currentProfileImageUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.menu_main_screen);

        handleSystemBars();

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance("https://makitaxi-e4108-default-rtdb.europe-west1.firebasedatabase.app/");
        imageUploadHelper = new ImageUploadHelper();

        initializeImagePicker();
        initializeViews();
        setupUIInteractions();
        loadUserInfo();
    }

    @Override
    protected void onResume() {
        super.onResume();
        reloadUserName();
        reloadProfileImage();
    }

    private void reloadUserName() {
        String cachedName = PreferencesManager.getCachedUserName(this);
        if (cachedName != null && !cachedName.isEmpty()) {
            txtUserName.setText(cachedName);
        } else {
            FirebaseUser currentUser = auth.getCurrentUser();
            if (currentUser != null && currentUser.getDisplayName() != null) {
                txtUserName.setText(currentUser.getDisplayName());
            } else {
                txtUserName.setText("User");
            }
        }
    }

    private void reloadProfileImage() {
        User cachedUser = PreferencesManager.getCachedUser(this);
        if (cachedUser != null && cachedUser.getProfilePicture() != null) {
            String profileImageUrl = cachedUser.getProfilePicture();
            if (!profileImageUrl.isEmpty()) {
                currentProfileImageUrl = profileImageUrl;
                loadProfileImage(profileImageUrl);
            }
        }
    }

    private void initializeImagePicker() {
        // Image picker launcher
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    uploadProfileImage(uri);
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
        btnCloseMenu = findViewById(R.id.btnCloseMenu);
        imgProfilePicture = findViewById(R.id.imgProfilePicture);
        btnEditProfilePicture = findViewById(R.id.btnEditProfilePicture);
        progressBarUpload = findViewById(R.id.progressBarUpload);
        txtUserName = findViewById(R.id.txtUserName);
        txtUserRole = findViewById(R.id.txtUserRole);
        txtTotalRides = findViewById(R.id.txtTotalRides);
        txtRating = findViewById(R.id.txtRating);
        txtTotalDistance = findViewById(R.id.txtTotalDistance);
        layoutRating = findViewById(R.id.layoutRating);
        layoutHistory = findViewById(R.id.layoutHistory);
        layoutMyAccount = findViewById(R.id.layoutMyAccount);
        layoutLogOut = findViewById(R.id.layoutLogOut);
    }

    private void setupUIInteractions() {
        btnCloseMenu.setOnClickListener(v -> closeMenu());

        btnEditProfilePicture.setOnClickListener(v -> showImagePickerDialog());

        layoutHistory.setOnClickListener(v -> {
            Intent intent = new Intent(this, HistoryScreen.class);
            startActivity(intent);
        });

        layoutMyAccount.setOnClickListener(v -> {
            Intent intent = new Intent(this, MyAccountScreen.class);
            startActivity(intent);
        });

        layoutLogOut.setOnClickListener(v -> handleLogout());
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
            Toast.makeText(this, "❌ Error opening image picker: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadProfileImage(Uri imageUri) {
        
        imageUploadHelper.uploadProfileImageSimple(this, imageUri, new ImageUploadHelper.ImageUploadListener() {
            @Override
            public void onUploadStart() {
                runOnUiThread(() -> {
                    progressBarUpload.setVisibility(View.VISIBLE);
                    progressBarUpload.setProgress(0);
                    btnEditProfilePicture.setEnabled(false);
                });
            }

            @Override
            public void onUploadSuccess(String imageUrl) {
                runOnUiThread(() -> {
                    progressBarUpload.setVisibility(View.GONE);
                    btnEditProfilePicture.setEnabled(true);
                    currentProfileImageUrl = imageUrl;
                    loadProfileImage(imageUrl);
                    
                    // Update cached user object with new profile picture
                    User cachedUser = PreferencesManager.getCachedUser(MenuMainScreen.this);
                    if (cachedUser != null) {
                        cachedUser.setProfilePicture(imageUrl);
                        PreferencesManager.updateCachedUser(MenuMainScreen.this, cachedUser);
                    }
                    
                    Toast.makeText(MenuMainScreen.this, "✅ Profile picture updated!", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onUploadError(String error) {
                runOnUiThread(() -> {
                    progressBarUpload.setVisibility(View.GONE);
                    btnEditProfilePicture.setEnabled(true);
                    Toast.makeText(MenuMainScreen.this, "❌ Upload failed: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void closeMenu() {
        finish();
    }

    private void loadUserInfo() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            Log.d(TAG, "Loading user info for userId: " + userId);
            
            database.getReference("users").child(userId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    Log.d(TAG, "User data snapshot exists: " + dataSnapshot.exists());
                    if (dataSnapshot.exists()) {
                        try {
                            String fullName = dataSnapshot.child("fullName").getValue(String.class);
                            String profilePictureUrl = dataSnapshot.child("profilePicture").getValue(String.class);
                            
                            Log.d(TAG, "Full name: " + fullName);
                            
                            if (fullName != null && !fullName.isEmpty()) {
                                txtUserName.setText(fullName);
                            } else {
                                String displayName = currentUser.getDisplayName();
                                if (displayName != null && !displayName.isEmpty()) {
                                    txtUserName.setText(displayName);
                                } else {
                                    txtUserName.setText("User");
                                }
                            }
                            
                            currentProfileImageUrl = profilePictureUrl;
                            loadProfileImage(profilePictureUrl);
                            
                            // Set user role and configure UI based on user type
                            String userType = dataSnapshot.child("role").getValue(String.class);
                            Log.d(TAG, "User type: " + userType);
                            boolean isDriver = "DRIVER".equals(userType);
                            
                            if (userType != null && !userType.isEmpty()) {
                                txtUserRole.setText(userType.substring(0, 1).toUpperCase() + userType.substring(1).toLowerCase());
                            } else {
                                txtUserRole.setText("Passenger"); // Default
                            }
                            
                            // Show/hide rating section based on user type
                            if (layoutRating != null) {
                                layoutRating.setVisibility(isDriver ? View.VISIBLE : View.GONE);
                            }
                            
                            // Load user statistics from User object
                            Integer totalRides = dataSnapshot.child("totalRides").getValue(Integer.class);
                            Double rating = dataSnapshot.child("rating").getValue(Double.class);
                            Integer totalDistance = dataSnapshot.child("totalDistance").getValue(Integer.class);
                            
                            Log.d(TAG, "Statistics - Rides: " + totalRides + ", Rating: " + rating + ", Distance: " + totalDistance);
                            
                            txtTotalRides.setText(String.valueOf(totalRides != null ? totalRides : 0));
                            txtRating.setText(String.format("%.1f", rating != null ? rating : 0.0));
                            txtTotalDistance.setText(String.valueOf(totalDistance != null ? totalDistance : 0));
                            
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing user data: " + e.getMessage(), e);
                            // Set default values
                            txtUserName.setText("User");
                            txtUserRole.setText("Passenger");
                            txtTotalRides.setText("0");
                            txtRating.setText("0.0");
                            txtTotalDistance.setText("0");
                            // Hide rating for passengers by default
                            if (layoutRating != null) {
                                layoutRating.setVisibility(View.GONE);
                            }
                        }
                    } else {
                        Log.w(TAG, "User data does not exist in database");
                        // Set default values
                        txtUserName.setText("User");
                        txtUserRole.setText("Passenger");
                        txtTotalRides.setText("0");
                        txtRating.setText("0.0");
                        txtTotalDistance.setText("0");
                        // Hide rating for passengers by default
                        if (layoutRating != null) {
                            layoutRating.setVisibility(View.GONE);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e(TAG, "Failed to load user info: " + databaseError.getMessage());
                    Toast.makeText(MenuMainScreen.this, "❌ Failed to load user info", Toast.LENGTH_SHORT).show();
                    // Set default values
                    txtUserName.setText("User");
                    txtUserRole.setText("Passenger");
                    txtTotalRides.setText("0");
                    txtRating.setText("0.0");
                    txtTotalDistance.setText("0");
                    // Hide rating for passengers by default
                    if (layoutRating != null) {
                        layoutRating.setVisibility(View.GONE);
                    }
                }
            });
        } else {
            Log.w(TAG, "No current user found, navigating to login");
            navigateToLogin();
        }
    }

    private void loadProfileImage(String imageUrl) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            progressBarUpload.setVisibility(View.VISIBLE);

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
                } finally {
                    progressBarUpload.setVisibility(View.GONE);
                }
            } else {
                Glide.with(this)
                        .load(imageUrl)
                        .error(R.drawable.taxi_logo)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                progressBarUpload.setVisibility(View.GONE);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                progressBarUpload.setVisibility(View.GONE);
                                return false;
                            }
                        })
                        .into(imgProfilePicture);
            }
        } else {
            imgProfilePicture.setImageResource(R.drawable.taxi_logo);
        }
    }

    private void handleLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    auth.signOut();
                    PreferencesManager.clearUserSession(this);
                    navigateToLogin();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, Login.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }



    @Override
    public void onBackPressed() {
        super.onBackPressed();
        closeMenu();
    }
}
