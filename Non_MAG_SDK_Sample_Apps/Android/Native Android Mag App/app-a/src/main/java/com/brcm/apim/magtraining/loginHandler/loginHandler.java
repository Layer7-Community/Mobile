package com.brcm.apim.magtraining.loginHandler;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.brcm.apim.magtraining.MagSecureSharedPreferences;
import com.brcm.apim.magtraining.MainActivity;
import com.brcm.apim.magtraining.certHandler.certHandler;
import com.brcm.apim.magtraining.magClientConfiguration;
import com.brcm.apim.magtraining.myCallBackHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class loginHandler  extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static String magGatewayEndPointURL = null;
    private static String appMasterClientId = null;

    private static certHandler appCertHandler=null;


    private static Context magClientAppCtx=null;
    private static String magClientKeyPairAlias=null;

    private static magClientConfiguration magClientConfiguration=null;

    private static MagSecureSharedPreferences magSecureSharedPreferences = null;

    private static String magClientKeyStoreName = "AndroidKeyStore";


    @RequiresApi(api = Build.VERSION_CODES.N)
    public loginHandler(Context pAppContext, String pMagGatewayURL, String pMagClientAppId, String pMagClientKeyAlias, certHandler pCertificateHandler, magClientConfiguration pMagClientConfiguration) throws IOException {
        magClientAppCtx = pAppContext;
        magGatewayEndPointURL = pMagGatewayURL;
        appMasterClientId = pMagClientAppId;
        appCertHandler = pCertificateHandler;

        magClientKeyPairAlias = pMagClientKeyAlias;

        magSecureSharedPreferences = new MagSecureSharedPreferences(pAppContext);

        magClientConfiguration = pMagClientConfiguration;
    }

    public void initMagClient(final myCallBackHandler<Void> callBackHandler) {

        String postUrl = magGatewayEndPointURL + "/connect/client/initialize";
        RequestQueue requestQueue = Volley.newRequestQueue(magClientAppCtx);

        //
        // Do I have an MAG Server supplied Access Token?
        //

        if (magSecureSharedPreferences.get("magGeneratedAccessToken") == "") {


            StringRequest strRequest = new StringRequest(Request.Method.POST, postUrl,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            //
                            // Need to parse out the client_id and client_secret json object string
                            //

                            try {
                                final JSONObject responseJSON = new JSONObject(response);

                                //
                                // Store these away in the shared preferences
                                //

                                magSecureSharedPreferences.put("magOauthClientId", responseJSON.getString("client_id"));
                                magSecureSharedPreferences.put("magOauthClientSecret", responseJSON.getString("client_secret"));
                                magSecureSharedPreferences.put("magOauthClientExpiration", responseJSON.getString("client_expiration"));

                                Log.d(TAG, "LoginHandler - Returned Client Id [" + magSecureSharedPreferences.get("magOauthClientId") + ']');
                                Log.d(TAG, "LoginHandler - Returned Client Secret [" + magSecureSharedPreferences.get("magOauthClientSecret") + ']');
                                Log.d(TAG, "LoginHandler - Returned Client Expiration [" + magSecureSharedPreferences.get("magOauthClientExpiration") + ']');


                                //
                                // Step 3: Register the User Device with the MAG Server
                                //

                                registerMagClient(callBackHandler);


                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.d(TAG, "LoginHandler - Invalid received from MAG server");
                            error.printStackTrace();
                        }
                    }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> headers = new HashMap<>();

                    headers.put("device-id", Base64.encodeToString(magSecureSharedPreferences.get("magClientDeviceIdentifier").getBytes(), Base64.NO_WRAP));
                    return headers;
                }

                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<String, String>();

                    //
                    // Generate the random nonce required during initialisation
                    //
                    {
                        int leftLimit = 48; // numeral '0'
                        int rightLimit = 122; // letter 'z'
                        int targetStringLength = 25;
                        Random random = new Random();

                        String nonce = random.ints(leftLimit, rightLimit + 1)
                                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                                .limit(targetStringLength)
                                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                                .toString();
                        params.put("nonce", nonce);
                        Log.d(TAG, "LoginHandler - generated nounce is [" + nonce + ']');
                    }

                    params.put("client_id", appMasterClientId);
                    return params;
                }

                @Override
                public String getBodyContentType() {
                    return "application/x-www-form-urlencoded";
                }
            };
            requestQueue.add(strRequest);
        } else {
            Log.d( TAG, "User Session has already been established");
        }


    }


    private void registerMagClient(final myCallBackHandler<Void> callBackHandler) {

        String postUrl = magGatewayEndPointURL + "/connect/device/register";

        //
        // Do I have a valid MAG Server generated Client certificate which has not expired? If so then I skip this
        // registration process and invoke the token endpoint with the login-dialog collected user credentials instead.
        //
        boolean validMagClientCertificateExists = false;
        try {
            X509Certificate[] magRegisteredCertificateChain = appCertHandler.getCertificateChain(magClientKeyStoreName, magClientKeyPairAlias);
            if (magRegisteredCertificateChain != null && magRegisteredCertificateChain.length == 1) {
                int numInChain = 0;
                for (int i = 0; i < magRegisteredCertificateChain.length; i++) {
                    Date notAfter = magRegisteredCertificateChain[i].getNotAfter();
                    Log.d(TAG, "Encoded Client CSR is: \n [" + notAfter + "]");

                    Date currentTime = Calendar.getInstance().getTime();

                    if (currentTime.after(notAfter)) {
                        Log.d(TAG, "Expired Client Certificate found");
                    } else
                        validMagClientCertificateExists = true;
                }
            }
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }

        if (!validMagClientCertificateExists)
        {
            RequestQueue requestQueue = Volley.newRequestQueue(magClientAppCtx);

            Log.d(TAG, "Encoded Client CSR is: \n [" + appCertHandler.stringEncodedCSR() + "]");


            StringRequest strRequest = new StringRequest(Request.Method.POST, postUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //
                        // Check the device-status value to confirm it has been set to be activated
                        //

                        //
                        // Need to Extract the Id Token, id_token_type and mag-identifier
                        //

                        Log.d(TAG, "LoginHandler - MAG Client Certificate is: [\n" + response + ']');
                        Log.d(TAG, "LoginHandler - MAG Registration Identifier: [\n" + magSecureSharedPreferences.get("magRegistrationIdentifier") + ']');

                        magSecureSharedPreferences.put("magGeneratedClientCertificate", response);

                        //
                        // Store the MAG Client Certificate into the Android key Store
                        //
                        CertificateFactory certFactory = null;
                        try {
                            certFactory = CertificateFactory.getInstance("X.509");
                            ByteArrayInputStream bytes = new ByteArrayInputStream(response.getBytes());
                            try {
                                X509Certificate certificate = (X509Certificate) certFactory.generateCertificate(bytes);
                                X509Certificate[] certificateChain = new X509Certificate[1];
                                certificateChain[0] = certificate;
                                appCertHandler.saveCertificateChain(magClientKeyStoreName, magClientKeyPairAlias, certificateChain);

                            } catch (CertificateException | KeyStoreException e) {
                                e.printStackTrace();
                            }
                        } catch (CertificateException e) {
                            e.printStackTrace();
                        }


                        //
                        // Step 4: Retrieve the required Access Token
                        //

                        retrieveAccessToken(magClientAppCtx, callBackHandler, false);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "LoginHandler - Invalid response received from MAG server");

                        // Was this because of an invalid username/password - nullify them and restart the login process
                        callBackHandler.onError();
                        error.printStackTrace();
                    }
                }) {
                @Override
                protected Response<String> parseNetworkResponse(NetworkResponse response) {
                    String parsed;
                    try {

                        parsed = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
                     } catch (UnsupportedEncodingException e) {
                        parsed = new String(response.data);
                    }

                    magSecureSharedPreferences.put("magDeviceRegistrationStatus", response.headers.get("device-status"));
                    magSecureSharedPreferences.put("magClientIdToken", response.headers.get("id-token"));
                    magSecureSharedPreferences.put("magClientIdTokenType", response.headers.get("id-token-type"));
                    magSecureSharedPreferences.put("magRegistrationIdentifier", response.headers.get("mag-identifier"));
                    magSecureSharedPreferences.put("magServername", response.headers.get("Server"));

                    if (magSecureSharedPreferences.get("magDeviceRegistrationStatus").equals("activated"))
                        return Response.success(parsed, HttpHeaderParser.parseCacheHeaders(response));
                    else
                        return null;

                }

                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("device-id", Base64.encodeToString(magSecureSharedPreferences.get("magClientDeviceIdentifier").getBytes(), Base64.NO_WRAP));
                    headers.put("device-name", Base64.encodeToString(magSecureSharedPreferences.get("magClientDeviceName").getBytes(), Base64.NO_WRAP));

                    //
                    // User Authorization Header
                    //
                    String userCredentials = magSecureSharedPreferences.get("magUserName") + ":" + magSecureSharedPreferences.get("magUserPassword");
                    headers.put("Authorization", "Basic " + Base64.encodeToString(userCredentials.getBytes(), Base64.NO_WRAP));


                    //
                    // Client Authorization Header
                    //
                    String magClientCredentials = magSecureSharedPreferences.get("magOauthClientId") + ":" + magSecureSharedPreferences.get("magOauthClientSecret");
                    headers.put("client-authorization", "Basic " + Base64.encodeToString(magClientCredentials.getBytes(), Base64.NO_WRAP));

                    Log.d(TAG, "LoginHandler - client-authorisation header is [" + Base64.encodeToString(magClientCredentials.getBytes(), Base64.NO_WRAP) + ']');

                    return headers;
                }

                @Override
                public String getBodyContentType() {
                return "text/plain";
            }

                @Override
                public byte[] getBody() throws AuthFailureError {
                    return appCertHandler.stringEncodedCSR().getBytes();
                }
            };

        requestQueue.add(strRequest);

    } else
    {
        //
        // We have a valid certificate from a previus registeration - we just need to invoke the MAG server token
        // endpoint to get the Id Token and new access token, using the supplied user credentials
        //
        Log.d(TAG, "LoginHandler - we have a valid MAG Client Cert so no need to register  - need to get a new Id Token and Access Token");
        retrieveAccessToken(magClientAppCtx, callBackHandler, true);
    }

    }



    public static void retrieveAccessToken(Context context, final myCallBackHandler<Void> callBackHandler, boolean isIDTokenRequired)
    {
        String postUrl = magGatewayEndPointURL + "/auth/oauth/v2/token";
        RequestQueue requestQueue = Volley.newRequestQueue(context);

        StringRequest strRequest = new StringRequest(Request.Method.POST, postUrl,
                new Response.Listener<String>()
                {
                    @Override
                    public void onResponse(String response)
                    {

                        //
                        // Need to parse the MAG Server JSON response
                        //
                        final JSONObject responseJSON;
                        try {
                            responseJSON = new JSONObject(response);

                            //
                            // Set some retrieved values for the access token
                            //

                            magSecureSharedPreferences.put("magGeneratedAccessToken", responseJSON.getString("access_token"));
                            magSecureSharedPreferences.put("magGeneratedAccessTokenType", responseJSON.getString("token_type"));
                            magSecureSharedPreferences.put("magGeneratedAccessTokenExpiresIn", responseJSON.getString("expires_in"));
                            magSecureSharedPreferences.put("magGeneratedRefreshAccessToken", responseJSON.getString("refresh_token"));
                            magSecureSharedPreferences.put("magGeneratedAccessTokenScope", responseJSON.getString("scope"));

                            if (isIDTokenRequired) {
                                magSecureSharedPreferences.put("magClientIdToken", responseJSON.getString("id_token"));
                                magSecureSharedPreferences.put("magClientIdTokenType", responseJSON.getString("id_token_type"));
                            }



                            callBackHandler.onSuccess();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error)
                    {
                        Log.d( TAG, "LoginHandler - Invalid response received from MAG server");
                        //
                        // The refresh token could have also expired
                        //
                        callBackHandler.onError();
                    }
                })
        {
            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                String parsed;
                try {

                    parsed = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
                } catch (UnsupportedEncodingException e) {
                    parsed = new String(response.data);
                }

                return Response.success(parsed, HttpHeaderParser.parseCacheHeaders(response));


            }
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("mag-identifier", magSecureSharedPreferences.get("magRegistrationIdentifier"));
                return headers;
            }
            @Override
            protected Map<String, String> getParams()
            {
                Map<String, String> params = new HashMap<String, String>();


                params.put("scope",magClientConfiguration.getClientIdConfigOption("scope"));
                params.put("client_id",magSecureSharedPreferences.get("magOauthClientId"));
                params.put("client_secret",magSecureSharedPreferences.get("magOauthClientSecret"));


                if (isIDTokenRequired) {
                    params.put("grant_type","password");

                    params.put("username", magSecureSharedPreferences.get("magUserName"));
                    params.put("password", magSecureSharedPreferences.get("magUserPassword"));
                } else {
                    params.put("assertion",magSecureSharedPreferences.get("magClientIdToken"));
                    params.put("grant_type",magSecureSharedPreferences.get("magClientIdTokenType"));
                }

                return params;
            }
            @Override
            public String getBodyContentType() {
                return "application/x-www-form-urlencoded";
            }


        };

        requestQueue.add(strRequest);

    }

    public static void refreshAccessToken(final myCallBackHandler<Void> callBackHandler)
    {
        String postUrl = magGatewayEndPointURL + "/auth/oauth/v2/token";
        RequestQueue requestQueue = Volley.newRequestQueue(magClientAppCtx);

        StringRequest strRequest = new StringRequest(Request.Method.POST, postUrl,
                new Response.Listener<String>()
                {
                    @Override
                    public void onResponse(String response)
                    {

                        //
                        // Need to parse the MAG Server JSON response
                        //
                        final JSONObject responseJSON;
                        try {
                            responseJSON = new JSONObject(response);

                            //
                            // Set some retrieved values for the access token
                            //

                            magSecureSharedPreferences.put("magGeneratedAccessToken", responseJSON.getString("access_token"));
                            magSecureSharedPreferences.put("magGeneratedAccessTokenType", responseJSON.getString("token_type"));
                            magSecureSharedPreferences.put("magGeneratedAccessTokenExpiresIn", responseJSON.getString("expires_in"));
                            magSecureSharedPreferences.put("magGeneratedRefreshAccessToken", responseJSON.getString("refresh_token"));
                            magSecureSharedPreferences.put("magGeneratedAccessTokenScope", responseJSON.getString("scope"));

                            callBackHandler.onSuccess();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error)
                    {
                        //
                        // Check the error code. If 401 then try to refresh the AccessToken with the ID token instead.
                        //

                        Log.d( TAG, "LoginHandler - Invalid response received from MAG server");

                        // Was this because of an invalid username/password - nullify them adn restart the login process
                        error.printStackTrace();
                    }
                })
        {
            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                String parsed;
                try {

                    parsed = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
                } catch (UnsupportedEncodingException e) {
                    parsed = new String(response.data);
                }

                return Response.success(parsed, HttpHeaderParser.parseCacheHeaders(response));


            }
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                String clientCredentials = magSecureSharedPreferences.get("magOauthClientId") + ":" + magSecureSharedPreferences.get("magOauthClientSecret");
                headers.put("Authorization", "Basic " + Base64.encodeToString(clientCredentials.getBytes(), Base64.NO_WRAP));

                headers.put("mag-identifier", magSecureSharedPreferences.get("magRegistrationIdentifier"));
                return headers;
            }
            @Override
            protected Map<String, String> getParams()
            {
                Map<String, String> params = new HashMap<String, String>();

                params.put("grant_type","refresh_token");
                params.put("refresh_token", magSecureSharedPreferences.get("magGeneratedRefreshAccessToken"));

                return params;
            }
            @Override
            public String getBodyContentType() {
                return "application/x-www-form-urlencoded";
            }


        };

        requestQueue.add(strRequest);

    }

}
