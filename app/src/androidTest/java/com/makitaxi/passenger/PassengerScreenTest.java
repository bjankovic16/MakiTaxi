package com.makitaxi.passenger;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.makitaxi.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class PassengerScreenTest {

    @Rule
    public ActivityScenarioRule<PassengerScreen> activityRule =
            new ActivityScenarioRule<>(PassengerScreen.class);

    @Test
    public void testBasicRideRequest() {
        // Enter pickup location
        onView(withId(R.id.pickupLocation))
            .perform(typeText("Terazije 23, Belgrade"));

        // Enter dropoff location
        onView(withId(R.id.dropoffLocation))
            .perform(typeText("Knez Mihailova 54, Belgrade"));

        // Open car selection
        onView(withId(R.id.carSelectionButton))
            .perform(click());

        // Select basic car type
        onView(withId(R.id.basicCarOption))
            .perform(click());

        // Request ride
        onView(withId(R.id.requestRideButton))
            .perform(click());

        // Verify that searching dialog is shown
        onView(withId(R.id.searchingDialog))
            .check(matches(isDisplayed()));
    }

    @Test
    public void testCarSelection() {
        // Open car selection
        onView(withId(R.id.carSelectionButton))
            .perform(click());

        // Verify all car types are displayed
        onView(withId(R.id.basicCarOption))
            .check(matches(isDisplayed()));
        onView(withId(R.id.luxuryCarOption))
            .check(matches(isDisplayed()));
        onView(withId(R.id.transportCarOption))
            .check(matches(isDisplayed()));

        // Select luxury car
        onView(withId(R.id.luxuryCarOption))
            .perform(click());

        // Verify selection is reflected
        onView(withId(R.id.selectedCarType))
            .check(matches(withText("LUXURY")));
    }

    @Test
    public void testLocationSelection() {
        // Click on map to select location
        onView(withId(R.id.mapView))
            .perform(click());

        // Verify location marker is shown
        onView(withId(R.id.locationMarker))
            .check(matches(isDisplayed()));

        // Verify confirm location button is shown
        onView(withId(R.id.confirmLocationButton))
            .check(matches(isDisplayed()));
    }
}

