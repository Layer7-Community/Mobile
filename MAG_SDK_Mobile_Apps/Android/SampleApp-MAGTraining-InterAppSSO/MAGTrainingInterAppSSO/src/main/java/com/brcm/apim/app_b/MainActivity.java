package com.brcm.apim.app_b;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Application;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.brcm.apim.app_b.R;
import com.ca.mas.core.error.MAGError;
import com.ca.mas.core.policy.exceptions.LocationRequiredException;
import com.ca.mas.foundation.MAS;
import com.ca.mas.foundation.MASCallback;
import com.ca.mas.foundation.MASConfiguration;
import com.ca.mas.foundation.MASConstants;
import com.ca.mas.foundation.MASDevice;
import com.ca.mas.foundation.MASFileObject;
import com.ca.mas.foundation.MASProgressListener;
import com.ca.mas.foundation.MASRequest;
import com.ca.mas.foundation.MASResponse;
import com.ca.mas.foundation.MASSecurityConfiguration;
import com.ca.mas.foundation.MASSessionUnlockCallback;
import com.ca.mas.foundation.MASUser;
import com.ca.mas.foundation.MultiPart;
import com.ca.mas.foundation.auth.MASProximityLoginBLE;
import com.ca.mas.foundation.auth.MASProximityLoginBLEPeripheralListener;
import com.ca.mas.foundation.auth.MASProximityLoginBLEUserConsentHandler;
import com.ca.mas.foundation.auth.MASProximityLoginQRCode;
import com.google.android.material.snackbar.Snackbar;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;


public class MainActivity extends AppCompatActivity {


    private String mEmulatorLocation = null;
    private Button loginButtonWrongCredentials = null;
    private Button mLoginButton = null;
    private Button mLogoutButton = null;

    private TextView mUserAuthenticatedStatus = null;
    private ProgressBar mProgressBar = null;


    private static final String TAG = MainActivity.class.getSimpleName();

    //
    // Background and Foreground Processing
    //
    private Timer mActivityTransitionTimer;
    private TimerTask mActivityTransitionTimerTask;
    private final long MAX_ACTIVITY_TRANSITION_TIME_MS = 600000;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //
        // Prevents screenshotting of content in Recents
        //
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);


        setTitle("Please login.....");


        //
        // Initialise the various Dialog Element references
        //
        loginButtonWrongCredentials = findViewById(R.id.loginButtonWrongCredentials);
        mLoginButton = findViewById(R.id.loginButton);
        mLogoutButton = findViewById(R.id.logoutButton);
        mProgressBar = findViewById(R.id.progressBar);

        mUserAuthenticatedStatus = findViewById(R.id.userStatusTextView);

        mProgressBar.setVisibility(View.INVISIBLE);

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
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }

        try {

            TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            String networkOperator = tm.getNetworkOperatorName();
            if ("Android".equals(networkOperator)) {
                //
                // Emulator
                //
                LocationManager locationManager = (LocationManager) getApplicationContext()
                        .getSystemService(Context.LOCATION_SERVICE);
                Location lastKnownLocation = locationManager
                        .getLastKnownLocation(LocationManager.GPS_PROVIDER);

                if (lastKnownLocation != null) {
                    mEmulatorLocation = String.format("%f,%f", lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                    //                   mEmulatorLocation = String.format("%f,%f",47.6773745,-122.3250831);

                    Log.d(TAG, "Last Known Location: [" + mEmulatorLocation + "]");
                }

                Log.d(TAG, "Emulator is being used: [" + mEmulatorLocation + "]");
            } else
                Log.d(TAG, "Real device Location is being used: [" + mEmulatorLocation + "]");

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


        int myMasState = MAS.getState(this);

        if (myMasState == MASConstants.MAS_STATE_STARTED)
            Log.d(TAG, "MAS SDK Successfully started");

        //
        // Checking for connectivity
        //
        MAS.gatewayIsReachable(new MASCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean aBoolean) {
                Log.d(TAG, "MAS Server is reachable!");
            }

            @Override
            public void onError(Throwable throwable) {

            }
        });


        //
        // Setup the primary dialog such that it reflects the user status
        //
        refreshDialogStatus();


        loginButtonWrongCredentials.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (BuildConfig.DEBUG) {
                    CountingIdlingResourceSingleton.increment();
                }
                mProgressBar.setVisibility(View.VISIBLE);
                Log.d(TAG, "Login Button has been clicked");
                // Passing wrong user-name
                MASUser.login("spock1", "StRonG5^)".toCharArray(), new MASCallback<MASUser>() {
                    @Override
                    public void onSuccess(MASUser masUser) {
                        Log.d(TAG, "User was successfully authenticated");
                        mProgressBar.setVisibility(View.INVISIBLE);
                        if (BuildConfig.DEBUG) {
                            CountingIdlingResourceSingleton.decrement();
                        }
                        refreshDialogStatus();
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mProgressBar.setVisibility(View.INVISIBLE);
                                if (BuildConfig.DEBUG) {
                                    CountingIdlingResourceSingleton.decrement();
                                }
                                Toast.makeText(MainActivity.this, "User was failed to authenticate", Toast.LENGTH_LONG).show();
                            }
                        });
                        Log.d(TAG, "User was failed to authenticate");
                    }
                });


            }
        });

        //
        // Defined the Login Buttton listener
        //
        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (BuildConfig.DEBUG) {
                    CountingIdlingResourceSingleton.increment();
                }
                mProgressBar.setVisibility(View.VISIBLE);
                Log.d(TAG, "Login Button has been clicked");

                MASUser.login("spock", "StRonG5^)".toCharArray(), new MASCallback<MASUser>() {
                    @Override
                    public void onSuccess(MASUser masUser) {
                        Log.d(TAG, "User was successfully authenticated");
                        mProgressBar.setVisibility(View.INVISIBLE);
                        refreshDialogStatus();
                        if (BuildConfig.DEBUG) {
                            CountingIdlingResourceSingleton.decrement();
                        }
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mProgressBar.setVisibility(View.INVISIBLE);
                                if (BuildConfig.DEBUG) {
                                    CountingIdlingResourceSingleton.decrement();
                                }
                                Toast.makeText(MainActivity.this, "User was failed to authenticate", Toast.LENGTH_LONG).show();
                            }
                        });
                        Log.d(TAG, "User was failed to authenticate");
                    }
                });


            }
        });

        //
        // Defined the Logout Buttton listener
        //

        mLogoutButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if (BuildConfig.DEBUG) {
                    CountingIdlingResourceSingleton.increment();
                }
                Log.d(TAG, "Logout Button has been clicked");

                if (MASUser.getCurrentUser() != null) {

                    final String lAuthenticatedUserName = MASUser.getCurrentUser().getUserName();

                    MASUser.getCurrentUser().logout(false, new MASCallback<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "User " + lAuthenticatedUserName + " has been logged out");
                            refreshDialogStatus();
                            if (BuildConfig.DEBUG) {
                                CountingIdlingResourceSingleton.decrement();
                            }
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            if (BuildConfig.DEBUG) {
                                CountingIdlingResourceSingleton.decrement();
                            }
                        }
                    });
                }
            }
        });
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
                Log.d("State", e.toString());
            }
        };
    }


    /**
     * Process remote login through QRCode
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //
        // Got the QR Code, perform the remote login request.
        //
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (scanResult != null) {
            String r = scanResult.getContents();
            if (r != null) {
                MASProximityLoginQRCode.authorize(r, new MASCallback<Void>() {


                    @Override
                    public void onSuccess(Void result) {
                        Log.d(TAG, "QR login Succeeded");


                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
                                    alertDialog.setTitle("SUCCESS");
                                    alertDialog.setMessage("SSO between devices successfully completed");
                                    alertDialog.setPositiveButton("OK",
                                            new DialogInterface.OnClickListener() {

                                                @Override
                                                public void onClick(DialogInterface arg0, int arg1) {


                                                }
                                            });
                                    alertDialog.show();
                                } catch (Exception e) {
                                    Log.d(TAG, e.getMessage().toString());
                                    e.printStackTrace();
                                }
                            }
                        });
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, "Failed QR login" + e.getMessage());

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
                                    alertDialog.setTitle("ERROR");
                                    alertDialog.setMessage("SSO between devices successfully failed");
                                    alertDialog.setPositiveButton("OK",
                                            new DialogInterface.OnClickListener() {

                                                @Override
                                                public void onClick(DialogInterface arg0, int arg1) {
                                                }
                                            });
                                    alertDialog.show();
                                } catch (Exception e) {
                                    Log.d(TAG, e.getMessage().toString());
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                });
            }
        }
    }


    private void refreshDialogStatus() {

        final MASUser currentUser = MASUser.getCurrentUser();

        if (currentUser != null) {
            if (currentUser.isAuthenticated()) {
                Log.d(TAG, "MAS User Session is currently authenticated");

                //
                // Was Social Media Login used by the user
                //

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mLogoutButton.setEnabled(true);
                            if (currentUser.getUserName().toString().contains("google-")) {
                                String googleId = currentUser.getUserName().toString().substring(currentUser.getUserName().toString().indexOf("-") + 1);
                                String googleUserId = googleId.substring(googleId.indexOf("-") + 1);

                                mUserAuthenticatedStatus.setText("Authenticated User [" + googleUserId + "]");
                                setTitle("Authenticated [" + googleUserId + "]");
                            } else {
                                mUserAuthenticatedStatus.setText("Authenticated User [" + currentUser.getUserName().toString() + "]");
                                setTitle("Authenticated [" + currentUser.getUserName().toString() + "]");
                            }
                            mLogoutButton.setVisibility(View.VISIBLE);
                            mLoginButton.setVisibility(View.INVISIBLE);
                            loginButtonWrongCredentials.setVisibility(View.INVISIBLE);
                        } catch (Exception e) {
                            Log.d(TAG, e.getMessage().toString());
                            e.printStackTrace();
                        }

                    }
                });

            } else if (currentUser.isSessionLocked()) {
                Log.d(TAG, "MAS User Session is currently locked");

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mLogoutButton.setEnabled(false);
                        mLoginButton.setVisibility(View.INVISIBLE);
                        loginButtonWrongCredentials.setVisibility(View.INVISIBLE);
                        mUserAuthenticatedStatus.setText("User Session is Locked!");
                    }
                });


            }
        } else {    // An Authenticated User Session doesn't exist yet
            Log.d(TAG, "MAS User Session is not Authenticated");

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLogoutButton.setVisibility(View.INVISIBLE);
                    mProgressBar.setVisibility(View.INVISIBLE);
                    mLoginButton.setVisibility(View.VISIBLE);
                    loginButtonWrongCredentials.setVisibility(View.VISIBLE);
                    mLoginButton.setEnabled(true);
                    setTitle("Please Authenticate");
                    mUserAuthenticatedStatus.setText("Not Authenticated");
                }
            });


        }
    }

    //
    // Check the app permissions before startup of the App
    //
    final int PERMISSION_ALL = 1;

    private void checkAppPermissions() {

        String[] registeredPermissions = null;
        ArrayList<String> requiredPermissions = new ArrayList<String>();
        try {
            registeredPermissions = getApplicationContext().getPackageManager()
                    .getPackageInfo(getApplicationContext().getPackageName(), PackageManager.GET_PERMISSIONS)
                    .requestedPermissions;
            Log.d(TAG, "Got the manifest permissions");
        } catch (PackageManager.NameNotFoundException e) {

        }

        boolean permissionsNecessary = false;
        for (int permissionsCount = 0; permissionsCount < registeredPermissions.length; permissionsCount++) {
            if (ContextCompat.checkSelfPermission(this, registeredPermissions[permissionsCount]) != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                if (!permissionsNecessary)
                    permissionsNecessary = true;
                requiredPermissions.add(registeredPermissions[permissionsCount]);

            }
        }
        if (permissionsNecessary) {
            String[] requiredPermissionsStrArray = new String[requiredPermissions.size()];

            requiredPermissionsStrArray = requiredPermissions.toArray(requiredPermissionsStrArray);
            ActivityCompat.requestPermissions(this, requiredPermissionsStrArray, PERMISSION_ALL);
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
        for (int currGrant = 0; currGrant < grantResults.length; currGrant++) {
            if (grantResults[currGrant] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, permissions[currGrant] + " Permission Granted!", Toast.LENGTH_SHORT).show();
            } else {
                criticalPermission = permissions[currGrant];
                break;
            }
        }

        if (criticalPermission != null) {

            final String tempPermission = criticalPermission;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
                        alertDialog.setTitle("ERROR!!");
                        alertDialog.setMessage("Application will exit as mandatory permission [" + tempPermission + "] was denied");
                        alertDialog.setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface arg0, int arg1) {
                                        android.os.SystemClock.sleep(1000);
                                        moveTaskToBack(true);
                                        android.os.Process.killProcess(android.os.Process.myPid());
                                        System.exit(1);

                                    }
                                });
                        alertDialog.show();
                    } catch (Exception e) {
                        Log.d(TAG, e.getMessage().toString());
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
    public void onPause() {
        super.onPause();

        Log.d(TAG, " App has paused");
    }

    @Override
    public void onResume() {
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
        Log.d(TAG, " App has resumed");
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
                    currentUser.lockSession(null);
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
                currentUser.lockSession(null);
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
    //
    // End of the App Background Handling and screen locking code
    //

}

