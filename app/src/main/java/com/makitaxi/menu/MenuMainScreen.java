package com.makitaxi.menu;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
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
    private TextView txtUserEmail;
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

    private void initializeImagePicker() {
        // Image picker launcher
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    uploadProfileImage(uri);
                } else {
                    Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(this, "Storage permission is required to select images", Toast.LENGTH_LONG).show();
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
        txtUserEmail = findViewById(R.id.txtUserEmail);
        layoutHistory = findViewById(R.id.layoutHistory);
        layoutMyAccount = findViewById(R.id.layoutMyAccount);
        layoutLogOut = findViewById(R.id.layoutLogOut);
    }

    private void setupUIInteractions() {
        btnCloseMenu.setOnClickListener(v -> closeMenu());

        btnEditProfilePicture.setOnClickListener(v -> showImagePickerDialog());

        layoutHistory.setOnClickListener(v -> {
            Toast.makeText(this, "📜 History feature coming soon", Toast.LENGTH_SHORT).show();
        });

        layoutMyAccount.setOnClickListener(v -> {
            Toast.makeText(this, "👤 My Account feature coming soon", Toast.LENGTH_SHORT).show();
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
            // Android 13+ uses READ_MEDIA_IMAGES
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES) 
                    == PackageManager.PERMISSION_GRANTED) {
                launchImagePicker();
            } else {
                permissionLauncher.launch(android.Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            // Android 12 and below use READ_EXTERNAL_STORAGE
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
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void loadUserInfo() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            
            database.getReference("users").child(userId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        // Load user name
                        String fullName = dataSnapshot.child("fullName").getValue(String.class);
                        String email = dataSnapshot.child("email").getValue(String.class);
                        String profilePictureUrl = dataSnapshot.child("profilePicture").getValue(String.class);
                        
                        if (email != null) {
                            txtUserEmail.setText(email);
                        }
                        
                        if (fullName != null && !fullName.isEmpty()) {
                            txtUserName.setText(fullName);
                        } else {
                            // Fallback to display name or extract from email
                            String displayName = currentUser.getDisplayName();
                            if (displayName != null && !displayName.isEmpty()) {
                                txtUserName.setText(displayName);
                            }
                        }
                        
                        // Load profile picture
                        currentProfileImageUrl = profilePictureUrl;
                        loadProfileImage(profilePictureUrl);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(MenuMainScreen.this, "❌ Failed to load user info", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            navigateToLogin();
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
