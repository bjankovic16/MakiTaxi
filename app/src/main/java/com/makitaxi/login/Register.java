package com.makitaxi.login;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.makitaxi.R;
import com.makitaxi.model.User;

public class Register extends AppCompatActivity {
    private static final String TAG = "Register";

    private EditText editTextName, editTextEmail, editTextPhone, editTextUsername, editTextPassword, editTextConfirmPassword;
    private ImageButton buttonShowPassword, buttonShowConfirmPassword;
    private Button buttonSignUp;

    private FirebaseAuth auth;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference firebaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.register);

        auth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance("https://makitaxi-e4108-default-rtdb.europe-west1.firebasedatabase.app/");
        firebaseReference = firebaseDatabase.getReference("users");

        bindViews();
        addButtonListeners();
    }

    private void bindViews() {
        editTextName = findViewById(R.id.editTextName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPhone = findViewById(R.id.editTextPhone);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        buttonShowPassword = findViewById(R.id.buttonShowPassword);
        buttonShowConfirmPassword = findViewById(R.id.buttonShowConfirmPassword);
        buttonSignUp = findViewById(R.id.buttonSignUp);
    }

    private void addButtonListeners() {
        buttonShowPassword.setOnClickListener(v -> togglePasswordVisibility(editTextPassword, buttonShowPassword));
        buttonShowConfirmPassword.setOnClickListener(v -> togglePasswordVisibility(editTextConfirmPassword, buttonShowConfirmPassword));
        buttonSignUp.setOnClickListener(v -> registerUser());
    }

    private void togglePasswordVisibility(EditText passwordField, ImageButton button) {
        int inputType = passwordField.getInputType();
        if ((inputType & InputType.TYPE_TEXT_VARIATION_PASSWORD) > 0) {
            passwordField.setInputType(InputType.TYPE_CLASS_TEXT);
            button.setImageResource(R.drawable.visibility_off);
        } else {
            passwordField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            button.setImageResource(R.drawable.visibility);
        }
        passwordField.setSelection(passwordField.getText().length());
    }

    private void registerUser() {
        String name = editTextName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String username = editTextUsername.getText().toString().trim();
        String phone = editTextPhone.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String confirmPassword = editTextConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(phone) || TextUtils.isEmpty(password) || TextUtils.isEmpty(username)) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("Invalid email format");
            editTextEmail.requestFocus();
            return;
        }

        if (password.length() < 6) {
            editTextPassword.setError("Password must be at least 6 characters");
            editTextPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        firebaseReference.orderByChild("username").equalTo(username).get().addOnCompleteListener(usernameTask -> {
            if (usernameTask.isSuccessful()) {
                if (usernameTask.getResult().exists()) {
                    Toast.makeText(this, "Username is already taken", Toast.LENGTH_SHORT).show();
                    editTextUsername.setError("Username is already taken");
                    editTextUsername.requestFocus();
                    return;
                }

                firebaseReference.orderByChild("email").equalTo(email).get().addOnCompleteListener(emailTask -> {
                    if (emailTask.isSuccessful()) {
                        if (emailTask.getResult().exists()) {
                            Toast.makeText(this, "Email is already registered", Toast.LENGTH_SHORT).show();
                            editTextEmail.setError("Email is already registered");
                            editTextEmail.requestFocus();
                            return;
                        }

                        createUserAccount(email, password, name, username, phone);
                    } else {
                        Log.e(TAG, "Error checking email: " + emailTask.getException().getMessage());
                        Toast.makeText(this, "Error checking email availability", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Log.e(TAG, "Error checking username: " + usernameTask.getException().getMessage());
                Toast.makeText(this, "Error checking username availability", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createUserAccount(String email, String password, String name, String username, String phone) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                FirebaseUser firebaseUser = auth.getCurrentUser();
                if (firebaseUser != null) {
                    String uid = firebaseUser.getUid();
                    
                    User user = new User(name, email, phone, username);

                    if (email.toLowerCase().endsWith("@drivermaki.com")) {
                        user.setRole("DRIVER");
                        user.setVerified(false);
                    } else {
                        user.setRole("PASSENGER");
                        user.setVerified(true);
                    }

                    firebaseReference.child(uid).setValue(user).addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "User registered successfully", Toast.LENGTH_SHORT).show();
                        // Navigate to Login, which will handle routing
                        Intent intent = new Intent(Register.this, Login.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }).addOnFailureListener(e -> {
                        Toast.makeText(this, "Error saving user data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            } else {
                Toast.makeText(this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
