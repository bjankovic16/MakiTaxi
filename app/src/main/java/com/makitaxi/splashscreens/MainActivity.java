package com.makitaxi.splashscreens;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.makitaxi.R;
import com.makitaxi.login.Login;
import com.makitaxi.utils.NavigationClickListener;
import com.makitaxi.utils.PreferencesManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (PreferencesManager.haveSplashScreensBeenShown(this)) {
            navigateToLogin();
        } else {
            EdgeToEdge.enable(this);
            setContentView(R.layout.home_screen);
            addButtonListener();
        }
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
}