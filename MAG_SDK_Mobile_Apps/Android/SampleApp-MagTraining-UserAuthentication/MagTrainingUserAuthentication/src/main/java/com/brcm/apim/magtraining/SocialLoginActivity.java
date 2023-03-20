package com.brcm.apim.magtraining;

import android.app.ActivityManager;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.ca.mas.core.log.MASLoggerConfiguration;
import com.ca.mas.core.service.MssoIntents;
import com.ca.mas.foundation.MAS;
import com.ca.mas.foundation.MASAuthenticationListener;
import com.ca.mas.foundation.MASCallback;
import com.ca.mas.foundation.MASConfiguration;
import com.ca.mas.foundation.MASConstants;
import com.ca.mas.foundation.MASDevice;
import com.ca.mas.foundation.MASOtpAuthenticationHandler;
import com.ca.mas.foundation.MASSessionUnlockCallback;
import com.ca.mas.foundation.MASUser;
import com.ca.mas.foundation.auth.MASAuthenticationProviders;
import com.ca.mas.ui.MASLoginActivity;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class SocialLoginActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private String mEmulatorLocation = null;
    private Timer mActivityTransitionTimer;
    private TimerTask mActivityTransitionTimerTask;
    private final long MAX_ACTIVITY_TRANSITION_TIME_MS = 600000;
    Button btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_social_login);
        btnLogout = findViewById(R.id.buttonLogout);

        //
        // Prevents screenshotting of content in Recents
        //
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

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
        MASConfiguration.getMASLoggerConfiguration().setLogLevel(MASLoggerConfiguration.MASLogLevel.VERBOSE);
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

        btnLogout.setOnClickListener(new View.OnClickListener() {
            final Dialog progress = new ProgressDialog(SocialLoginActivity.this);
            @Override
            public void onClick(View v) {
                if (btnLogout.getText().equals("Login")) {
                    btnLogout.setText("Logout");
                    refreshDialogStatus();
                } else {
                    progress.setTitle("Please wait...");
                    progress.show();
                    MASDevice masDevice = MASDevice.getCurrentDevice();
                    if (masDevice != null) {
                        masDevice.deregister(new MASCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                progress.dismiss();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        setTitle("Please Authenticate");
                                        btnLogout.setText("Login");
                                        Toast.makeText(getApplicationContext(), "Successfully Deregistered.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }

                            @Override
                            public void onError(Throwable e) {
                                progress.dismiss();
                                //Toast.makeText(SocialLoginActivity.this, "Something went wrong...", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }else {
                        progress.dismiss();
                        Toast.makeText(SocialLoginActivity.this, "No user logged in...", Toast.LENGTH_SHORT).show();
                    }

                }
            }
        });


    }


    private final int REQUEST_CODE = 0x1000;

    private MASSessionUnlockCallback<Void> getUnlockCallback(final SocialLoginActivity activity) {
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
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        boolean permissionsNecessary = false;
        assert registeredPermissions != null;
        for (String registeredPermission : registeredPermissions) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(this, registeredPermission) != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                if (!permissionsNecessary)
                    permissionsNecessary = true;
                requiredPermissions.add(registeredPermission);
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
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {


        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        String criticalPermission = null;
        for (int currGrant = 0; currGrant < grantResults.length; currGrant++) {
            if (grantResults[currGrant] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(SocialLoginActivity.this, permissions[currGrant] + " Permission Granted!", Toast.LENGTH_SHORT).show();
            } else {
                criticalPermission = permissions[currGrant];
                break;
            }
        }

       /* if (criticalPermission != null) {

            final String tempPermission = criticalPermission;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(SocialLoginActivity.this);
                        alertDialog.setTitle("ERROR!!");
                        alertDialog.setMessage("Application will exit as mandatory permission [" + tempPermission + "] was denied.");
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
                        Log.d(TAG, e.getMessage());
                        e.printStackTrace();
                    }

                }
            });
        }*/
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

        final MASUser currentUser = MASUser.getCurrentUser();
        if (currentUser != null && currentUser.isSessionLocked()) {
            MASUser.getCurrentUser().unlockSession(getUnlockCallback(this));
        }

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

                            if (currentUser.getUserName().toString().contains("google-")) {
                                String googleId = currentUser.getUserName().toString().substring(currentUser.getUserName().toString().indexOf("-") + 1);
                                String googleUserId = googleId.substring(googleId.indexOf("-") + 1);


                                setTitle("Authenticated [" + googleUserId + "]");
                                btnLogout.setVisibility(View.VISIBLE);
                                btnLogout.setText("Logout");
                            } else {
                                btnLogout.setVisibility(View.VISIBLE);
                                btnLogout.setText("Logout");
                                setTitle("Authenticated [" + currentUser.getUserName().toString() + "]");
                            }

                        } catch (Exception e) {
                            Log.d(TAG, e.getMessage());
                            e.printStackTrace();
                        }
                    }
                });

            } else if (currentUser.isSessionLocked()) {
                Log.d(TAG, "MAS User Session is currently locked");

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                    }
                });
            }
        }

        //refreshDialogStatus();
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
                    //refreshDialogStatus();
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

                            if (currentUser.getUserName().toString().contains("google-")) {
                                String googleId = currentUser.getUserName().toString().substring(currentUser.getUserName().toString().indexOf("-") + 1);
                                String googleUserId = googleId.substring(googleId.indexOf("-") + 1);


                                setTitle("Authenticated [" + googleUserId + "]");
                                btnLogout.setVisibility(View.VISIBLE);
                            } else {
                                btnLogout.setVisibility(View.VISIBLE);
                                setTitle("Authenticated [" + currentUser.getUserName().toString() + "]");
                            }

                        } catch (Exception e) {
                            Log.d(TAG, e.getMessage());
                            e.printStackTrace();
                        }
                    }
                });

            } else if (currentUser.isSessionLocked()) {
                Log.d(TAG, "MAS User Session is currently locked");

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                    }
                });
            }
        } else {    // An Authenticated User Session doesn't exist yet
            MAS.setAuthenticationListener(new MASAuthenticationListener() {
                @Override
                public void onAuthenticateRequest(Context context, long requestId, MASAuthenticationProviders providers) {
                    Intent intent = new Intent(context, MASLoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra(MssoIntents.EXTRA_REQUEST_ID, requestId);
                    intent.putExtra(MssoIntents.EXTRA_AUTH_PROVIDERS, providers);
                    context.startActivity(intent);
                }

                @Override
                public void onOtpAuthenticateRequest(Context context, MASOtpAuthenticationHandler handler) {
                    //Ignore for now
                }
            });

            MASUser.login(new MASCallback<MASUser>() {
                @Override
                public void onSuccess(MASUser result) {
                    Log.w("TUTORIAL", "Logged in as " + MASDevice.getCurrentDevice().getIdentifier());

                }

                @Override
                public void onError(Throwable e) {
                    Log.w("TUTORIAL", "Login failure: " + e);
                }
            });

            Log.d(TAG, "MAS User Session is not Authenticated");
            btnLogout.setVisibility(View.GONE);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setTitle("Please Authenticate");

                }
            });
        }
    }
}
