package com.brcm.apim.magtraining;

import android.os.Handler;
public abstract class myCallBackHandler<T>{

        /**
        * The Handler to handle the callback, refer to {@link Handler} for details
        */
        public Handler getHandler() {
                return null;
        }

        /**
         * Called when an asynchronous call completes successfully.
         * @param result the value returned
         */
        public abstract void onSuccess();

        public abstract void onSuccess(String magUserName, String magUserPassword);

        /**
         * Called when an asynchronous call fails to complete.
         * @param e the reason for failure
         */
        public abstract void onError();

}
