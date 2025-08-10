package com.makitaxi.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import com.makitaxi.utils.ToastUtils;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.makitaxi.R;

public class PasswordChange extends AppCompatActivity {
    private EditText editTextInput;
    private Button buttonResetPassword;
    private FirebaseAuth mAuth;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference firebaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.password_change);
        handleSystemBars();

        mAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance("https://makitaxi-e4108-default-rtdb.europe-west1.firebasedatabase.app/");
        firebaseReference = firebaseDatabase.getReference("users");

        bindViews();
        setupListeners();
    }

    private void bindViews() {
        editTextInput = findViewById(R.id.editTextEmailOrUsernamePasswordChange);
        buttonResetPassword = findViewById(R.id.buttonChangePassword);
    }

    private void setupListeners() {
        buttonResetPassword.setOnClickListener(v -> initiatePasswordReset());
    }

    private void initiatePasswordReset() {
        String input = editTextInput.getText().toString().trim();

        if (input.isEmpty()) {
            ToastUtils.showError(this, "Please enter your email or username");
            return;
        }

        if (android.util.Patterns.EMAIL_ADDRESS.matcher(input).matches()) {
            sendPasswordResetEmail(input);
        } else {
            findEmailByUsername(input);
        }
    }

    private void findEmailByUsername(String username) {
        showLoading(true);

        firebaseReference.orderByChild("username").equalTo(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                showLoading(false);

                if (dataSnapshot.exists()) {
                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        String email = userSnapshot.child("email").getValue(String.class);
                        if (email != null) {
                            sendPasswordResetEmail(email);
                            return;
                        }
                    }
                }
                ToastUtils.showError(PasswordChange.this, "No account found with this username");
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                showLoading(false);
                ToastUtils.showError(PasswordChange.this, "Error: " + databaseError.getMessage());
            }
        });
    }

    private void sendPasswordResetEmail(String email) {
        showLoading(true);

        mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            showLoading(false);

            if (task.isSuccessful()) {
                ToastUtils.showSuccess(PasswordChange.this, "Password reset instructions have been sent to your email");
                finish();
            } else {
                ToastUtils.showError(PasswordChange.this, "Failed to send reset email: " + task.getException().getMessage());
            }
        });
    }

    private void showLoading(boolean isLoading) {
        buttonResetPassword.setEnabled(!isLoading);
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
}