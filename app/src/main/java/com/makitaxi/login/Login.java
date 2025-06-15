package com.makitaxi.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;
import com.makitaxi.R;
import com.makitaxi.passanger.PassangerMainScreen;
import com.makitaxi.utils.NavigationClickListener;

public class Login extends AppCompatActivity {
    private static final String TAG = "Login";

    private EditText loginInput;
    private EditText loginPassword;
    private ImageButton buttonShowPassword;
    private Button signInButton;
    private Button signUpButton;
    private TextView changePasswordText;

    private FirebaseAuth auth;
    private FirebaseDatabase firebaseDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.login);

        auth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance("https://makitaxi-e4108-default-rtdb.europe-west1.firebasedatabase.app/");

        bindViews();
        addButtonListeners();
    }

    private void bindViews() {
        loginInput = findViewById(R.id.loginUsername);
        loginPassword = findViewById(R.id.loginPassword);
        buttonShowPassword = findViewById(R.id.buttonShowPassword);
        signInButton = findViewById(R.id.buttonSignIn);
        signUpButton = findViewById(R.id.singUpFromLoginButton);
        changePasswordText = findViewById(R.id.textViewChangePassword);
    }

    private void addButtonListeners() {
        if (signUpButton != null) {
            signUpButton.setOnClickListener(NavigationClickListener.navigateTo(this, Register.class));
        }

        if (changePasswordText != null) {
            changePasswordText.setOnClickListener(NavigationClickListener.navigateTo(this, PasswordChange.class));
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

    private void signInWithEmail(String email, String password) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                navigateToMainScreen();
            } else {
                Toast.makeText(Login.this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void signInWithUsername(String username, String password) {
        firebaseDatabase.getReference("users").orderByChild("username").equalTo(username).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                DataSnapshot snapshot = task.getResult().getChildren().iterator().next();
                String email = snapshot.child("email").getValue(String.class);

                if (email != null) {
                    signInWithEmail(email, password);
                } else {
                    Toast.makeText(Login.this, "Error: User email not found", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(Login.this, "Username not found", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void navigateToMainScreen() {
        Intent intent = new Intent(Login.this, PassangerMainScreen.class);
        startActivity(intent);
        finish();
    }
}