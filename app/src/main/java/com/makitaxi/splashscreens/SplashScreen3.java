package com.makitaxi.splashscreens;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.makitaxi.login.Login;
import com.makitaxi.R;
import com.makitaxi.utils.NavigationClickListener;

@SuppressLint("CustomSplashScreen")
public class SplashScreen3 extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.splash_screen3);
        addButtonListener();
    }

    private void addButtonListener() {
        Button button = findViewById(R.id.nextSurvey3);
        if (button != null) {
            button.setOnClickListener(
                    NavigationClickListener.navigateTo(this, Login.class)
            );
        }
    }

}