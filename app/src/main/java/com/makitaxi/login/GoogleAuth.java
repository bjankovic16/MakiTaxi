package com.makitaxi.login;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;
import com.makitaxi.R;
import com.makitaxi.model.User;
import com.makitaxi.utils.PreferencesManager;

public class GoogleAuth {
    private static final String TAG = "GoogleAuth";
    private static GoogleAuth instance;
    private final FirebaseAuth auth;
    private final FirebaseDatabase database;
    private final SignInClient oneTapClient;
    private ActivityResultLauncher<IntentSenderRequest> signInLauncher;
    private Context context; // Store context for session saving

    private GoogleAuth(Context context) {
        this.context = context;
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance("https://makitaxi-e4108-default-rtdb.europe-west1.firebasedatabase.app/");
        oneTapClient = Identity.getSignInClient(context);
    }

    public static GoogleAuth getInstance(Context context) {
        if (instance == null) {
            instance = new GoogleAuth(context);
        }
        return instance;
    }

    public void initializeSignInLauncher(AppCompatActivity activity, OnSignInResultListener listener) {
        signInLauncher = activity.registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                try {
                    SignInCredential credential = oneTapClient.getSignInCredentialFromIntent(result.getData());
                    String idToken = credential.getGoogleIdToken();
                    String email = credential.getId();
                    String name = credential.getDisplayName();

                    if (idToken != null) {
                        firebaseAuthWithGoogle(idToken, email, name, listener);
                    } else {
                        listener.onError("Failed to get required information from Google Sign In");
                    }
                } catch (ApiException e) {
                    Log.e(TAG, "Google sign in failed", e);
                    String errorMessage = "Google sign in failed: ";
                    switch (e.getStatusCode()) {
                        case 10:
                            errorMessage += "Developer error - check your OAuth configuration";
                            break;
                        case 7:
                            errorMessage += "Network error - check your internet connection";
                            break;
                        case 12500:
                            errorMessage += "Sign in was cancelled";
                            break;
                        default:
                            errorMessage += e.getStatusCode();
                    }
                    listener.onError(errorMessage);
                } catch (Exception e) {
                    Log.e(TAG, "Unexpected error during Google sign in", e);
                    listener.onError("An unexpected error occurred: " + e.getMessage());
                }
            } else {
                listener.onError("Sign in was cancelled");
            }
        });
    }

    public void signIn(AppCompatActivity activity, OnSignInResultListener listener) {
        BeginSignInRequest signInRequest = BeginSignInRequest.builder().setGoogleIdTokenRequestOptions(BeginSignInRequest.GoogleIdTokenRequestOptions.builder().setSupported(true).setServerClientId(activity.getString(R.string.default_web_client_id)).setFilterByAuthorizedAccounts(false).build()).build();

        oneTapClient.beginSignIn(signInRequest).addOnSuccessListener(activity, result -> {
            try {
                IntentSenderRequest intentSenderRequest = new IntentSenderRequest.Builder(result.getPendingIntent().getIntentSender()).build();
                signInLauncher.launch(intentSenderRequest);
            } catch (Exception e) {
                Log.e(TAG, "Could not start sign in", e);
                listener.onError("Could not start sign in: " + e.getMessage());
            }
        }).addOnFailureListener(activity, e -> {
            Log.e(TAG, "Sign in failed", e);
            listener.onError("Sign in failed: " + e.getMessage());
        });
    }

    private void firebaseAuthWithGoogle(String idToken, String email, String name, OnSignInResultListener listener) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String userId = auth.getCurrentUser().getUid();
                checkAndCreateUser(userId, email, name, listener);
            } else {
                Log.e(TAG, "Firebase authentication failed", task.getException());
                listener.onError("Authentication failed: " + task.getException().getMessage());
            }
        });
    }

    private void checkAndCreateUser(String userId, String email, String name, OnSignInResultListener listener) {
        database.getReference("users").child(userId).get().addOnCompleteListener(dbTask -> {
            if (dbTask.isSuccessful()) {
                DataSnapshot snapshot = dbTask.getResult();
                if (!snapshot.exists()) {
                    createNewGoogleUser(userId, email, name, null, listener);
                } else {
                    User user = snapshot.getValue(User.class);
                    if(user != null) {
                        Log.d(TAG, "Existing user found, saving session");
                        PreferencesManager.saveLoginSession(context, email);
                        
                        PreferencesManager.cacheUser(context, user);
                        Log.d(TAG, "User object cached on Google login");
                        
                        listener.onSuccess(user.getRole(), user.isVerified());
                    } else {
                        String errorMessage = "Failed to parse user data from database.";
                        Log.e(TAG, errorMessage);
                        listener.onError(errorMessage);
                    }
                }
            } else {
                Log.e(TAG, "Failed to check user existence", dbTask.getException());
                listener.onError("Failed to check user existence: " + dbTask.getException().getMessage());
            }
        });
    }

    private void createNewGoogleUser(String userId, String email, String name, String photoUrl, OnSignInResultListener listener) {
        User user = new User(name, email, null, null);

        // Assign role and verification status based on email
        if (email != null && email.toLowerCase().endsWith("@drivermaki.com")) {
            user.setRole("DRIVER");
            user.setVerified(false); // Drivers require manual verification
        } else {
            user.setRole("PASSENGER");
            user.setVerified(true); // Passengers are auto-verified
        }

        // Set profile picture if available
        if (photoUrl != null) {
            user.setProfilePicture(photoUrl);
        }

        database.getReference("users").child(userId).setValue(user).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                PreferencesManager.saveLoginSession(context, email);
                
                PreferencesManager.cacheUser(context, user);
                Log.d(TAG, "New Google user object cached");
                
                listener.onSuccess(user.getRole(), user.isVerified());
            } else {
                Log.e(TAG, "Failed to create user profile", task.getException());
                listener.onError("Failed to create user profile: " + task.getException().getMessage());
            }
        });
    }

    public interface OnSignInResultListener {
        void onSuccess(String role, boolean verified);

        void onError(String error);
    }
} 