package com.makitaxi.splashscreens;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.makitaxi.login.Login;
import com.makitaxi.R;
import com.makitaxi.utils.NavigationClickListener;

public class SplashScreen2 extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.splash_screen2);
        addButtonListener();
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