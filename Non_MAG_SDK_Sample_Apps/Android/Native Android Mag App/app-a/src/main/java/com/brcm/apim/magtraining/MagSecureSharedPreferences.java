package com.brcm.apim.magtraining;


import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import java.security.GeneralSecurityException;

//
// In this AES encrypted Shared preferences we are going to store the following:
//  deviceID, mag-identifier, MAG generated Access Token, Refresh Token, AccessToken-ExpiryTime, MAG-Client Certificate, MAG generated ID token,
//

public class MagSecureSharedPreferences {

    private final static String SHARED_PREF_NAME="magSecureSharedPreferences";
    private static SharedPreferences encryptedSharedPreferences = null;
    private static final String TAG = MainActivity.class.getSimpleName();

    public MagSecureSharedPreferences(Context ctx) {

        String masterKeyAlias = null;
        try {
            masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            encryptedSharedPreferences = EncryptedSharedPreferences.create(
                    SHARED_PREF_NAME,
                    masterKeyAlias,
                    ctx,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void put(String keyValue, String keyData) {

        if (get(keyValue) != "") {
            remove(keyValue);
        }
        Log.d( TAG, "LoginHandler - Storing " + keyValue + " with value: [" + keyData + ']');
        encryptedSharedPreferences.edit().putString(keyValue, keyData).commit();
    }
    public static String get(String keyValue) {
        return encryptedSharedPreferences.getString(keyValue,"");
    }

    public static void remove(String keyValue) {
        encryptedSharedPreferences.edit().remove(keyValue).commit();
    }

    //
    // Destroys the contexts of the Encrypted Shared Preferences, not just a particular entry
    //
    public static void destroy()
    {
        encryptedSharedPreferences.edit().clear().apply();
    }

}