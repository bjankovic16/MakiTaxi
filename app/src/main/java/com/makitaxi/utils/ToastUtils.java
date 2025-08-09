package com.makitaxi.utils;

import android.content.Context;
import android.widget.Toast;

/**
 * Utility class for displaying improved toast messages
 * Handles multi-line toasts for better readability
 */
public class ToastUtils {
    
    private ToastUtils() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Show a short toast message
     * Automatically formats multi-sentence messages on separate lines
     */
    public static void showShort(Context context, String message) {
        if (context == null || message == null) return;
        
        String formattedMessage = formatMessage(message);
        Toast.makeText(context, formattedMessage, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Show a long toast message
     * Automatically formats multi-sentence messages on separate lines
     */
    public static void showLong(Context context, String message) {
        if (context == null || message == null) return;
        
        String formattedMessage = formatMessage(message);
        Toast.makeText(context, formattedMessage, Toast.LENGTH_LONG).show();
    }
    
    /**
     * Show a success toast with checkmark emoji
     */
    public static void showSuccess(Context context, String message) {
        showShort(context, "✅ " + message);
    }
    
    /**
     * Show an error toast with X emoji
     */
    public static void showError(Context context, String message) {
        showLong(context, "❌ " + message);
    }
    
    /**
     * Show an info toast with info emoji
     */
    public static void showInfo(Context context, String message) {
        showShort(context, "ℹ️ " + message);
    }
    
    /**
     * Show a warning toast with warning emoji
     */
    public static void showWarning(Context context, String message) {
        showShort(context, "⚠️ " + message);
    }
    
    /**
     * Format message to display multiple sentences on separate lines
     * Splits on sentence boundaries and joins with newlines
     */
    private static String formatMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return message;
        }
        
        // Remove emoji prefixes for processing
        String cleanMessage = message;
        if (message.startsWith("✅ ") || message.startsWith("❌ ") || 
            message.startsWith("ℹ️ ") || message.startsWith("⚠️ ")) {
            String prefix = message.substring(0, 2);
            cleanMessage = message.substring(2).trim();
            
            // Process the clean message and add prefix back
            String processed = processMultiSentence(cleanMessage);
            return prefix + " " + processed;
        }
        
        return processMultiSentence(cleanMessage);
    }
    
    /**
     * Process multi-sentence messages to display on separate lines
     */
    private static String processMultiSentence(String message) {
        // Split on sentence boundaries (. ! ?) followed by space and capital letter
        String[] sentences = message.split("(?<=[.!?])\\s+(?=[A-Z])");
        
        if (sentences.length <= 1) {
            return message;
        }
        
        // Join sentences with newlines for better readability
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
