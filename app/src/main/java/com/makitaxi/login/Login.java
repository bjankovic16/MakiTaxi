package com.makitaxi.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.makitaxi.utils.FirebaseHelper;
import com.makitaxi.utils.ToastUtils;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.makitaxi.R;
import com.makitaxi.driver.DriverMainScreen;
import com.makitaxi.passenger.PassengerScreen;
import com.makitaxi.utils.NavigationClickListener;
import com.makitaxi.utils.PreferencesManager;
import com.makitaxi.model.User;
import com.makitaxi.config.AppConfig;

import androidx.annotation.NonNull;

public class Login extends AppCompatActivity {
    private static final String TAG = "Login";

    private EditText loginInput;
    private EditText loginPassword;
    private ImageButton buttonShowPassword;
    private Button signInButton;
    private Button signUpButton;
    private Button signInWithGoogleButton;
    private TextView changePasswordText;

    private FirebaseAuth auth;
    private FirebaseDatabase firebaseDatabase;
    private GoogleAuth googleAuth;

    private static final String driver = "DRIVER";
    private static final String passenger = "PASSENGER";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.login);

        handleSystemBars();

        auth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance(AppConfig.FIREBASE_DATABASE_URL);
        googleAuth = GoogleAuth.getInstance(this);
        googleAuth.initializeSignInLauncher(this, new GoogleAuth.OnSignInResultListener() {
            @Override
            public void onSuccess(String role, boolean verified) {
                navigateToPassengerOrDriver(role, verified);
            }

            @Override
            public void onError(String error) {
                ToastUtils.showError(Login.this, error);
            }
        });

        bindViews();
        addButtonListeners();
    }

    private void bindViews() {
        loginInput = findViewById(R.id.loginUsername);
        loginPassword = findViewById(R.id.loginPassword);
        buttonShowPassword = findViewById(R.id.buttonShowPassword);
        signInButton = findViewById(R.id.buttonSignIn);
        signUpButton = findViewById(R.id.singUpFromLoginButton);
        signInWithGoogleButton = findViewById(R.id.singInWihGoogle);
        changePasswordText = findViewById(R.id.textViewChangePassword);
    }

    private void addButtonListeners() {
        if (signUpButton != null) {
            signUpButton.setOnClickListener(NavigationClickListener.navigateTo(this, Register.class));
        }

        if (changePasswordText != null) {
            changePasswordText.setOnClickListener(NavigationClickListener.navigateTo(this, PasswordChange.class));
        }

        if (signInWithGoogleButton != null) {
            signInWithGoogleButton.setOnClickListener(v -> 
                googleAuth.signIn(this, new GoogleAuth.OnSignInResultListener() {
                    @Override
                    public void onSuccess(String role, boolean verified) {
                        navigateToPassengerOrDriver(role, verified);
                    }

                    @Override
                    public void onError(String error) {
                        ToastUtils.showError(Login.this, error);
                    }
                })
            );
        }

        buttonShowPassword.setOnClickListener(v -> togglePasswordVisibility());

        addLoginLogic();
    }

    private void togglePasswordVisibility() {
        int inputType = loginPassword.getInputType();
        if ((inputType & InputType.TYPE_TEXT_VARIATION_PASSWORD) > 0) {
            loginPassword.setInputType(InputType.TYPE_CLASS_TEXT);
            buttonShowPassword.setImageResource(R.drawable.visibility_off);
        } else {
            loginPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            buttonShowPassword.setImageResource(R.drawable.visibility);
        }
        loginPassword.setSelection(loginPassword.getText().length());
    }

    private void addLoginLogic() {
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String input = loginInput.getText().toString().trim();
                String password = loginPassword.getText().toString().trim();

                if (input.isEmpty()) {
                    loginInput.setError("Email or username cannot be empty");
                    loginInput.requestFocus();
                    return;
                }

                if (password.isEmpty()) {
                    loginPassword.setError("Password cannot be empty");
                    loginPassword.requestFocus();
                    return;
                }

                if (android.util.Patterns.EMAIL_ADDRESS.matcher(input).matches()) {
                    signInWithEmail(input, password);
                } else {
                    signInWithUsername(input, password);
                }
            }
        });
    }

    private void signIn(String email, String password, String role, boolean verified) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Login successful for user: " + email);
                
                PreferencesManager.saveLoginSession(Login.this, email);
                
                String userId = auth.getCurrentUser().getUid();
                cacheUserDataOnLogin(userId, role, verified);
                            } else {
                    ToastUtils.showError(Login.this, "Login failed: " + task.getException().getMessage());
                }
        });
    }

    private void cacheUserDataOnLogin(String userId, String role, boolean verified) {
        FirebaseDatabase.getInstance(AppConfig.FIREBASE_DATABASE_URL)
            .getReference(AppConfig.NODE_USERS)
            .child(userId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        User user = snapshot.getValue(User.class);
                        if (user != null) {
                            PreferencesManager.cacheUser(Login.this, user);
                            Log.d(TAG, "User object cached on login");
                        }
                    }
                    navigateToPassengerOrDriver(role, verified);
                }
                
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to cache user data: " + error.getMessage());
                    navigateToPassengerOrDriver(role, verified);
                }
            });
    }

    private void signInWithEmail(String email, String password) {
        firebaseDatabase.getReference("users").orderByChild("email").equalTo(email).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                DataSnapshot snapshot = task.getResult().getChildren().iterator().next();
                String role = snapshot.child("role").getValue(String.class);
                boolean verified = Boolean.TRUE.equals(snapshot.child("verified").getValue(Boolean.class));

                if (email != null) {
                    signIn(email, password, role, verified);
                } else {
                    ToastUtils.showError(Login.this, "Error: User email not found");
                }
            } else {
                ToastUtils.showError(Login.this, "Username not found");
            }
        });
    }

    private void signInWithUsername(String username, String password) {
        firebaseDatabase.getReference("users").orderByChild("username").equalTo(username).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                DataSnapshot snapshot = task.getResult().getChildren().iterator().next();
                String email = snapshot.child("email").getValue(String.class);
                String role = snapshot.child("role").getValue(String.class);
                boolean verified = Boolean.TRUE.equals(snapshot.child("verified").getValue(Boolean.class));

                if (email != null) {
                    signIn(email, password, role, verified);
                } else {
                    ToastUtils.showError(Login.this, "Error: User email not found");
                }
            } else {
                ToastUtils.showError(Login.this, "Username not found");
            }
        });
    }

    private void navigateToPassengerOrDriver(String role, boolean verified) {
        if (role.equals(driver) && verified) {
            navigateTo(DriverMainScreen.class);
        } else if(role.equals(passenger)){
            navigateTo(PassengerScreen.class);
        } else {
            ToastUtils.showWarning(Login.this, "Driver isn't verified yet");
        }
    }

    private void navigateTo(Class<?> destination) {
        Intent intent = new Intent(Login.this, destination);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
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