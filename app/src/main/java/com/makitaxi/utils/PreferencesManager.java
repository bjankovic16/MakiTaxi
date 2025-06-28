package com.makitaxi.utils;

import android.content.Context;
import android.content.SharedPreferences;

public final class PreferencesManager {

    private static final String PREFS_NAME = "prefs";
    private static final String SPLASH_SHOWN_KEY = "splash_screens_shown";
    
    // Session management keys
    private static final String IS_LOGGED_IN_KEY = "is_logged_in";
    private static final String USER_EMAIL_KEY = "user_email";
    private static final String LOGIN_TIME_KEY = "login_time";

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private PreferencesManager() {
    }

    /**
     * Gets the SharedPreferences instance for the app.
     *
     * @param context The application context.
     * @return The SharedPreferences instance.
     */
    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Gets the SharedPreferences instance for OSMDroid configuration.
     * This method is used by OSMDroid to store map tiles cache and configuration.
     *
     * @param context The application context.
     * @return The SharedPreferences instance for OSMDroid.
     */
    public static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE);
    }

    /**
     * Checks if the splash screens have been shown before.
     *
     * @param context The application context.
     * @return True if the splash screens have been shown, false otherwise.
     */
    public static boolean haveSplashScreensBeenShown(Context context) {
        return getPrefs(context).getBoolean(SPLASH_SHOWN_KEY, false);
    }

    /**
     * Sets the flag indicating that the splash screens have been shown.
     *
     * @param context The application context.
     * @param shown   The value to set the flag to.
     */
    public static void setSplashScreensShown(Context context, boolean shown) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putBoolean(SPLASH_SHOWN_KEY, shown);
        editor.apply();
    }
    
    // ========== SESSION MANAGEMENT ==========
    
    /**
     * Save user login session
     *
     * @param context The application context
     * @param email User's email
     */
    public static void saveLoginSession(Context context, String email) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putBoolean(IS_LOGGED_IN_KEY, true);
        editor.putString(USER_EMAIL_KEY, email);
        editor.putLong(LOGIN_TIME_KEY, System.currentTimeMillis());
        editor.apply();
    }
    
    /**
     * Check if user is logged in
     *
     * @param context The application context
     * @return True if user is logged in, false otherwise
     */
    public static boolean isUserLoggedIn(Context context) {
        return getPrefs(context).getBoolean(IS_LOGGED_IN_KEY, false);
    }
    
    /**
     * Get logged in user's email
     *
     * @param context The application context
     * @return User's email or null if not logged in
     */
    public static String getUserEmail(Context context) {
        if (!isUserLoggedIn(context)) {
            return null;
        }
        return getPrefs(context).getString(USER_EMAIL_KEY, null);
    }
    
    /**
     * Get login timestamp
     *
     * @param context The application context
     * @return Login timestamp or 0 if not logged in
     */
    public static long getLoginTime(Context context) {
        if (!isUserLoggedIn(context)) {
            return 0;
        }
        return getPrefs(context).getLong(LOGIN_TIME_KEY, 0);
    }
    
    /**
     * Clear user session (logout)
     *
     * @param context The application context
     */
    public static void clearUserSession(Context context) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.remove(IS_LOGGED_IN_KEY);
        editor.remove(USER_EMAIL_KEY);
        editor.remove(LOGIN_TIME_KEY);
        editor.apply();
    }
    
    /**
     * Check if session is still valid (not expired)
     * Sessions expire after 30 days of inactivity
     *
     * @param context The application context
     * @return True if session is valid, false if expired
     */
    public static boolean isSessionValid(Context context) {
        if (!isUserLoggedIn(context)) {
            return false;
        }
        
        long loginTime = getLoginTime(context);
        long currentTime = System.currentTimeMillis();
        long sessionDuration = currentTime - loginTime;
        
        // Session expires after 30 days (30 * 24 * 60 * 60 * 1000 milliseconds)
        long maxSessionDuration = 30L * 24 * 60 * 60 * 1000;
        
        if (sessionDuration > maxSessionDuration) {
            // Session expired, clear it
            clearUserSession(context);
            return false;
        }
        
        return true;
    }
    
    /**
     * Update login time (refresh session)
     *
     * @param context The application context
     */
    public static void refreshSession(Context context) {
        if (isUserLoggedIn(context)) {
            SharedPreferences.Editor editor = getPrefs(context).edit();
            editor.putLong(LOGIN_TIME_KEY, System.currentTimeMillis());
            editor.apply();
        }
    }
} 