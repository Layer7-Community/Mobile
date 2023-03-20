package com.brcm.apim.magtraining;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

public class QRCodeAuthenticationTest {
    /*This method is used to get the content from string.xml */
    private String getResourceString(int id) {
        Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        return targetContext.getResources().getString(id);
    }
}
