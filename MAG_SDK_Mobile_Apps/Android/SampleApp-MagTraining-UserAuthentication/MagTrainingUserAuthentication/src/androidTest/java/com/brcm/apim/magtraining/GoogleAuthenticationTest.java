package com.brcm.apim.magtraining;


import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.web.sugar.Web.onWebView;
import static androidx.test.espresso.web.webdriver.DriverAtoms.clearElement;
import static androidx.test.espresso.web.webdriver.DriverAtoms.findElement;
import static androidx.test.espresso.web.webdriver.DriverAtoms.webKeys;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.KeyEvent;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.web.model.SimpleAtom;
import androidx.test.espresso.web.webdriver.DriverAtoms;
import androidx.test.espresso.web.webdriver.Locator;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

/* This class is used to test Social Authentication */
@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GoogleAuthenticationTest {

    /* This method is called before the test, it will invoke/enable the runtime permissions, This also initiates Idling resource
       object */

    /* This will launch the MainActivity */
    @Rule
    public ActivityScenarioRule<SocialLoginActivity> mActivityRule1 =
            new ActivityScenarioRule<>(SocialLoginActivity.class);


    @Test
    public void testGoogleLogIn() throws InterruptedException, UiObjectNotFoundException {
        Thread.sleep(5000);
        onView(withText("Google")).perform(click());
        Log.d("TAG", "Google Clicked Successfully");
        Thread.sleep(5000);
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());



        if (device.findObject(new UiSelector().resourceId("identifierId")).exists()) {
            UiSelector selector = new UiSelector();
            UiObject userName = device.findObject(selector.resourceId("identifierId"));
            userName.setText((getResourceString(R.string.email)));
            UiObject next = device.findObject(selector.resourceId("identifierNext"));
            next.click();
            Thread.sleep(2000);

            device.pressKeyCode(KeyEvent.KEYCODE_G);
            device.pressKeyCode(KeyEvent.KEYCODE_O);
            device.pressKeyCode(KeyEvent.KEYCODE_O);
            device.pressKeyCode(KeyEvent.KEYCODE_G);
            device.pressKeyCode(KeyEvent.KEYCODE_L);
            device.pressKeyCode(KeyEvent.KEYCODE_E);
            device.pressKeyCode(KeyEvent.KEYCODE_1);
            device.pressKeyCode(KeyEvent.KEYCODE_2);
            device.pressKeyCode(KeyEvent.KEYCODE_3);
            device.pressKeyCode(KeyEvent.KEYCODE_G);
            device.pressKeyCode(KeyEvent.KEYCODE_G);

            Thread.sleep(5000);

            UiObject login = device.findObject(selector.resourceId("passwordNext"));
            login.click();
            Thread.sleep(3000);

            if (device.findObject(selector.textContains("Advanced")).exists()) {
                device.findObject(selector.textContains("Advanced")).click();
                Thread.sleep(3000);
                device.findObject(selector.resourceId("proceed-link")).click();
                Thread.sleep(3000);
            }
            device.findObject(new UiSelector().textContains("Grant")).click();
        } else {
            device.findObject(new UiSelector().textContains("sdklogin22@gmail.com")).click();
            Thread.sleep(5000);
            UiSelector selector = new UiSelector();
            if (device.findObject(selector.textContains("Advanced")).exists()) {
                device.findObject(selector.textContains("Advanced")).click();
                Thread.sleep(1000);
                device.findObject(selector.resourceId("proceed-link")).click();
                Thread.sleep(3000);
            }
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
