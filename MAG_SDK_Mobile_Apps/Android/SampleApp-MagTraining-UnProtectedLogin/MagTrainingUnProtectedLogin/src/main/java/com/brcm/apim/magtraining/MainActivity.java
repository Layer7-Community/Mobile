package com.brcm.apim.magtraining;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ca.mas.core.policy.exceptions.LocationRequiredException;
import com.ca.mas.foundation.MAS;
import com.ca.mas.foundation.MASCallback;
import com.ca.mas.foundation.MASConfiguration;
import com.ca.mas.foundation.MASConstants;
import com.ca.mas.foundation.MASRequest;
import com.ca.mas.foundation.MASResponse;
import com.ca.mas.foundation.MASSecurityConfiguration;
import com.ca.mas.foundation.MASSecurityPinningMode;
import com.ca.mas.foundation.MASUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
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
    private Button mUnProtectedAPIButton=null;
    private TextView mJsonResponseTextView=null;
    private TextView mUserAuthenticatedStatus=null;
    private EditText username=null;
    private EditText passsword=null;
    private ProgressBar mProgressBar=null;
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );
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
        mProgressBar.setVisibility( View.INVISIBLE );
        username = findViewById( R.id.username);
        passsword = findViewById( R.id.password);

        username.setHint("User Name");
        passsword.setHint("Password");

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

        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MASUser.login( username.getText().toString(), passsword.getText().toString().toCharArray(), new MASCallback<MASUser>() {
                    @Override
                    public Handler getHandler() {
                        return new Handler(Looper.getMainLooper());
                    }
                    @Override
                    public void onSuccess(MASUser result) {
                        MASUser user = MASUser.getCurrentUser();
                        Toast.makeText(getApplicationContext(), "login in as "+user.getUserName(), Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(getApplicationContext(), "login in FAIL ", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

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
        // Set Up the various Button Listeners
        //


        //
        // Define the unprotected API Button listener
        //

        mUnProtectedAPIButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mJsonResponseTextView.setText( "" );

                //
                // URI is set from the msso_config.json file
                //

                String lunProtectedAPIUri = null;
                try {
                    JSONObject mssoConfigObject = new JSONObject(loadJSONFromAsset(getApplicationContext()));
                    lunProtectedAPIUri = mssoConfigObject.getJSONObject( "server" ).getString( "hostname" ) +
                     ':' + mssoConfigObject.getJSONObject( "server" ).getString( "port" );

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                Uri uri = new Uri.Builder().encodedAuthority( lunProtectedAPIUri )
                        .scheme( "https" )
                        .appendPath( "unprotected" ).appendPath( "products" )
                        .build();

                MASSecurityConfiguration configuration = new MASSecurityConfiguration.Builder()
                        .host( uri )
                        .allowSSLPinning(false)
                        .trustPublicPKI( true )
                        .build();

                MASConfiguration.getCurrentConfiguration().addSecurityConfiguration( configuration );

                MASRequest.MASRequestBuilder requestBuilder = new MASRequest.MASRequestBuilder( uri );

                //
                // Are we running this is an emulator or on a real device
                // Sometime sthe Emulator does not add in the location details automatically so we have to force it
                //

                if (mEmulatorLocation != null) // is location set because of emulator
                    requestBuilder.header( "geo-location", mEmulatorLocation );
                MASRequest request = requestBuilder.get().setPublic().build();
                MAS.invoke( request, new MASCallback<MASResponse<JSONObject>>() {

                    @Override
                    public Handler getHandler() {
                        return new Handler( Looper.getMainLooper() );
                    }

                    @Override
                    public void onSuccess(MASResponse<JSONObject> result) {

                        //
                        // Un-hide the logout Button in the main dialog screen
                        //

                        // Need to process the Json Payload

                        if (HttpURLConnection.HTTP_OK == result.getResponseCode()) {
                            String resultResponseType = result.getBody().getContentType();
                            if (resultResponseType.contains( "json" )) {
                                JSONObject jsonResponseBody = result.getBody().getContent();

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
                                } catch (JSONException e) {
                                    Log.e(TAG, e.getMessage());
                                }

                            }
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                       // Log.d( TAG, "Failed to retrieve un-protected resource data" );
                        if (e.getCause() instanceof LocationRequiredException) {
                            //Handle Error
                        }
                    }
                } );

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
                            mUnProtectedAPIButton.setEnabled( true );
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
                        mUnProtectedAPIButton.setEnabled( false );
                        mUserAuthenticatedStatus.setText( "User Session is Locked!" );
                    }
                } );
            }
        } else {
            // An Authenticated User Session doesn't exist yet
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

        String lTimeStamp = (String) json.get( "TimeStamp" );

        objects.add( "\n\nGateway TimeStamp: " + lTimeStamp + "\n" );

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
            if(criticalPermission.equals(Manifest.permission.READ_LOGS)){
                return;
            }else if(Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU &&
                    criticalPermission.equals(Manifest.permission.BLUETOOTH_SCAN)){
                return;
            }

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
}
