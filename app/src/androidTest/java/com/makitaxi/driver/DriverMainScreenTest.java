package com.makitaxi.driver;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.makitaxi.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class DriverMainScreenTest {

    @Rule
    public ActivityScenarioRule<DriverMainScreen> activityRule =
            new ActivityScenarioRule<>(DriverMainScreen.class);

    @Test
    public void testDriverAvailabilityToggle() {
        // Toggle driver availability
        onView(withId(R.id.availabilityToggle))
            .perform(click());

        // Verify status text changes
        onView(withId(R.id.statusText))
            .check(matches(withText("Online")));

        // Toggle back
        onView(withId(R.id.availabilityToggle))
            .perform(click());

        // Verify status text changes back
        onView(withId(R.id.statusText))
            .check(matches(withText("Offline")));
    }

    @Test
    public void testRideRequestDialog() {
        // Simulate incoming ride request
        // Note: This would require mock data or test doubles
        
        // Verify ride request dialog is shown
        onView(withId(R.id.rideRequestDialog))
            .check(matches(isDisplayed()));

        // Verify ride details are shown
        onView(withId(R.id.pickupAddressText))
            .check(matches(isDisplayed()));
        onView(withId(R.id.dropoffAddressText))
            .check(matches(isDisplayed()));
        onView(withId(R.id.estimatedPriceText))
            .check(matches(isDisplayed()));

        // Accept ride request
        onView(withId(R.id.acceptRideButton))
            .perform(click());

        // Verify navigation starts
        onView(withId(R.id.navigationView))
            .check(matches(isDisplayed()));
    }

    @Test
    public void testNavigationDrawer() {
        // Open navigation drawer
        onView(withId(R.id.menuButton))
            .perform(click());

        // Verify drawer items are displayed
        onView(withId(R.id.nav_history))
            .check(matches(isDisplayed()));
        onView(withId(R.id.nav_account))
            .check(matches(isDisplayed()));
        onView(withId(R.id.nav_logout))
            .check(matches(isDisplayed()));
    }
}

