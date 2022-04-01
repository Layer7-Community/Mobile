package com.brcm.apim.magtraining;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ca.mas.core.policy.exceptions.LocationRequiredException;
import com.ca.mas.foundation.MAS;
import com.ca.mas.foundation.MASCallback;
import com.ca.mas.foundation.MASConstants;
import com.ca.mas.foundation.MASRequest;
import com.ca.mas.foundation.MASResponse;
import com.ca.mas.foundation.MASUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


public class MainActivity extends AppCompatActivity {


    private String mEmulatorLocation=null;
    private Button mLoginButton=null;
    private Button mLogoutButton=null;

    private TextView mJsonResponseTextView=null;
    private TextView mUserAuthenticatedStatus=null;
    private ProgressBar mProgressBar=null;

    private Button mProtectedAPIButton=null;
    public static boolean LOGIN_STATUS = false;

    private static final String TAG = MainActivity.class.getSimpleName();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );
        LOGIN_STATUS = false;

        //
        // Prevents screenshotting of content in Recents
        //
        getWindow().setFlags( WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);


        setTitle("Please login.....");

        //
        // Initialise the various Dialog Element references
        //

        mLoginButton = findViewById(R.id.loginButton);
        mLogoutButton = findViewById( R.id.logoutButton );

        mJsonResponseTextView = findViewById( R.id.jsonResponseData );
        mProgressBar = findViewById( R.id.progressBar );
        mUserAuthenticatedStatus = findViewById( R.id.userStatusTextView );

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
        // Check the App Permissions based on the contents of the Manifest File
        //

        checkAppPermissions();


        //
        // Start the MAS SDK
        //

        MAS.start(this, true);

        int myMasState = MAS.getState( this );

        if ( myMasState == MASConstants.MAS_STATE_STARTED )
            Log.d(TAG,"MAS SDK Successfully started");

        //
        // Checking for connectivity
        //
        MAS.gatewayIsReachable( new MASCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean aBoolean) {
                Log.d(TAG,"MAS Server is reachable!");
            }

            @Override
            public void onError(Throwable throwable) {

            }
        } );


        //
        // Setup the primary dialog such that it reflects the user status
        //
        refreshDialogStatus();

        //
        // Defined the Login Buttton listener
        //
        mLoginButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Log.d( TAG, "Login Button has been clicked" );
                if (BuildConfig.DEBUG) {
                    CountingIdlingResourceSingleton.increment();
                }

                mJsonResponseTextView.setText( "" );

//                MASUser.login("spock","StRonG5^)".toCharArray(), new MASCallback<MASUser>() {
                MASUser.login( new MASCallback<MASUser>() {
                    @Override
                    public void onSuccess(MASUser masUser) {
                        Log.d( TAG, "User was successfully authenticated" );
                        if (BuildConfig.DEBUG) {
                            CountingIdlingResourceSingleton.decrement();
                        }
                        refreshDialogStatus();
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.d( TAG, "User was failed to authenticate successfully" );
                        if (BuildConfig.DEBUG) {
                            CountingIdlingResourceSingleton.decrement();
                        }
                    }
                } );
            }
        });

        //
        // Defined the Logout Button listener
        //

        mLogoutButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d( TAG, "Logout Button has been clicked" );
                if (BuildConfig.DEBUG) {
                    CountingIdlingResourceSingleton.increment();
                }

                if (MASUser.getCurrentUser() != null) {

                    final String lAuthenticatedUserName = MASUser.getCurrentUser().getUserName();

                    MASUser.getCurrentUser().logout( true, new MASCallback<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d( TAG, "User " + lAuthenticatedUserName + " has been logged out" );
                            if (BuildConfig.DEBUG) {
                                CountingIdlingResourceSingleton.decrement();
                            }
                            refreshDialogStatus();
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            if (BuildConfig.DEBUG) {
                                CountingIdlingResourceSingleton.decrement();
                            }

                        }
                    } );
                }
            }
        } );


        //
        // Define the protected button listener
        //

        mProtectedAPIButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mJsonResponseTextView.setText( "" );

                Uri uri = new Uri.Builder().path("/protected/resource/products").appendQueryParameter("operation", "listProducts").build();

                //
                // Build the MAG gateway request
                //

                MASRequest.MASRequestBuilder requestBuilder = new MASRequest.MASRequestBuilder(uri);

                // Are we running this is an emulator or on a real device

                if (mEmulatorLocation != null) // is location set because of emulator
                    requestBuilder.header("geo-location", mEmulatorLocation);

                MASRequest request = requestBuilder.get().notifyOnCancel().build();

                Log.d( TAG, "Authenticate Button has been clicked" );

                mProgressBar.setVisibility(View.VISIBLE);

                MAS.invoke(request, new MASCallback<MASResponse<JSONObject>>() {

                    @Override
                    public Handler getHandler() {
                        return new Handler( Looper.getMainLooper());
                    }

                    @Override
                    public void onSuccess(MASResponse<JSONObject> result) {
                        Log.d( TAG, "Successsfully retrieved protected resource data" );
                        refreshDialogStatus();

                        //
                        // Un-hide the logout Button in the main dialog screen
                        //


                        mProgressBar.setVisibility(View.INVISIBLE);

                        // Need to process the Json Payload

                        if (HttpURLConnection.HTTP_OK == result.getResponseCode()) {
                            String resultResponseType = result.getBody().getContentType();
                            if (resultResponseType.contains( "json" )) {
                                JSONObject jsonResponseBody = result.getBody().getContent();
                                try {
                                    String jsonResponseBodyString = jsonResponseBody.toString( 2 );
                                    Log.d( TAG, "Successsfully retrieved protected resource data" + jsonResponseBodyString);

                                    try {
                                        List<String> objects = parseJsonResponseData(result.getBody().getContent());
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
                                        mJsonResponseTextView.setMovementMethod(new ScrollingMovementMethod());
                                    } catch (JSONException e) {

                                        Log.e(TAG, e.getMessage());
                                    }


                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        mProgressBar.setVisibility(View.INVISIBLE);
                        Log.d( TAG, "Failed to retrieve protected resource data" );
                        if (e.getCause() instanceof LocationRequiredException) {
                            //Handle Error
                        }
                    }
                });
            }
        } );
    }

    private void refreshDialogStatus() {

        final MASUser currentUser = MASUser.getCurrentUser();

        if (currentUser != null) {
            if (currentUser.isAuthenticated()) {
                Log.d( TAG, "MAS User Session is currently authenticated" );

                //
                // Was Social Media Login used by the user
                //

                runOnUiThread( new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mJsonResponseTextView.setText( "" );
                            mProtectedAPIButton.setEnabled( true );
                            mLogoutButton.setEnabled( true );

                            if (currentUser.getUserName().toString().contains( "google-" )) {
                                String googleId = currentUser.getUserName().toString().substring( currentUser.getUserName().toString().indexOf( "-" ) + 1 );
                                String googleUserId = googleId.substring( googleId.indexOf( "-" ) + 1 );

                                mUserAuthenticatedStatus.setText( "Authenticated User [" + googleUserId + "]" );
                                setTitle( "Authenticated [" + googleUserId + "]" );
                            } else {
                                mUserAuthenticatedStatus.setText( "Authenticated User [" + currentUser.getUserName().toString() + "]" );
                                setTitle( "Authenticated [" + currentUser.getUserName().toString() + "]" );
                            }
                            mLogoutButton.setVisibility( View.VISIBLE );
                            // Change the wording of the Protected APi button
                            mLoginButton.setVisibility( View.INVISIBLE);

                        } catch (Exception e) {
                            Log.d( TAG, e.getMessage().toString() );
                            e.printStackTrace();
                        }
                    }
                } );

            } else if (currentUser.isSessionLocked()) {
                Log.d( TAG, "MAS User Session is currently locked" );

                runOnUiThread( new Runnable() {
                    @Override
                    public void run() {
                        mLogoutButton.setEnabled( false );
                        mLoginButton.setVisibility( View.INVISIBLE );
                        mProtectedAPIButton.setEnabled( false );
                        mUserAuthenticatedStatus.setText( "User Session is Locked!" );
                    }
                } );
            }
        } else {    // An Authenticated User Session doesn't exist yet
            Log.d( TAG, "MAS User Session is not Authenticated" );

            runOnUiThread( new Runnable() {
                @Override
                public void run() {
                    mLogoutButton.setVisibility( View.INVISIBLE );
                    mProgressBar.setVisibility( View.INVISIBLE);
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
}
