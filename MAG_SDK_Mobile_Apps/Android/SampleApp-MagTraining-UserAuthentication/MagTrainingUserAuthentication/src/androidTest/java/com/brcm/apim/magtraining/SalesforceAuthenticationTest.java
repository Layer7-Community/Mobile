package com.brcm.apim.magtraining;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Context;
import android.util.Log;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

public class SalesforceAuthenticationTest {
    /* This will launch the MainActivity */
    @Rule
    public ActivityScenarioRule<SocialLoginActivity> mActivityRule1 =
            new ActivityScenarioRule<>(SocialLoginActivity.class);

    @Test
    public void testSalesforceLogIn() throws InterruptedException, UiObjectNotFoundException {
        Thread.sleep(5000);
        onView(withText("Salesforce")).perform(click());
        Log.d("TAG", "Salesforce Clicked Successfully");
        Thread.sleep(8000);
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        UiSelector selector = new UiSelector();

        if (device.findObject(new UiSelector().textContains("Grant")).exists()){
            device.findObject(new UiSelector().textContains("Grant")).click();
        }else {
            if (device.findObject(selector.textContains("Advanced")).exists()) {
                device.findObject(selector.textContains("Advanced")).click();
                Thread.sleep(3000);
                device.findObject(selector.resourceId("proceed-link")).click();
                Thread.sleep(3000);
            }


            UiObject userName = device.findObject(selector.resourceId("username"));
            userName.setText((getResourceString(R.string.email)));
            UiObject password = device.findObject(selector.resourceId("password"));
            password.setText((getResourceString(R.string.passSalesforce)));
            UiObject signIn = device.findObject(selector.resourceId("Login"));
            signIn.click();
            Thread.sleep(8000);
            device.findObject(new UiSelector().textContains("Grant")).click();
        }

    }

    //This method is executed after the test, used to unregister Idling resource
    @After
    public void unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(CountingIdlingResourceSingleton.countingIdlingResource);
    }
    /*This method is used to get the content from string.xml */
    private String getResourceString(int id) {
        Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        return targetContext.getResources().getString(id);
    }

}
