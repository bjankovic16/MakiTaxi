package com.makitaxi.utils;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FirebaseHelper {

    private static final String BASE_URL = "https://makitaxi-e4108-default-rtdb.europe-west1.firebasedatabase.app/";
    private static final DatabaseReference rootRef = FirebaseDatabase.getInstance(BASE_URL).getReference();

    public static DatabaseReference getRideRequestsRef() {
        return rootRef.child("ride_requests");
    }

    public static DatabaseReference getUserRequestsRef() {
        return rootRef.child("users");
    }
}
