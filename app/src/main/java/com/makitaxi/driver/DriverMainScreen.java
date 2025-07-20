package com.makitaxi.driver;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import com.makitaxi.R;
import com.makitaxi.menu.MenuMainScreen;
import org.osmdroid.views.MapView;


public class DriverMainScreen extends AppCompatActivity{

    // UI Components
    private MapView mapView;
    private ImageButton btnHamburgerMenu;
    private Button btnZoomIn;
    private Button btnZoomOut;
    private ImageButton btnMyLocation;
    private MapDriver map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.driver_main_screen);
        handleSystemBars();
        initializeViews();
        initializeControllers();
        setupUIInteractions();
    }

    private void initializeControllers() {
        map = new MapDriver(this, mapView);
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
        // Map
        mapView = findViewById(R.id.mapView);

        // Header components
        btnHamburgerMenu = findViewById(R.id.btnHamburgerMenu);

        btnZoomIn = findViewById(R.id.btnZoomIn);
        btnZoomOut = findViewById(R.id.btnZoomOut);
        btnMyLocation = findViewById(R.id.btnMyLocation);
    }

    private void setupUIInteractions() {
        btnZoomIn.setOnClickListener(v -> map.zoomIn());

        btnZoomOut.setOnClickListener(v -> map.zoomOut());

        btnMyLocation.setOnClickListener(v -> map.centerOnCurrentLocation());

        btnHamburgerMenu.setOnClickListener(v -> openHamburgerMenu());
    }

    private void openHamburgerMenu() {
        Intent intent = new Intent(this, MenuMainScreen.class);
        startActivity(intent);

        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
