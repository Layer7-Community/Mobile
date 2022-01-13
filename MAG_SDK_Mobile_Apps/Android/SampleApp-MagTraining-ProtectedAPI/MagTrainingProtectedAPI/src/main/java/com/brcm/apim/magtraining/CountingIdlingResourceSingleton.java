package com.brcm.apim.magtraining;

import androidx.test.espresso.idling.CountingIdlingResource;

/* This class is used for wait until the resource completes the background task/job */
public class CountingIdlingResourceSingleton {

    private static String RESOURCE = "GLOBAL";

    public static CountingIdlingResource countingIdlingResource = new CountingIdlingResource(RESOURCE);

    /* Call this method when the background job is initiated */
    static void increment() {
        countingIdlingResource.increment();
    }

    /* Call this method when the background job is completed */
    static void decrement() {
        if (!countingIdlingResource.isIdleNow()) {
            countingIdlingResource.decrement();
        }
    }
}
