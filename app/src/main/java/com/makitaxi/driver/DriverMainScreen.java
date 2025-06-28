package com.makitaxi.driver;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.makitaxi.R;
import com.makitaxi.login.Login;
import com.makitaxi.utils.NavigationClickListener;

public class DriverMainScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.driver_main_screen);
        //addButtonListener();

        // Handle system bars (status bar and navigation bar)
        handleSystemBars();

        // Handle back button press with modern approach
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Move app to background instead of going back to login
                moveTaskToBack(true);
            }
        });

        Button btnLogout = findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(DriverMainScreen.this, Login.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }

    private void addButtonListener() {
        Button button = findViewById(R.id.takeSurveyButton);
        if (button != null) {
            button.setOnClickListener(
                    NavigationClickListener.navigateTo(this, DriverMainScreen.class)
            );
        }
    }

    /**
     * Handle System Bars (Status Bar and Navigation Bar)
     * This ensures content is not covered by system UI elements
     */
    private void handleSystemBars() {
        // Get the root view
        View rootView = findViewById(android.R.id.content);
        
        // Set up window insets listener to handle system bars
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            // Get system bars insets (status bar, navigation bar)
            androidx.core.graphics.Insets systemBars = insets.getInsets(
                androidx.core.view.WindowInsetsCompat.Type.systemBars()
            );
            
            // Apply padding to avoid content being covered by system bars
            // Top padding for status bar, bottom padding for navigation bar
            v.setPadding(
                systemBars.left,    // Left padding (usually 0)
                systemBars.top,     // Top padding (status bar)
                systemBars.right,   // Right padding (usually 0)
                systemBars.bottom   // Bottom padding (navigation bar)
            );
            
            return insets;
        });
    }

}