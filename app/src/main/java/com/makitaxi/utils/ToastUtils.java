package com.makitaxi.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Build;

public class ToastUtils {
    
    private ToastUtils() {
    }
    
    public static void showShort(Context context, String message) {
        if (context == null || message == null) return;
        
        String formattedMessage = formatMessage(message);
        showCustomToast(context, formattedMessage, Toast.LENGTH_SHORT);
    }
    
    public static void showLong(Context context, String message) {
        if (context == null || message == null) return;
        
        String formattedMessage = formatMessage(message);
        showCustomToast(context, formattedMessage, Toast.LENGTH_LONG);
    }
    
    public static void showSuccess(Context context, String message) {
        showShort(context, "✅ " + message);
    }
    
    public static void showError(Context context, String message) {
        showLong(context, "❌ " + message);
    }
    
    public static void showInfo(Context context, String message) {
        showShort(context, "ℹ️ " + message);
    }
    
    public static void showWarning(Context context, String message) {
        showShort(context, "⚠️ " + message);
    }

    private static void showCustomToast(Context context, String message, int duration) {
        Toast toast = new Toast(context);

        TextView textView = new TextView(context);
        textView.setText("🚕 " + message);
        textView.setTextColor(Color.WHITE);
        textView.setTextSize(16);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setGravity(Gravity.CENTER);
        textView.setPadding(40, 24, 40, 24);

        // Solid background color instead of gradient
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.parseColor("#343B71"));
        background.setCornerRadius(50);
        background.setStroke(2, Color.parseColor("#DDDDDD"));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            textView.setElevation(10);
        }

        textView.setBackground(background);

        toast.setView(textView);
        toast.setDuration(duration);
        toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, 120);
        toast.show();
    }


    private static String formatMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return message;
        }
        
        String cleanMessage = message;
        if (message.startsWith("✅ ") || message.startsWith("❌ ") || 
            message.startsWith("ℹ️ ") || message.startsWith("⚠️ ")) {
            String prefix = message.substring(0, 2);
            cleanMessage = message.substring(2).trim();
            
            String processed = processMultiSentence(cleanMessage);
            return prefix + " " + processed;
        }
        
        return processMultiSentence(cleanMessage);
    }
    
    private static String processMultiSentence(String message) {
        String[] sentences = message.split("(?<=[.!?])\\s+(?=[A-Z])");
        
        if (sentences.length <= 1) {
            return message;
        }
        
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < sentences.length; i++) {
            formatted.append(sentences[i].trim());
            if (i < sentences.length - 1) {
                formatted.append("\n");
            }
        }
        
        return formatted.toString();
    }
}
