package com.brcm.apim.app_b;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import android.os.Build;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;


import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

/*This class is used to test SSO Login */
@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SSOLoginTest {


    /* This method is called before the test, it will invoke/enable the runtime permissions, This also initiates Idling resource
  object */
    @Before
    public void before() {
        IdlingRegistry.getInstance().register(CountingIdlingResourceSingleton.countingIdlingResource);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getInstrumentation().getUiAutomation().executeShellCommand(
                    "pm grant " + InstrumentationRegistry.getInstrumentation().getTargetContext().getPackageName()
                            + " android.permission.ACCESS_COARSE_LOCATION");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getInstrumentation().getUiAutomation().executeShellCommand(
                    "pm grant " + InstrumentationRegistry.getInstrumentation().getTargetContext().getPackageName()
                            + " android.permission.READ_PHONE_STATE");
        }
    }

    /* This will launch the MainActivity */
    @Rule
    public ActivityScenarioRule<MainActivity> mActivityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    /*This test is used to test the negative use-case of user-Authentication*/
    @Test
    public void test_01ssoLoginTestWrongCredentials() {
        onView(withId(R.id.loginButtonWrongCredentials)).perform(click());

        onView(withId(R.id.userStatusTextView))
                .check(matches(withText("Not Authenticated")));
    }

    /*This test is used to test the positive use-case of user-Authentication*/
    @Test
    public void test_02ssoLoginTest() {
        onView(withId(R.id.loginButton)).perform(click());

        onView(withId(R.id.logoutButton)).check(matches(isDisplayed()));
    }

    /* This test is used to test the logout functionality */
    @Test
    public void test_03ssoLogoutTest() {
        onView(withId(R.id.logoutButton)).perform(click());
        onView(withId(R.id.loginButton)).check(matches(isDisplayed()));
    }


}
