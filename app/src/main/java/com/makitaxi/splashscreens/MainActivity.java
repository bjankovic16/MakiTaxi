package com.makitaxi.splashscreens;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;
import com.makitaxi.R;
import com.makitaxi.driver.DriverMainScreen;
import com.makitaxi.login.Login;
import com.makitaxi.passenger.PassengerScreen;
import com.makitaxi.utils.NavigationClickListener;
import com.makitaxi.utils.PreferencesManager;

import org.osmdroid.config.Configuration;

public class MainActivity extends AppCompatActivity {
    
    private static final String TAG = "MainActivity";
    private static final String DRIVER = "DRIVER";
    private static final String PASSENGER = "PASSENGER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        handleSystemBars();
        initializeOSMDroid();
        
        Log.d(TAG, "MainActivity started - checking session");
        
        if (PreferencesManager.isSessionValid(this)) {
            Log.d(TAG, "Valid session found - checking Firebase auth");
            checkFirebaseAuthAndNavigate();
        } else if (PreferencesManager.haveSplashScreensBeenShown(this)) {
            Log.d(TAG, "No valid session - navigating to login");
            navigateToLogin();
        } else {
            Log.d(TAG, "First time user - showing splash screens");
            setContentView(R.layout.home_screen);
            addButtonListener();
        }
    }

    private void checkFirebaseAuthAndNavigate() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();
        
        if (currentUser != null) {
            String userEmail = PreferencesManager.getUserEmail(this);
            Log.d(TAG, "Firebase user authenticated: " + userEmail);
            
            PreferencesManager.refreshSession(this);
            
            getUserRoleAndNavigate(userEmail);
        } else {
            Log.d(TAG, "No Firebase authentication - clearing session");
            PreferencesManager.clearUserSession(this);
            navigateToLogin();
        }
    }

    private void getUserRoleAndNavigate(String email) {
        if (email == null) {
            Log.w(TAG, "Email is null - navigating to login");
            navigateToLogin();
            return;
        }
        
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance(
            "https://makitaxi-e4108-default-rtdb.europe-west1.firebasedatabase.app/"
        );
        
        firebaseDatabase.getReference("users")
            .orderByChild("email")
            .equalTo(email)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult().exists()) {
                    DataSnapshot snapshot = task.getResult().getChildren().iterator().next();
                    String role = snapshot.child("role").getValue(String.class);
                    boolean verified = Boolean.TRUE.equals(snapshot.child("verified").getValue(Boolean.class));
                    
                    Log.d(TAG, "User role: " + role + ", verified: " + verified);
                    
                    if (DRIVER.equals(role) && verified) {
                        navigateToDriverScreen();
                    } else if (PASSENGER.equals(role)) {
                        navigateToPassengerScreen();
                    } else {
                        Log.w(TAG, "Driver not verified or invalid role - navigating to login");
                        navigateToLogin();
                    }
                } else {
                    Log.w(TAG, "User not found in database - clearing session");
                    PreferencesManager.clearUserSession(this);
                    navigateToLogin();
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to get user role", e);
                navigateToLogin();
            });
    }

    private void initializeOSMDroid() {
        try {
            Configuration.getInstance().setUserAgentValue(getPackageName());
            Configuration.getInstance().setOsmdroidBasePath(getCacheDir());
            Configuration.getInstance().setOsmdroidTileCache(getCacheDir());
            Log.d(TAG, "OSMDroid initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing OSMDroid", e);
        }
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

    private void addButtonListener() {
        Button button = findViewById(R.id.takeSurveyButton);
        if (button != null) {
            button.setOnClickListener(
                    NavigationClickListener.navigateTo(this, SplashScreen1.class)
            );
        }
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, Login.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    private void navigateToPassengerScreen() {
        Intent intent = new Intent(this, PassengerScreen.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    private void navigateToDriverScreen() {
        Intent intent = new Intent(this, DriverMainScreen.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}