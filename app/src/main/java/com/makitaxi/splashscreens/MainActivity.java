package com.makitaxi.splashscreens;

import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.makitaxi.R;
import com.makitaxi.utils.NavigationClickListener;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.home_screen);
        addButtonListener();
    }

    private void addButtonListener() {
        Button button = findViewById(R.id.takeSurveyButton);
        if (button != null) {
            button.setOnClickListener(
                    NavigationClickListener.navigateTo(this, SplashScreen1.class)
            );
        }
    }

}