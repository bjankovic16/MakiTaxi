package com.makitaxi.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ImageUploadHelper {
    private static final String TAG = "ImageUploadHelper";

    private final FirebaseStorage storage;
    private final FirebaseAuth auth;
    private final FirebaseDatabase database;
    
    public ImageUploadHelper() {
        FirebaseStorage tempStorage;
        try {
            tempStorage = FirebaseStorage.getInstance("gs://makitaxi-e4108.firebasestorage.app");
            Log.d(TAG, "Using explicit storage bucket: gs://makitaxi-e4108.firebasestorage.app");
        } catch (Exception e) {
            Log.w(TAG, "Failed to initialize with explicit bucket, using default", e);
            tempStorage = FirebaseStorage.getInstance();
        }
        storage = tempStorage;
        
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance("https://makitaxi-e4108-default-rtdb.europe-west1.firebasedatabase.app/");
    }
    
    public interface ImageUploadListener {
        void onUploadStart();
        void onUploadSuccess(String imageUrl);
        void onUploadError(String error);
    }

    private Bitmap cropToSquare(Bitmap source) {
        int width = source.getWidth();
        int height = source.getHeight();

        if (width == height) {
            return source;
        }

        int size = Math.min(width, height);
        int xOffset = (width - size) / 2;
        int yOffset = (height - size) / 2;

        Bitmap croppedBitmap = Bitmap.createBitmap(source, xOffset, yOffset, size, size);
        
        Log.d(TAG, "Cropped image from " + width + "x" + height + " to " + size + "x" + size);
        
        return croppedBitmap;
    }
    public void deleteProfileImage(String imageUrl, DeleteImageListener listener) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            listener.onDeleteError("No image URL provided");
            return;
        }
        
        try {
            StorageReference imageRef = storage.getReferenceFromUrl(imageUrl);
            imageRef.delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Profile image deleted from storage");
                        listener.onDeleteSuccess();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to delete profile image", e);
                        listener.onDeleteError("Failed to delete image: " + e.getMessage());
                    });
        } catch (Exception e) {
            Log.e(TAG, "Invalid image URL", e);
            listener.onDeleteError("Invalid image URL");
        }
    }
    
    public interface DeleteImageListener {
        void onDeleteSuccess();
        void onDeleteError(String error);
    }

    public void uploadProfileImageSimple(Context context, Uri imageUri, ImageUploadListener listener) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            listener.onUploadError("User not authenticated");
            return;
        }
        
        if (imageUri == null) {
            listener.onUploadError("No image selected");
            return;
        }
        
        listener.onUploadStart();
        
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                listener.onUploadError("Cannot access selected image");
                return;
            }

            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
            
            if (bitmap == null) {
                listener.onUploadError("Failed to decode image");
                return;
            }
            Bitmap croppedBitmap = cropToSquare(bitmap);

            Bitmap resizedBitmap = Bitmap.createScaledBitmap(croppedBitmap, 300, 300, true);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] imageData = baos.toByteArray();
            String base64Image = "data:image/jpeg;base64," + android.util.Base64.encodeToString(imageData, android.util.Base64.DEFAULT);

            if (bitmap != croppedBitmap) bitmap.recycle();
            if (croppedBitmap != resizedBitmap) croppedBitmap.recycle();
            resizedBitmap.recycle();

            String userId = currentUser.getUid();
            database.getReference("users")
                    .child(userId)
                    .child("profilePicture")
                    .setValue(base64Image)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Profile image saved to database");
                        listener.onUploadSuccess(base64Image);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to save profile image", e);
                        listener.onUploadError("Failed to save image: " + e.getMessage());
                    });
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing image", e);
            listener.onUploadError("Error processing image: " + e.getMessage());
        }
    }
} 