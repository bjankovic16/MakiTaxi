package com.makitaxi.e2e;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.makitaxi.R;
import com.makitaxi.passenger.PassengerScreen;
import com.makitaxi.utils.NotificationStatus;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class CompleteRideFlowTest {

    @Rule
    public ActivityScenarioRule<PassengerScreen> activityRule =
            new ActivityScenarioRule<>(PassengerScreen.class);

    @Test
    public void testCompleteRideFlow() throws InterruptedException {
        onView(withId(R.id.pickupLocation))
            .perform(typeText("Теразије 23, Београд"));
        
        onView(withId(R.id.dropoffLocation))
            .perform(typeText("Калемегдан, Београд"));

        onView(withId(R.id.carSelectionButton))
            .perform(click());

        onView(withId(R.id.basicCarOption))
            .perform(click());

        onView(withId(R.id.estimatedPriceText))
            .check(matches(isDisplayed()));

        onView(withId(R.id.requestRideButton))
            .perform(click());

        onView(withId(R.id.searchingDialog))
            .check(matches(isDisplayed()));

        Thread.sleep(2000);

        onView(withId(R.id.driverDetailsSheet))
            .check(matches(isDisplayed()));

        onView(withId(R.id.driverName))
            .check(matches(isDisplayed()));
        onView(withId(R.id.carDetails))
            .check(matches(isDisplayed()));

        onView(withId(R.id.confirmRideButton))
            .perform(click());

        onView(withId(R.id.activeRideLayout))
            .check(matches(isDisplayed()));

        Thread.sleep(2000);

        onView(withId(R.id.rideCompletedDialog))
            .check(matches(isDisplayed()));

        onView(withId(R.id.ratingBar))
            .perform(click());

        onView(withId(R.id.feedbackText))
            .perform(typeText("Одлична вожња!"));

        onView(withId(R.id.submitFeedbackButton))
            .perform(click());
    }

    @Test
    public void testRideCancellation() {
        onView(withId(R.id.pickupLocation))
            .perform(typeText("Теразије 23, Београд"));
        
        onView(withId(R.id.dropoffLocation))
            .perform(typeText("Калемегдан, Београд"));

        onView(withId(R.id.requestRideButton))
            .perform(click());

        onView(withId(R.id.cancelSearchButton))
            .perform(click());

        onView(withId(R.id.requestRideButton))
            .check(matches(isDisplayed()));
    }

    @Test
    public void testDriverDecline() throws InterruptedException {
        onView(withId(R.id.pickupLocation))
            .perform(typeText("Теразије 23, Београд"));
        
        onView(withId(R.id.dropoffLocation))
            .perform(typeText("Калемегдан, Београд"));

        onView(withId(R.id.requestRideButton))
            .perform(click());

        Thread.sleep(2000);

        onView(withId(R.id.searchingDialog))
            .check(matches(isDisplayed()));
        
        onView(withId(R.id.searchingText))
            .check(matches(withText(containsString("Тражимо следећег возача"))));
    }
}