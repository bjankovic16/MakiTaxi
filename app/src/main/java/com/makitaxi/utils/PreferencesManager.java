package com.makitaxi.utils;

import android.content.Context;
import android.content.SharedPreferences;

public final class PreferencesManager {

    private static final String PREFS_NAME = "prefs";
    private static final String SPLASH_SHOWN_KEY = "splash_screens_shown";

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
} 