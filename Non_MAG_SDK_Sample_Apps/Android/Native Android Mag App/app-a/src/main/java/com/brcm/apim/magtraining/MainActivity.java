package com.brcm.apim.magtraining;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.brcm.apim.magtraining.certHandler.certHandler;
import com.brcm.apim.magtraining.loginHandler.loginHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.spongycastle.pkcs.PKCS10CertificationRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.security.auth.x500.X500Principal;



public class MainActivity extends AppCompatActivity {

    private String mEmulatorLocation=null;
    private Button mLoginButton=null;
    private Button mLogoutButton=null;
    private Button mUnProtectedAPIButton=null;

    private TextView mJsonResponseTextView=null;
    private TextView mUserAuthenticatedStatus=null;
    private ProgressBar mProgressBar=null;
    private Button mDeRegisterButton=null;

    private Switch mScreenLockSwitch=null;
    private Button mProtectedAPIButton=null;

    private static loginHandler magGatewayLoginHandler = null;
    private static final String TAG = MainActivity.class.getSimpleName();


    private static final String MSSO_CLIENT_PRIVATE_KEY = "msso.clientCertPrivateKey";


    private static MagSecureSharedPreferences magSecureSharedPreferences = null;
    private static String magClientKeyStoreName = "AndroidKeyStore";

    private static String magServerCertificateAlias = "MAG Server Certificate";
    private static String magServerLocation=null;

    private magClientConfiguration  magConfiguration= null;

    private certHandler mCertificateHandler = new certHandler();


    public static final int RESULT_ENABLE = 11;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );


        //
        // Prevents screenshotting of content in Recents
        //
        getWindow().setFlags( WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        //
        // Check the App Permissions based on the contents of the Manifest File
        //

        checkAppPermissions();

        //
        // Lets load the contents of the MAG Manager supplied mss_config file so we can access its configuration
        //

        magConfiguration = new magClientConfiguration(this);

        magServerLocation = "https://" + magConfiguration.getMAGServerConfigOption("hostname") +':' + magConfiguration.getMAGServerConfigOption("port");

        magSecureSharedPreferences = new MagSecureSharedPreferences(this);
        try {
            magGatewayLoginHandler = new loginHandler(  this,
                    magServerLocation, magConfiguration.getClientIdConfigOption("client_id"),
                    MSSO_CLIENT_PRIVATE_KEY, mCertificateHandler, magConfiguration);

        } catch (IOException e) {
            Log.d(TAG, "MainActivity - Invalid Input setup parameters provided ");
            e.printStackTrace();
        }

        //
        // Add the MAG Server certificate into the local MAG Server keyStore so we can use it for subsequent server SSL requests
        //

        try {
                if (mCertificateHandler.getCertificateChain(magClientKeyStoreName,magServerCertificateAlias) == null) {
                    CertificateFactory certFactory = null;
                    certFactory = CertificateFactory.getInstance("X.509");
                    ByteArrayInputStream bytes = new ByteArrayInputStream(magConfiguration.getMAGServerConfigOption("server_certs").getBytes());

                    X509Certificate certificate = (X509Certificate) certFactory.generateCertificate(bytes);
                    X509Certificate[] certificateChain = new X509Certificate[1];
                    certificateChain[0] = certificate;
                    mCertificateHandler.saveCertificateChain(magClientKeyStoreName,"MAG Server Certificate", certificateChain);

                }
        } catch (CertificateException | KeyStoreException e) {
          e.printStackTrace();
        }

        setTitle("Please login.....");

        //
        // Initialise the various Dialog Element references
        //

        mLoginButton = findViewById(R.id.loginButton);
        mLogoutButton = findViewById( R.id.logoutButton );
        mUnProtectedAPIButton = findViewById(  R.id.unProtectedAPIButton );

        mJsonResponseTextView = findViewById( R.id.jsonResponseData );
        mProgressBar = findViewById( R.id.progressBar );
        mUserAuthenticatedStatus = findViewById( R.id.userStatusTextView );
        mDeRegisterButton = findViewById( R.id.deRegisterButton );

        mScreenLockSwitch = (Switch)findViewById( R.id.sessionLockSwitch );

        mProtectedAPIButton = (Button)findViewById( R.id.protectedAPIButton );

        mProgressBar.setVisibility( View.INVISIBLE );



        //
        // We need to ensure the location service is available on the emulator and real device
        //

        LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean enabled = service
                .isProviderEnabled(LocationManager.GPS_PROVIDER);

        // check if enabled and if not send user to the GSP settings
        // Better solution would be to display a dialog and suggesting to
        // go to the settings
        if (!enabled) {
            Intent intent = new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }

        try {

            TelephonyManager tm = (TelephonyManager)getSystemService( Context.TELEPHONY_SERVICE);
            String networkOperator = tm.getNetworkOperatorName();
            if ("Android".equals(networkOperator)) {
                //
                // Emulator
                //
                LocationManager locationManager = (LocationManager) getApplicationContext()
                        .getSystemService( Context.LOCATION_SERVICE );
                Location lastKnownLocation = locationManager
                        .getLastKnownLocation( LocationManager.GPS_PROVIDER );

                if (lastKnownLocation != null){
                    mEmulatorLocation = String.format("%f,%f", lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                    //                   mEmulatorLocation = String.format("%f,%f",47.6773745,-122.3250831);

                    Log.d(TAG,"Last Known Location: [" + mEmulatorLocation + "]");
                }

                Log.d(TAG,"Emulator is being used: [" + mEmulatorLocation + "]");
            } else
                Log.d(TAG,"Real device Location is being used: [" + mEmulatorLocation + "]");

        } catch (SecurityException e) {
            e.printStackTrace();
        }


        //
        // End of Location Handling
        //


        //
        // Setup the primary dialog such that it reflects the user status
        //
        refreshDialogStatus();


        //
        // Set Up the various Button Listeners
        //


        mScreenLockSwitch.setOnCheckedChangeListener(getLockListener(this));
        //
        // Define the unprotected API Button listener
        //

        mUnProtectedAPIButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mJsonResponseTextView.setText( "" );

                // Create a String request using Volley Library

                String myUrl = magServerLocation + "/unprotected/products";
                StringRequest myRequest = new StringRequest(Request.Method.GET, myUrl,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                try {
                                    Log.d( TAG, "Successsfully retrieved unprotected resource data" );
                                    //Create a JSON object containing information from the API.
                                    JSONObject jsonResponseObject = new JSONObject(response.toString());

                                    try {
                                        List<String> objects = parseJsonResponseData(jsonResponseObject);
                                        String objectString = "";
                                        int size = objects.size();
                                        for (int i = 0; i < size; i++) {
                                            objectString += objects.get(i);
                                            if (i != size - 1) {
                                                objectString += "\n";
                                            }
                                        }

                                        //
                                        // Display the json Payload Response Content
                                        //
                                        mJsonResponseTextView.setTextColor( Color.RED );
                                        mJsonResponseTextView.setText(objectString);
                                    } catch (JSONException e) {

                                        Log.e(TAG, e.getMessage());
                                    }

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError volleyError) {
                                Toast.makeText(
                                        MainActivity.this,
                                        volleyError.getMessage(),
                                        Toast.LENGTH_SHORT)
                                        .show();
                            }
                        });

                RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.super.getBaseContext());
                requestQueue.add(myRequest);
            }
        } );




        //
        // Defined the Login Buttton listener
        //
        mLoginButton.setOnClickListener( new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            public void onClick(View view) {

                Log.d( TAG, "Login Button has been clicked" );

                performLogin(view.getContext(), null);

                mJsonResponseTextView.setText( "" );

            }
        });



        //
        // Defined the Logout Buttton listener
        //

        mLogoutButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Logout Button has been clicked");
                mJsonResponseTextView.setText( "" );
                if (magSecureSharedPreferences.get("magGeneratedAccessToken") != "")
                    magSecureSharedPreferences.remove("magGeneratedAccessToken");

                    logoutCurrentUserSession(view.getContext(), new myCallBackHandler<Void>() {
                        @Override
                        public void onSuccess() {

                            refreshDialogStatus();

                            magSecureSharedPreferences.remove("magClientIdToken");
                            magSecureSharedPreferences.remove("magClientIdTokenType");
                            refreshDialogStatus();
                        }

                        @Override
                        public void onSuccess(String magUserName, String magUserPassword) {

                        }

                        @Override
                        public void onError() {
                            magSecureSharedPreferences.remove("magClientIdToken");
                            magSecureSharedPreferences.remove("magClientIdTokenType");

                            refreshDialogStatus();
                        }
                    });

                    //
                    // Destroy the Id token after invoking the gateway logout endpoint with the access token
                    //


                }

        } );


        //
        // Define the protected button listener
        //


        mProtectedAPIButton.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View view) {


                performLogin(view.getContext(), new myCallBackHandler<Void>() {
                    @Override
                    public void onSuccess() {
                        RequestQueue requestQueue=null;
                        {

                            try {
                                KeyStore magClientKeyStore = mCertificateHandler.getKeyStore(magClientKeyStoreName);


                                String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
                                TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
                                tmf.init(magClientKeyStore);

                                KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("X509");
                                keyManagerFactory.init(magClientKeyStore, null);

                                KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();

                                // Create an SSLContext that uses our TrustManager
                                SSLContext sslContext = SSLContext.getInstance("TLS");
                                sslContext.init(keyManagers, tmf.getTrustManagers(), null);

                                SSLSocketFactory sf = sslContext.getSocketFactory();
                                requestQueue = Volley.newRequestQueue(view.getContext(),new HurlStack(null, sf));

                            } catch (KeyStoreException e) {
                                e.printStackTrace();
                            } catch (CertificateException e) {
                                e.printStackTrace();
                            } catch (NoSuchAlgorithmException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (KeyManagementException e) {
                                e.printStackTrace();
                            } catch (UnrecoverableKeyException e) {
                                e.printStackTrace();
                            }

                        }
                        mJsonResponseTextView.setText( "" );

                        // Create a String request using Volley Library

                        String myUrl = magServerLocation + "/protected/resource/products?operation=listProducts";
                        StringRequest myProtectedRequest = new StringRequest(Request.Method.GET, myUrl,
                                new Response.Listener<String>() {
                                    @Override
                                    public void onResponse(String response) {
                                        try {
                                            Log.d( TAG, "Successsfully retrieved protected resource data" );
                                            //Create a JSON object containing information from the API.
                                            JSONObject jsonResponseObject = new JSONObject(response.toString());

                                            try {
                                                List<String> objects = parseJsonResponseData(jsonResponseObject);
                                                String objectString = "";
                                                int size = objects.size();
                                                for (int i = 0; i < size; i++) {
                                                    objectString += objects.get(i);
                                                    if (i != size - 1) {
                                                        objectString += "\n";
                                                    }
                                                }

                                                //
                                                // Display the json Payload Response Content
                                                //
                                                mJsonResponseTextView.setTextColor( Color.RED );
                                                mJsonResponseTextView.setText(objectString);
                                            } catch (JSONException e) {

                                                Log.e(TAG, e.getMessage());
                                            }

                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                },

                                new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError volleyError) {
                                        if ((volleyError.networkResponse != null) && (volleyError.networkResponse.statusCode == 401)) {
                                            Log.d( TAG, "Current Access Token needs to be renewed" );
                                            loginHandler.refreshAccessToken(new myCallBackHandler<Void>() {
                                                @Override
                                                public void onSuccess() {
                                                    mProtectedAPIButton.performClick();
                                                }

                                                @Override
                                                public void onSuccess(String magUserName, String magUserPassword) {

                                                }

                                                @Override
                                                public void onError() {
                                                    //
                                                    // Lets try to review using the ID token instead
                                                    //
                                                    loginHandler.retrieveAccessToken(view.getContext(), new myCallBackHandler<Void>() {
                                                        @Override
                                                        public void onSuccess() {
                                                            mProtectedAPIButton.performClick();
                                                        }

                                                        @Override
                                                        public void onSuccess(String magUserName, String magUserPassword) {

                                                        }

                                                        @Override
                                                        public void onError() {
                                                            Log.d(TAG, "Failed to renew the Access Token using the previous recieved IDToken");
                                                            Toast.makeText(
                                                                    MainActivity.this,
                                                                    "Error: Renewal of required access token failed",
                                                                    Toast.LENGTH_SHORT)
                                                                    .show();

                                                        }
                                                    }, false);
                                                }
                                            });

                                        }
                                        else {
                                            String errorMessage = volleyError.getMessage();
                                            if (errorMessage != null) {
                                                Toast.makeText(
                                                        MainActivity.this,
                                                        errorMessage,
                                                        Toast.LENGTH_LONG)
                                                        .show();
                                            } else {
                                                Toast.makeText(
                                                        MainActivity.this,
                                                        "Error: Failed to retrieve the response from the MAG Server",
                                                        Toast.LENGTH_LONG)
                                                        .show();
                                            }
                                        }
                                    }
                                })
                        {
                            @Override
                            public Map<String, String> getHeaders() throws AuthFailureError {
                                Map<String, String> headers = new HashMap<>();

                                headers.put("mag-identifier",magSecureSharedPreferences.get("magRegistrationIdentifier"));
                                headers.put("Authorization", "Bearer " + magSecureSharedPreferences.get("magGeneratedAccessToken"));
                                headers.put("geo-location", mEmulatorLocation);
                                headers.put("x-cert", Base64.encodeToString(magSecureSharedPreferences.get("magGeneratedClientCertificate").getBytes(), Base64.NO_WRAP));


                                return headers;
                            }
                            @Override
                            protected Map<String, String> getParams()
                            {
                                Map<String, String> params = new HashMap<String, String>();

                                // params.put("operation","listProducts");

                                return params;
                            }
                            @Override
                            public String getBodyContentType() {
                                return "text/plain";
                            }
                        };

                        requestQueue.add(myProtectedRequest);

                    }

                    @Override
                    public void onSuccess(String magUserName, String magUserPassword) {

                    }

                    @Override
                    public void onError() {

                    }
                } );


            }
        });

        mDeRegisterButton.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick (View view){
                Log.d(TAG, "De-Register Button has been clicked");
                String postUrl = magServerLocation + "/connect/device/remove";

                Log.d(TAG, "De-Register URL is: [" + postUrl + ']');

                RequestQueue requestQueue=null;

                //
                // Tell the MAG server to de-register the device and then
                // destroy the primary keystore where the MAG Client private key is
                // and everything in the encrypted SharedPreferences
                //

                {


                    try {
                        KeyStore magClientKeyStore = mCertificateHandler.getKeyStore(magClientKeyStoreName);


                        String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
                        TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
                        tmf.init(magClientKeyStore);

                        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("X509");
                        keyManagerFactory.init(magClientKeyStore, null);

                        KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();

                        // Create an SSLContext that uses our TrustManager
                        SSLContext sslContext = SSLContext.getInstance("TLS");
                        sslContext.init(keyManagers, tmf.getTrustManagers(), null);

                        SSLSocketFactory sf = sslContext.getSocketFactory();
                        requestQueue = Volley.newRequestQueue(MainActivity.super.getBaseContext(),new HurlStack(null, sf));

                    } catch (KeyStoreException e) {
                        e.printStackTrace();
                    } catch (CertificateException e) {
                        e.printStackTrace();
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (KeyManagementException e) {
                        e.printStackTrace();
                    } catch (UnrecoverableKeyException e) {
                        e.printStackTrace();
                    }


                }


                // Create a String request using Volley Library

                StringRequest myDeviceRemoveRequest = new StringRequest(Request.Method.DELETE, postUrl,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {

                                mJsonResponseTextView.setText( "" );

                                Log.d( TAG, "Successsfully De-Registered the MAG Client" );

                                //
                                // Destroy the entire contents of the Encrypted Shared Preferences
                                //
                                magSecureSharedPreferences.destroy();

                                //
                                // Destroy the private key
                                //
                                mCertificateHandler.deleteKey(magClientKeyStoreName,MSSO_CLIENT_PRIVATE_KEY);
                                mCertificateHandler.deleteCertificateChain(magClientKeyStoreName,MSSO_CLIENT_PRIVATE_KEY);

                                refreshDialogStatus();

                            }
                        },

                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError volleyError) {
                                String errorMessage = volleyError.getMessage();
                                if (errorMessage != null) {
                                    Toast.makeText(
                                            MainActivity.this,
                                            errorMessage,
                                            Toast.LENGTH_SHORT)
                                            .show();
                                } else {
                                    Toast.makeText(
                                            MainActivity.this,
                                            "Error: Failed to retrieve the response from the MAG Server",
                                            Toast.LENGTH_LONG)
                                            .show();
                                }

                                //
                                // Destroy the entire contents of the Encrypted Shared Preferences
                                //
                                magSecureSharedPreferences.destroy();

                                //
                                // Destroy the private key
                                //
                                mCertificateHandler.deleteKey(magClientKeyStoreName,MSSO_CLIENT_PRIVATE_KEY);
                                mCertificateHandler.deleteCertificateChain(magClientKeyStoreName,MSSO_CLIENT_PRIVATE_KEY);

                                refreshDialogStatus();
                            }
                        })
                {
                    @Override
                    public Map<String, String> getHeaders() throws AuthFailureError {
                        Map<String, String> headers = new HashMap<>();

                        headers.put("mag-identifier",magSecureSharedPreferences.get("magRegistrationIdentifier"));
                        headers.put("Authorization", "Bearer " + magSecureSharedPreferences.get("magGeneratedAccessToken"));
                        headers.put("geo-location", mEmulatorLocation);
                        headers.put("x-cert", Base64.encodeToString(magSecureSharedPreferences.get("magGeneratedClientCertificate").getBytes(), Base64.NO_WRAP));

                        return headers;
                    }

                    @Override
                    public String getBodyContentType() {
                        return "text/plain";
                    }
                };

                requestQueue.add(myDeviceRemoveRequest);


            }

        });


    }


    static void logoutCurrentUserSession(Context context, final myCallBackHandler<Void> callBackHandler)
    {
        Log.d( TAG, "Logging out the User Session with the MAG Gateway Server" );
        String postUrl = magServerLocation + "/connect/session/logout";
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


                            String sessionStatus = responseJSON.getString("session_status");

                            Log.d( TAG, "MainActivity - LogOut Status is: " + sessionStatus);


                            if (sessionStatus.compareTo("logged out") == 0) {
                                callBackHandler.onSuccess();
                            }
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
                        Log.d( TAG, "MainActivity - Invalid response received from MAG server");

                        callBackHandler.onError();
                    }
                })
        {
            /*
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
            */

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


                params.put("client_id",magSecureSharedPreferences.get("magOauthClientId"));
                params.put("client_secret",magSecureSharedPreferences.get("magOauthClientSecret"));

                params.put("id_token",magSecureSharedPreferences.get("magClientIdToken"));
                params.put("id_token_type",magSecureSharedPreferences.get("magClientIdTokenType"));
                params.put("logout_apps","true");

                return params;
            }
            @Override
            public String getBodyContentType() {
                return "application/x-www-form-urlencoded";
            }


        };

        requestQueue.add(strRequest);


    }


    private void performLogin(Context ctx,final myCallBackHandler<Void> handler)
    {

        if (magSecureSharedPreferences.get("magGeneratedAccessToken") == "" )
        {
            // Step 1 - We need a key pair for this MAG Client instance and then get a certificate for it

            if (mCertificateHandler.getPrivateKey(magClientKeyStoreName, MSSO_CLIENT_PRIVATE_KEY) == null) {
                mCertificateHandler.generateKeyPair(magClientKeyStoreName, MSSO_CLIENT_PRIVATE_KEY);
            }

            //
            // Need to gather the user credentials: username and password
            //
            showLoginDialog((Activity) ctx, new myCallBackHandler<Void>() {
                @Override
                public void onSuccess() {
                    // Do nothing
                }

                @Override
                public void onSuccess(String magUserName, String magUserPassword) {


                    magSecureSharedPreferences.put("magUserName", magUserName);
                    magSecureSharedPreferences.put("magUserPassword", magUserPassword);

                    // Need to create the certificate DN from username, device IS, device Name and organisation
                    String magUserCommonName = magUserName.replace("\"", "\\\"");


                    //
                    // Generate the Device Identifier if one doesn't exist already
                    //
                    if (magSecureSharedPreferences.get("magClientDeviceIdentifier") == "") {
                        String magClientDeviceId = mCertificateHandler.generateMagClientDeviceIdentifier();
                        magClientDeviceId = magClientDeviceId.replace("\"", "\\\"");
                        magSecureSharedPreferences.put("magClientDeviceIdentifier", magClientDeviceId);
                    }

                    //
                    // Handle the MAG Client Device Name
                    //

                    if (magSecureSharedPreferences.get("magClientDeviceName") == "") {
                        String magClientDeviceName = android.os.Build.MODEL;
                        if (magClientDeviceName.isEmpty())
                            magClientDeviceName = "Undefined";

                        magClientDeviceName = magClientDeviceName.replace("\"", "\\\"");
                        magSecureSharedPreferences.put("magClientDeviceName", magClientDeviceName);
                    }


                    String magOrganziationName = "Broadcom Software";
                    magOrganziationName = magOrganziationName.replace("\"", "\\\"");

                    X500Principal magClientSubjectDN = new X500Principal("cn=\"" + magUserCommonName +
                            "\", ou=\"" + magSecureSharedPreferences.get("magClientDeviceIdentifier") +
                            "\", o=\"" + magOrganziationName +
                            "\", dc=\"" + magSecureSharedPreferences.get("magClientDeviceName") + "\"");

                    PKCS10CertificationRequest certificateRequest = mCertificateHandler.generateCSR(magClientKeyStoreName,MSSO_CLIENT_PRIVATE_KEY,magClientSubjectDN);

                    Log.d(TAG, "Encoded CSR is: \n [" + mCertificateHandler.stringEncodedCSR() + "]");

                    magGatewayLoginHandler.initMagClient(new myCallBackHandler<Void>() {
                        @Override
                        public void onSuccess() {

                            refreshDialogStatus();
                            if (handler != null)
                                handler.onSuccess();

                        }

                        @Override
                        public void onSuccess(String magUserName, String magUserPassword) {

                        }

                        @Override
                        public void onError() {
                            Toast.makeText( MainActivity.this, "Invalid User Credentials Provided", Toast.LENGTH_LONG ).show();
                        }
                    });


                }

                @Override
                public void onError() {

                }
            });

        } else {
            refreshDialogStatus();
            if (handler != null)
                handler.onSuccess();
        }
    }

    public void refreshDialogStatus() {

           if (magSecureSharedPreferences.get("magGeneratedAccessToken") != "") {
                Log.d( TAG, "MAG User Session is currently authenticated" );

                String currentUser = magSecureSharedPreferences.get("magUserName");

                //
                // Was Social Media Login used by the user
                //

                runOnUiThread( new Runnable() {
                    @Override
                    public void run() {
                        try {

                            mJsonResponseTextView.setText( "" );
                            mUnProtectedAPIButton.setEnabled( true );
                            mProtectedAPIButton.setEnabled( true );
                            mDeRegisterButton.setEnabled( true );
                            mLogoutButton.setEnabled( true );
                            mScreenLockSwitch.setChecked( false );

                            if (currentUser.contains( "google-" )) {
                                String googleId = currentUser.substring( currentUser.indexOf( "-" ) + 1 );
                                String googleUserId = googleId.substring( googleId.indexOf( "-" ) + 1 );

                                mUserAuthenticatedStatus.setText( "Authenticated User [" + googleUserId + "]" );
                                setTitle( "Authenticated [" + googleUserId + "]" );
                            } else {
                                mUserAuthenticatedStatus.setText( "Authenticated User [" + currentUser + "]" );
                                setTitle( "Authenticated [" + currentUser + "]" );
                            }
                            mLogoutButton.setVisibility( View.VISIBLE );
                            mDeRegisterButton.setVisibility( View.VISIBLE );

                            // Change the wording of the Protected APi button

                            mLoginButton.setVisibility( View.INVISIBLE);
                            mScreenLockSwitch.setVisibility( View.VISIBLE );

                        } catch (Exception e) {
                            Log.d( TAG, e.getMessage().toString() );
                            e.printStackTrace();
                        }

                    }
                } );

            } else {
               Log.d( TAG, "MAS User Session is not Authenticated" );

               runOnUiThread( new Runnable() {
                   @Override
                   public void run() {
                       mLogoutButton.setVisibility( View.INVISIBLE );
                       mDeRegisterButton.setVisibility( View.INVISIBLE );
                       mProgressBar.setVisibility( View.INVISIBLE);
                       mScreenLockSwitch.setVisibility( View.INVISIBLE );
                       mLoginButton.setVisibility( View.VISIBLE);
                       mLoginButton.setEnabled( true );
                       mJsonResponseTextView.setText( "" );
                       setTitle( "Please Authenticate" );
                       mUserAuthenticatedStatus.setText( "Not Authenticated" );
                   }
               } );
           }


    }

    private static List<String> parseJsonResponseData(JSONObject json) throws JSONException {
        List<String> objects = new ArrayList<>();



        try {
            if (json.has( "products" )) {
                JSONArray items = json.getJSONArray( "products" );
                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = (JSONObject) items.get( i );
                    Integer id = (Integer) item.get( "id" );
                    String name = (String) item.get( "name" );
                    String price = (String) item.get( "price" );
                    objects.add( id + ": " + name + ", $" + price );
                }
            } else if (json.has( "TimeStamp" )) {
                String lTimeStamp = (String) json.get( "TimeStamp" );

                objects.add( "\nGateway TimeStamp: " + lTimeStamp + "\n" );
            }


        } catch (ClassCastException e) {
            throw (JSONException) new JSONException("Response JSON was not in the expected format").initCause(e);
        }

        return objects;
    }


    //
    // Check the app permissions before startup of the App
    //
    final int PERMISSION_ALL=1;

    private void checkAppPermissions() {

        String[] registeredPermissions = null;
        ArrayList<String> requiredPermissions = new ArrayList<String>();
        try
        {
            registeredPermissions= getApplicationContext().getPackageManager()
                    .getPackageInfo( getApplicationContext().getPackageName(), PackageManager.GET_PERMISSIONS )
                    .requestedPermissions;
            Log.d(TAG, "Got the manifest permissions");
        } catch (PackageManager.NameNotFoundException e) {

        }

        boolean permissionsNecessary=false;
        for (int permissionsCount=0; permissionsCount < registeredPermissions.length; permissionsCount++) {
            if (ContextCompat.checkSelfPermission(this, registeredPermissions[permissionsCount]) != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                if (!permissionsNecessary)
                    permissionsNecessary = true;
                requiredPermissions.add( registeredPermissions[permissionsCount] );

            }
        }
        if (permissionsNecessary) {
            String[] requiredPermissionsStrArray = new String[requiredPermissions.size()];

            requiredPermissionsStrArray = requiredPermissions.toArray(requiredPermissionsStrArray);
            ActivityCompat.requestPermissions( this, requiredPermissionsStrArray, PERMISSION_ALL );
        }

    }

    //
    // Handle the Event if some permissions are not granted
    //
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String permissions[],
            int[] grantResults) {


        String criticalPermission = null;
        for (int currGrant=0; currGrant < grantResults.length; currGrant++)
        {
            if (grantResults[currGrant] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText( MainActivity.this, permissions[currGrant] + " Permission Granted!", Toast.LENGTH_SHORT ).show();
            } else {
                criticalPermission = permissions[currGrant];
                break;
            }

        }

        if (criticalPermission != null) {

            final String tempPermission = criticalPermission;
            runOnUiThread( new Runnable() {
                @Override
                public void run() {
                    try {
                        AlertDialog.Builder alertDialog = new AlertDialog.Builder( MainActivity.this );
                        alertDialog.setTitle( "ERROR!!" );
                        alertDialog.setMessage( "Application will exit as mandatory permission [" + tempPermission + "] was denied" );
                        alertDialog.setPositiveButton( "OK",
                                new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface arg0, int arg1) {
                                        android.os.SystemClock.sleep( 1000 );
                                        moveTaskToBack( true );
                                        android.os.Process.killProcess( android.os.Process.myPid() );
                                        System.exit( 1 );

                                    }
                                } );
                        alertDialog.show();
                    } catch (Exception e) {
                        Log.d( TAG, e.getMessage().toString() );
                        e.printStackTrace();
                    }

                }

            });
        }

    }

    //
    // Handle the App pause and resume events when it is put into the background and brought into foreground.
    //
    @Override
    public void onPause()
    {
        super.onPause();

        //
        // If the user is logged out then log them out, don't necessarily log them out when the locked screen session resumes
        //
        if (!mScreenLockSwitch.isChecked())
            mLogoutButton.performClick();
        Log.d(TAG," App has paused");
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if (mScreenLockSwitch.isChecked())
            mScreenLockSwitch.setChecked(false);

        Log.d(TAG," App has resumed");
    }

    private String loadJSONFromAsset(Context context) {
        String json = null;
        try {
            InputStream is = context.getAssets().open("msso_config.json");

            int size = is.available();

            byte[] buffer = new byte[size];

            is.read(buffer);

            is.close();

            json = new String(buffer, "UTF-8");


        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;

    }




    private void showLoginDialog(Activity ctx, final myCallBackHandler<Void> handler) {
        AlertDialog.Builder alert;

        // Have we previously authenticated successfully?

        if (magSecureSharedPreferences.get("magGeneratedAccessToken") == "") {


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                alert = new AlertDialog.Builder(ctx, android.R.style.Theme_Material_Dialog_Alert);
            } else {
                alert = new AlertDialog.Builder(ctx);
            }

            LayoutInflater inflater = ctx.getLayoutInflater();
            View view = inflater.inflate(R.layout.login_dialog, null);

            TextView username = view.findViewById(R.id.etUserName);
            TextView password = view.findViewById(R.id.etUserPassword);

            Button btnLogin = view.findViewById(R.id.LoginButton);
            Button dismissLogin = view.findViewById(R.id.DismissButton);
            alert.setView(view);
            alert.setCancelable(false);

            AlertDialog dialog = alert.create();
            dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            dialog.show();

            btnLogin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "Login Button clicked");

                    handler.onSuccess(username.getText().toString(),password.getText().toString());
                    dialog.dismiss();
                }
            });
            dismissLogin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "Dismiss Button clicked");
                    dialog.dismiss();
                }
            });

        }
    }

    private Switch.OnCheckedChangeListener getLockListener(final MainActivity activity) {
        return new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {
                    //
                    // For handling screen locking
                    //
                    DevicePolicyManager devicePolicyManager;
                    ComponentName compName;

                    compName = new ComponentName(MainActivity.this, DeviceAdmin.class);
                    devicePolicyManager = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
                    boolean active = devicePolicyManager.isAdminActive(compName);
                    if (!active) {
                        Toast.makeText(MainActivity.this, "You need to enable the Admin Device Features", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName);
                        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Additional text explaining why we need this permission");
                        startActivityForResult(intent, RESULT_ENABLE);
                    }
                    devicePolicyManager.lockNow();

                    Log.d(TAG, "User Session is locked due to user throwing the dialog switch");


                } else {
                    Log.d(TAG, "User Session is unlocked ");
                }

            }
        };
    }
}
