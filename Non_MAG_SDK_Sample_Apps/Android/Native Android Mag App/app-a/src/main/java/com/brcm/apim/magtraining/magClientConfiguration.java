package com.brcm.apim.magtraining;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class magClientConfiguration {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static JSONObject magJsonConfig = null;

    public magClientConfiguration (Context ctx)  {
        String jsonFileString = Utils.getJsonFromAssets(ctx, "msso_config.json");
        Log.d(TAG,jsonFileString);
        try {
            magJsonConfig = new JSONObject(jsonFileString);

        } catch (JSONException e) {
            Log.d(TAG,"MAG JSON config file is invalid");
        }

    }

    public static String getMAGServerConfigOption(String configOption) {

        try {

            JSONObject magServerConfig  = magJsonConfig.getJSONObject("server");
            if (configOption != "server_certs" )
                return magServerConfig.getString(configOption);
            else {
                JSONArray magServerCerts = magServerConfig.getJSONArray("server_certs");
                JSONArray magServerCertFinal = magServerCerts.getJSONArray(0);

                String flattenedPemMagServerCertificate = null;

                for (int i = 0; i < magServerCertFinal.length(); i++) {
                    if (i == 0)
                        flattenedPemMagServerCertificate = magServerCertFinal.getString(i) + "\n";
                    else
                        flattenedPemMagServerCertificate = flattenedPemMagServerCertificate + magServerCertFinal.getString(i) + "\n";

                }

                return flattenedPemMagServerCertificate;
            }

        } catch (JSONException e) {
            Log.d(TAG,"MAG JSON config file doesn't contain the MAG " + configOption);
            return null;
        }

    }
    public static String getClientIdConfigOption(String configOption) {

        try {

            JSONObject magOauthConfig  = magJsonConfig.getJSONObject("oauth");
            JSONObject magClientConfig  = magOauthConfig.getJSONObject("client");

            JSONArray magClientIds = magClientConfig.getJSONArray("client_ids");

            return magClientIds.getJSONObject(0).getString(configOption);

        } catch (JSONException e) {
            Log.d(TAG,"MAG JSON config file doesn't contain the MAG " + configOption);
            return null;
        }

    }


}

