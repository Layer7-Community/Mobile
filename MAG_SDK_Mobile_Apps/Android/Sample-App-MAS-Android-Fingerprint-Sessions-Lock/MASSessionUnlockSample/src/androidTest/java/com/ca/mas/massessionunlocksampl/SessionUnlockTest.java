package com.ca.mas.massessionunlocksampl;

import static androidx.test.espresso.Espresso.closeSoftKeyboard;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import android.content.Context;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.action.ViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;


import com.ca.mas.massessionunlocksample.R;
import com.ca.mas.massessionunlocksample.activity.CountingIdlingResourceSingleton;
import com.ca.mas.massessionunlocksample.activity.SessionUnlockSampleActivity;

import static org.hamcrest.core.StringContains.containsString;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.io.IOException;

/* This class is used to test Session Unlock */
@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SessionUnlockTest {

    /* This method is called before the test, it will invoke/enable the runtime permissions, This also initiates Idling resource
      object */
    @Before
    public void before() {
        IdlingRegistry.getInstance().register(CountingIdlingResourceSingleton.countingIdlingResource);
    }

    /* This will launch the MainActivity */
    @Rule
    public ActivityScenarioRule<SessionUnlockSampleActivity> mActivityRule =
            new ActivityScenarioRule<>(SessionUnlockSampleActivity.class);

    /*This test is used to test the negative use-case of user-Authentication*/
    @Test
    public void test_01UserAuthenticationWrongPassword() {
        onView(withId(R.id.edit_text_username))
                .perform(typeText(getResourceString(R.string.user_name)), ViewActions.closeSoftKeyboard());

        onView(withId(R.id.edit_text_password))
                .perform(typeText(getResourceString(R.string.wrong_password)), ViewActions.closeSoftKeyboard());
        onView(withId(R.id.login_button)).perform(click());
//        Thread.sleep(8000);
        onView(withId(R.id.login_button))
                .check(matches(withText("Log in")));

    }

    /*This test is used to test the positive use-case of user-Authentication*/
    @Test
    public void test_02UserAuthentication() {
        onView(withId(R.id.edit_text_username))
                .perform(typeText(getResourceString(R.string.correct_user_name)), ViewActions.closeSoftKeyboard());

        onView(withId(R.id.edit_text_password))
                .perform(typeText(getResourceString(R.string.password)), ViewActions.closeSoftKeyboard());
        onView(withId(R.id.login_button)).perform(click());
//        Thread.sleep(8000);
        onView(withId(R.id.invoke_button)).check(matches(isDisplayed()));
    }

    /*This test is used to test the Invoke api positive use-case*/
    @Test
    public void test_03InvokeProtectedAPI() throws InterruptedException {
        onView(withId(R.id.invoke_button)).perform(click());
        onView(withId(R.id.data_text_view))
                .check(matches(withText(containsString("1: Red Stapler"))));
    }

    /*This test is used to test the Invoke session lock*/
    @Test
    public void test_04InvokeSessionLock() throws InterruptedException, IOException {
        onView(withId(R.id.checkbox_lock)).perform(click());
        onView(withId(R.id.data_text_view))
                .check(matches(withText(containsString("Session Locked"))));
        onView(withId(R.id.invoke_button)).perform(click());
        onView(withId(R.id.data_text_view))
                .check(matches(withText(containsString("Session Locked"))));

    }


//    @Test
//    public void test_05Logout() throws InterruptedException {
//        onView(withId(R.id.login_button)).perform(click());
//        onView(withId(R.id.data_text_view))
//                .check(matches(withText(containsString("Session Locked"))));
//        onView(withId(R.id.invoke_button)).perform(click());
//        onView(withId(R.id.data_text_view))
//                .check(matches(withText(containsString("Session Locked"))));
//        onView(withId(R.id.edit_text_username)).check(matches(isDisplayed()));
//    }


    /*This method is used to get the content from string.xml */
    private String getResourceString(int id) {
        Context targetContext = getInstrumentation().getTargetContext();
        return targetContext.getResources().getString(id);
    }

}
