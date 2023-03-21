package com.brcm.apim.magtraining;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.ca.mas.foundation.MAS;
import com.ca.mas.foundation.MASCallback;
import com.ca.mas.foundation.MASConstants;
import com.ca.mas.foundation.MASDevice;
import com.ca.mas.foundation.MASSessionUnlockCallback;
import com.ca.mas.foundation.MASUser;
import com.ca.mas.foundation.auth.MASProximityLoginBLE;
import com.ca.mas.foundation.auth.MASProximityLoginBLEPeripheralListener;
import com.ca.mas.foundation.auth.MASProximityLoginBLEUserConsentHandler;
import com.google.zxing.integration.android.IntentIntegrator;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private String mEmulatorLocation=null;
    private Button mLoginButton=null;
    private Button mLogoutButton=null;

    private TextView mUserAuthenticatedStatus=null;

    private ProgressBar mProgressBar=null;
    private Button mSessionTransferButton=null;
    private Button mBleButton=null;

    private static final String TAG = MainActivity.class.getSimpleName();

    //
    // Background and Foreground Processing
    //
    private Timer mActivityTransitionTimer;
    private TimerTask mActivityTransitionTimerTask;
    private final long MAX_ACTIVITY_TRANSITION_TIME_MS = 600000;

    protected MASProximityLoginBLEPeripheralListener mBLEPeripheralListener;

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

        mProgressBar = findViewById( R.id.progressBar );
        mUserAuthenticatedStatus = findViewById( R.id.userStatusTextView );

        mSessionTransferButton = findViewById( R.id.sessionTransferButton );
        mBleButton = findViewById( R.id.bleTransferButton );

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
        // Set Up the various Button Listeners
        //

        //
        // Defined the Login Buttton listener
        //
        mLoginButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Log.d( TAG, "Login Button has been clicked" );

                MASUser.login( new MASCallback<MASUser>() {
                    @Override
                    public void onSuccess(MASUser masUser) {
                        Log.d( TAG, "User was successfully authenticated" );
                        refreshDialogStatus();
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.d( TAG, "User was failed to authenticate successfully" );
                    }
                } );
            }
        });

        //
        // Defined the Logout Buttton listener
        //

        mLogoutButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d( TAG, "Logout Button has been clicked" );

                if (MASUser.getCurrentUser() != null) {

                    final String lAuthenticatedUserName = MASUser.getCurrentUser().getUserName();

                    MASUser.getCurrentUser().logout( true, new MASCallback<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d( TAG, "User " + lAuthenticatedUserName + " has been logged out" );
                            refreshDialogStatus();
                        }

                        @Override
                        public void onError(Throwable throwable) {

                        }
                    } );
                }
            }
        } );

        //
        // Session Transfer Button Click Handler
        //
        mSessionTransferButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d( TAG, "Session Transfer Button Clicked" );

                IntentIntegrator intentIntegrator = new IntentIntegrator(MainActivity.this);
                intentIntegrator.initiateScan();
            }
        });

        //
        // Session Lock Listener Handler
        //

        //
        // Register the BLE Session Transfer Listener
        //

        mBleButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initiatePeripheralBLE();
                Log.d( TAG, "BLE Session Transfer Initiated" );
            }
        } );



    }

    protected void initiatePeripheralBLE() {
        if (mBLEPeripheralListener == null) {
            mBLEPeripheralListener = new MASProximityLoginBLEPeripheralListener() {
                @Override
                public void onStatusUpdate(int state) {
                    String result = "BLE ";
                    switch (state) {
                        case MASProximityLoginBLEPeripheralListener.BLE_STATE_CONNECTED:
                            result += "client connected";
                            break;
                        case MASProximityLoginBLEPeripheralListener.BLE_STATE_DISCONNECTED:
                            result += "client disconnected";
                            break;
                        case MASProximityLoginBLEPeripheralListener.BLE_STATE_STARTED:
                            result += "peripheral mode started";
                            break;
                        case MASProximityLoginBLEPeripheralListener.BLE_STATE_STOPPED:
                            result += "peripheral mode stopped";
                            break;
                        case MASProximityLoginBLEPeripheralListener.BLE_STATE_SESSION_AUTHORIZED:
                            result += "session authorized";
                            break;
                    }

                    Log.d(TAG, result);
                }

                @Override
                public void onError(int errorCode) {
                    String result = "BLE ";
                    switch (errorCode) {
                        case MASProximityLoginBLEPeripheralListener.BLE_ERROR_ADVERTISE_FAILED:
                            result += "advertise failed.";
                            break;
                        case MASProximityLoginBLEPeripheralListener.BLE_ERROR_AUTH_FAILED:
                            result += "authorize failed.";
                            break;
                        case MASProximityLoginBLEPeripheralListener.BLE_ERROR_CENTRAL_UNSUBSCRIBED:
                            result += "central unsubscribed.";
                            break;
                        case MASProximityLoginBLEPeripheralListener.BLE_ERROR_PERIPHERAL_MODE_NOT_SUPPORTED:
                            result += "peripheral mode not supported.";
                            break;
                        case MASProximityLoginBLE.BLE_ERROR_DISABLED:
                            result += "disabled.";
                            break;
                        case MASProximityLoginBLE.BLE_ERROR_INVALID_UUID:
                            result += "invalid UUID.";
                            break;
                        case MASProximityLoginBLE.BLE_ERROR_NOT_SUPPORTED:
                            result += "not supported.";
                            break;
                        case MASProximityLoginBLE.BLE_ERROR_SESSION_SHARING_NOT_SUPPORTED:
                            result += "session sharing not supported.";
                            break;
                        default:
                            result += "unknown errorCode " + errorCode;
                    }

                    Log.d(TAG, result);
                }

                @Override
                public void onConsentRequested(final Context context, final String deviceName, final MASProximityLoginBLEUserConsentHandler handler) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            AlertDialog.Builder builder = new AlertDialog.Builder(context);
                            builder.setMessage("Do you want to grant session to " + deviceName + "?")
                                    .setPositiveButton("Grant", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            // Authorize
                                            handler.proceed();
                                        }
                                    })
                                    .setNegativeButton("Reject", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            // Deny
                                            handler.cancel();
                                        }
                                    }).show();
                        }
                    });
                }
            };
        }

        MASDevice.getCurrentDevice().startAsBluetoothPeripheral(mBLEPeripheralListener);
        Log.d(TAG, "Started as peripheral.");
    }


    private int REQUEST_CODE = 0x1000;

    private MASSessionUnlockCallback<Void> getUnlockCallback(final MainActivity activity) {
        return new MASSessionUnlockCallback<Void>() {
            @Override
            public void onUserAuthenticationRequired() {

                KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
                Intent intent = keyguardManager.createConfirmDeviceCredentialIntent("Confirm your pattern",
                        "Please provide your credentials.");
                if (intent != null) {
                    startActivityForResult(intent, REQUEST_CODE);
                }

            }

            @Override
            public void onSuccess(Void result) {
                refreshDialogStatus();
            }

            @Override
            public void onError(Throwable e) {
                Log.d( TAG, e.toString() );
            }
        };
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

                            mSessionTransferButton.setEnabled( true );
                            mBleButton.setEnabled( true );
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
                            mSessionTransferButton.setVisibility( View.VISIBLE );
                            mBleButton.setVisibility( View.VISIBLE );

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
                        mSessionTransferButton.setEnabled( false );
                        mBleButton.setEnabled( false );
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
                    mSessionTransferButton.setVisibility( View.INVISIBLE );
                    mProgressBar.setVisibility( View.INVISIBLE);
                    mBleButton.setVisibility( View.INVISIBLE );
                    mLoginButton.setVisibility( View.VISIBLE);
                    mLoginButton.setEnabled( true );
                    setTitle( "Please Authenticate" );
                    mUserAuthenticatedStatus.setText( "Not Authenticated" );
                }
            } );
        }
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
            }
            else if(Build.VERSION.SDK_INT < Build.VERSION_CODES.S && (
                    criticalPermission.equals(Manifest.permission.BLUETOOTH_SCAN)
                    || criticalPermission.equals(Manifest.permission.BLUETOOTH_CONNECT))){
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

    //
    // Handle the App pause and resume events when it is put into the background and brought into foreground.
    //
    @Override
    public void onPause()
    {
        super.onPause();

        Log.d(TAG," App has paused");
    }

    @Override
    public void onResume()
    {
        super.onResume();

        //
        // To cater for in App Session Locking after a timeout period start the ActivityTimer
        //
        startInActivityTimer();

        MASUser currentUser = MASUser.getCurrentUser();
        if (currentUser != null && currentUser.isSessionLocked()) {
            MASUser.getCurrentUser().unlockSession(getUnlockCallback(this));
        }
        refreshDialogStatus();
        Log.d(TAG," App has resumed");
    }

    //
    // Two timers to monitor whether the app is really n the true background or just going through a transition to a subactivity
    //

    private void startInActivityTimer() {
        this.mActivityTransitionTimer = new Timer();
        this.mActivityTransitionTimerTask = new TimerTask() {
            public void run() {

                MASUser currentUser = MASUser.getCurrentUser();

                if (currentUser != null && !currentUser.isSessionLocked()) {
                    currentUser.lockSession( null );
                    Log.d(TAG, "User Session has been locked");
                    refreshDialogStatus();
                }
            }
        };

        this.mActivityTransitionTimer.schedule(mActivityTransitionTimerTask,
                MAX_ACTIVITY_TRANSITION_TIME_MS);
    }

    private void stopInActivityTimer() {
        if (this.mActivityTransitionTimerTask != null) {
            this.mActivityTransitionTimerTask.cancel();
        }

        if (this.mActivityTransitionTimer != null) {
            this.mActivityTransitionTimer.cancel();
        }

    }

    @Override
    public void onUserInteraction() {
        Log.d(TAG, "onUserInteraction");

        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);

        if (!tasks.get(0).topActivity.getPackageName().equals(getPackageName())) {
            // for some reason(HOME, BACK, RECENT APPS, etc.) the app is no longer visible
            // do your thing here
            Log.d(TAG, "App is no longer visible so it is in the background");

            MASUser currentUser = MASUser.getCurrentUser();

            if (currentUser != null && !currentUser.isSessionLocked()) {
                currentUser.lockSession( null );
                Log.d(TAG, "User Session is locked due to deliberate locking");
                stopInActivityTimer();
            }

        } else {
            //
            // Reset the timer here by stopping and starting it again, to reflect in-app interaction
            //
            stopInActivityTimer();
            startInActivityTimer();
        }
    }
}
