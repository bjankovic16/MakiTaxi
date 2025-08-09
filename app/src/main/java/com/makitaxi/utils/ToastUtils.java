package com.makitaxi.utils;

import android.content.Context;
import android.widget.Toast;

public class ToastUtils {
    
    private ToastUtils() {
    }
    
    public static void showShort(Context context, String message) {
        if (context == null || message == null) return;
        
        String formattedMessage = formatMessage(message);
        Toast.makeText(context, formattedMessage, Toast.LENGTH_SHORT).show();
    }
    
    public static void showLong(Context context, String message) {
        if (context == null || message == null) return;
        
        String formattedMessage = formatMessage(message);
        Toast.makeText(context, formattedMessage, Toast.LENGTH_LONG).show();
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
