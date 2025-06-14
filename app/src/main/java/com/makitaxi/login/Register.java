package com.makitaxi.login;

import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.makitaxi.R;
import com.makitaxi.utils.NavigationClickListener;

public class Register extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.register);
        addButtonListeners();
    }

    private void addButtonListeners() {
        Button button = findViewById(R.id.buttonSignInFromSingUp);
        if (button != null) {
            button.setOnClickListener(
                    NavigationClickListener.navigateTo(this, Login.class)
            );
        }
    }

}