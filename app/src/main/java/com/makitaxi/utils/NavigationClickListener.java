package com.makitaxi.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import com.makitaxi.R;

/**
 * A utility class that provides reusable OnClickListener implementations
 * for common navigation patterns in the app.
 */
public class NavigationClickListener implements View.OnClickListener {

    private final Context context;
    private final Class<?> destinationActivity;
    private final NavigationType navigationType;

    /**
     * Navigation types supported by this listener
     */
    public enum NavigationType {
        NAVIGATE_TO,      // Navigate to a new activity
        FINISH_ACTIVITY,  // Close the current activity
        GO_BACK           // Go back to previous activity
    }

    /**
     * Creates a listener that navigates to a destination activity
     */
    public static NavigationClickListener navigateTo(Context context, Class<?> destinationActivity) {
        return new NavigationClickListener(context, destinationActivity, NavigationType.NAVIGATE_TO);
    }

    /**
     * Creates a listener that finishes the current activity
     */
    public static NavigationClickListener finishActivity(Activity activity) {
        return new NavigationClickListener(activity, null, NavigationType.FINISH_ACTIVITY);
    }

    /**
     * Creates a listener that goes back to the previous activity
     */
    public static NavigationClickListener goBack(Activity activity) {
        return new NavigationClickListener(activity, null, NavigationType.GO_BACK);
    }

    private NavigationClickListener(Context context, Class<?> destinationActivity, NavigationType navigationType) {
        this.context = context;
        this.destinationActivity = destinationActivity;
        this.navigationType = navigationType;
    }

    @Override
    public void onClick(View v) {
        switch (navigationType) {
            case NAVIGATE_TO:
                Intent intent = new Intent(context, destinationActivity);
                context.startActivity(intent);
                break;

            case FINISH_ACTIVITY:
                ((Activity) context).finish();
                break;

            case GO_BACK:
                ((Activity) context).onBackPressed();
                break;
        }
    }
}