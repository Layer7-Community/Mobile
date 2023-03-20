package com.brcm.apim.magtraining;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import android.content.Context;
import android.os.Build;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;


/* This class is used to test User Authentication */
@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class UserAuthenticationTest {

    /* This method is called before the test, it will invoke/enable the runtime permissions, This also initiates Idling resource
       object */

    /* This will launch the MainActivity */
    @Rule
    public ActivityScenarioRule<MainActivity> mActivityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    /*This test is used to test the negative use-case of user-Authentication*/
    @Test
    public void test_01UserAuthenticationWrongPassword() {
        onView(withId(R.id.textView1))
                .perform(typeText(getResourceString(R.string.user_name)), closeSoftKeyboard());

        onView(withId(R.id.textView))
                .perform(typeText(getResourceString(R.string.wrong_password)), closeSoftKeyboard());
        onView(withId(R.id.loginButton)).perform(click());
//        Thread.sleep(8000);
        onView(withId(R.id.userStatusTextView))
                .check(matches(withText("Not Authenticated")));

    }

    /*This test is used to test the positive use-case of user-Authentication*/
    @Test
    public void test_02UserAuthenticationLogin() {
        onView(withId(R.id.textView1))
                .perform(typeText(getResourceString(R.string.user_name)), closeSoftKeyboard());
        onView(withId(R.id.textView))
                .perform(typeText(getResourceString(R.string.password)), closeSoftKeyboard());
        onView(withId(R.id.loginButton)).perform(click());
        onView(withId(R.id.logoutButton)).check(matches(isDisplayed()));
    }

    /* This test is used to test the logout functionality */
    @Test
    public void test_03UserAuthenticationLogout() {
        onView(withId(R.id.logoutButton)).perform(click());
        onView(withId(R.id.loginButton)).check(matches(isDisplayed()));
    }

    /*This method is used to get the content from string.xml */
    private String getResourceString(int id) {
        Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        return targetContext.getResources().getString(id);
    }

    /*This method is executed after the test, used to unregister Idling resource */
    @After
    public void unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(CountingIdlingResourceSingleton.countingIdlingResource);
    }
}
