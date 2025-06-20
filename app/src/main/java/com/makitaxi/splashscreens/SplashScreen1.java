package com.makitaxi.splashscreens;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.makitaxi.login.Login;
import com.makitaxi.R;
import com.makitaxi.utils.NavigationClickListener;

@SuppressLint("CustomSplashScreen")
public class SplashScreen1 extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.splash_screen1);
        addButtonListeners();
    }

    private void addButtonListeners() {
        Button button = findViewById(R.id.nextSurvey1);
        if (button != null) {
            button.setOnClickListener(
                    NavigationClickListener.navigateTo(this, SplashScreen2.class)
            );
        }
        TextView skipSurvey = findViewById(R.id.skipSurvey1);
        if (skipSurvey != null) {
            skipSurvey.setOnClickListener(NavigationClickListener.navigateTo(this, Login.class));
        }
    }

}