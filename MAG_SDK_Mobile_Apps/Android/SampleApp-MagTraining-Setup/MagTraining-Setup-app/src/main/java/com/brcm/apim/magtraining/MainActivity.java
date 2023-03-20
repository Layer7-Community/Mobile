package com.brcm.apim.magtraining;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.os.SystemClock;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.ca.mas.core.log.MASLoggerConfiguration;
import com.ca.mas.foundation.MAS;
import com.ca.mas.foundation.MASCallback;
import com.ca.mas.foundation.MASConfiguration;
import com.ca.mas.foundation.MASConstants;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {


    private String mEmulatorLocation = null;
    private static final String TAG = MainActivity.class.getSimpleName();

    TextView textViewApiStatus;
    ProgressBar progressBarApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textViewApiStatus = findViewById(R.id.textViewApiStatus);
        progressBarApp = findViewById(R.id.progressBarApp);

        //
        // Check the App Permissions based on the contents of the Manifest File
        //
        checkAppPermissions();
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
        // Start the MAS SDK
        //

        MAS.start(this);
        if (BuildConfig.DEBUG) {
            CountingIdlingResourceSingleton.increment();
        }

        int myMasState = MAS.getState(this);

        if (myMasState == MASConstants.MAS_STATE_STARTED)
            Log.d(TAG, "MAS SDK Successfully started");

        // Checking for connectivity
        MAS.gatewayIsReachable(new MASCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean aBoolean) {
                textViewApiStatus.setText(getString(R.string.server_reachable));
                if (BuildConfig.DEBUG) {
                    CountingIdlingResourceSingleton.decrement();
                }

                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        progressBarApp.setVisibility(View.GONE);
                    }
                });

                Log.d(TAG, "MAS Server is reachable!");
            }

            @Override
            public void onError(Throwable throwable) {
                textViewApiStatus.setText("MAS Server is not reachable!");

                if (BuildConfig.DEBUG) {
                    CountingIdlingResourceSingleton.decrement();
                }
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        progressBarApp.setVisibility(View.GONE);
                    }
                });
            }
        });


        MASConfiguration.getMASLoggerConfiguration().setLogLevel(MASLoggerConfiguration.MASLogLevel.VERBOSE);

    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String permissions[],
            int[] grantResults) {


        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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
            if(criticalPermission.equals(Manifest.permission.READ_LOGS)){
                return;
            }else if(Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU &&
                    criticalPermission.equals(Manifest.permission.BLUETOOTH_SCAN)){
                return;
            }

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
                                        SystemClock.sleep(1000);
                                        moveTaskToBack(true);
                                        Process.killProcess(Process.myPid());
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

}
