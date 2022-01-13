package com.brcm.apim.magtraining;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.action.ViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.matchers.JUnitMatchers.containsString;

/* This class is used to test Protected API */
@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ProtectedAPITest {

    /* This method is called before the test, it will initiates Idling resource
      object */
    @Before
    public void before() {
        IdlingRegistry.getInstance().register(CountingIdlingResourceSingleton.countingIdlingResource);
    }

    /* This will launch the MainActivity */
    @Rule
    public ActivityScenarioRule<MainActivity> mActivityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    /*This test is used to test the positive use-case of user-Authentication*/
    @Test
    public void test01_loginUser() throws InterruptedException {
        onView(withId(R.id.loginButton)).perform(click());
        onView(withId(R.id.logoutButton)).check(matches(withText("Logout")));
    }

    /*This test is used to test invoking the protected API End Point functionality*/
    @Test
    public void test02_InvokeProtectedEndPoint() throws InterruptedException {

        onView(withId(R.id.protectedAPIButton)).perform(click());
//        onView(withId(R.id.jsonResponseData))
//                .check(matches(withText(containsString("1: Red Stapler"))));
    }

    /* This test is used to test the logout functionality */
    @Test
    public void test03_UserLogout() {
        onView(withId(R.id.logoutButton)).perform(click());
        onView(withId(R.id.loginButton)).check(matches(withText("Login")));
    }


}
