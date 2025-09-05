package com.makitaxi;

import android.app.Application;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

public class TestApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseOptions options = new FirebaseOptions.Builder()
                .setProjectId("test-project")
                .setApplicationId("test-app")
                .build();
            
            FirebaseApp.initializeApp(this, options);
        }
    }
}