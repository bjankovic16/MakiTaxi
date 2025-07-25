package com.makitaxi.splashscreens;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.makitaxi.login.Login;
import com.makitaxi.R;
import com.makitaxi.utils.NavigationClickListener;
import com.makitaxi.utils.PreferencesManager;

@SuppressLint("CustomSplashScreen")
public class SplashScreen2 extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        PreferencesManager.setSplashScreensShown(this, true);
        setContentView(R.layout.splash_screen2);
        handleSystemBars();
        addButtonListener();
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
        Button button = findViewById(R.id.nextSurvey2);
        if (button != null) {
            button.setOnClickListener(
                    NavigationClickListener.navigateTo(this, SplashScreen3.class)
            );
        }
        TextView skipSurvey = findViewById(R.id.skipSurvey2);
        if (skipSurvey != null) {
            skipSurvey.setOnClickListener(NavigationClickListener.navigateTo(this, Login.class));
        }
    }

}