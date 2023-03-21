/*
 * Copyright (c) 2016 CA. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 */

package com.ca.masstoragesample;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.ca.mas.core.service.MssoIntents;
import com.ca.mas.foundation.MAS;
import com.ca.mas.foundation.MASAuthenticationListener;
import com.ca.mas.foundation.MASCallback;
import com.ca.mas.foundation.MASConstants;
import com.ca.mas.foundation.MASOtpAuthenticationHandler;
import com.ca.mas.foundation.auth.MASAuthenticationProviders;
import com.ca.mas.storage.MASSecureLocalStorage;
import com.ca.mas.storage.MASStorage;
import com.ca.mas.ui.MASLoginActivity;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private EditText title;
    private EditText content;
    private Button save;
    private Button open;

    public static boolean LOGIN_STATUS = false;
    public static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        LOGIN_STATUS = false;
        title = (EditText) findViewById(R.id.title);
        content = (EditText) findViewById(R.id.content);
        save = (Button) findViewById(R.id.saveButton);
        open = (Button) findViewById(R.id.open);

        checkAppPermissions();

        MAS.start(this);

        MAS.setAuthenticationListener(new MASAuthenticationListener() {
            @Override
            public void onAuthenticateRequest(Context context, long requestId, MASAuthenticationProviders providers) {
                LOGIN_STATUS = true;
                Intent loginIntent = new Intent(context, MASLoginActivity.class);
                loginIntent.putExtra(MssoIntents.EXTRA_AUTH_PROVIDERS, providers);
                loginIntent.putExtra(MssoIntents.EXTRA_REQUEST_ID, requestId);
                startActivity(loginIntent);
            }

            @Override
            public void onOtpAuthenticateRequest(Context context, MASOtpAuthenticationHandler handler) {
                //Ignore for now
            }
        });

        final MASStorage storage = new MASSecureLocalStorage();

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (BuildConfig.DEBUG) {
                    CountingIdlingResourceSingleton.increment();
                }
                storage.save(
                        title.getText().toString(),
                        content.getText().toString(),
                        MASConstants.MAS_USER | MASConstants.MAS_APPLICATION,
                        new MASCallback<Void>() {

                            @Override
                            public void onSuccess(Void result) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        content.setText("");
                                        if (BuildConfig.DEBUG) {
                                            CountingIdlingResourceSingleton.decrement();
                                        }
                                    }
                                });
                            }

                            @Override
                            public void onError(Throwable e) {
                                if (BuildConfig.DEBUG) {
                                    CountingIdlingResourceSingleton.decrement();
                                }
                            }
                        });
            }
        });


        open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (BuildConfig.DEBUG) {
                    CountingIdlingResourceSingleton.increment();
                }
                storage.findByKey(title.getText().toString(),
                        MASConstants.MAS_USER | MASConstants.MAS_APPLICATION,

                        new MASCallback() {
                            @Override
                            public Handler getHandler() {
                                return new Handler(Looper.getMainLooper());
                            }

                            @Override
                            public void onSuccess(Object result) {
                                content.setText((String) result);
                                if (BuildConfig.DEBUG) {
                                    CountingIdlingResourceSingleton.decrement();
                                }
                            }

                            @Override
                            public void onError(Throwable e) {
                                if (BuildConfig.DEBUG) {
                                    CountingIdlingResourceSingleton.decrement();
                                }
                            }
                        });
            }
        });


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
            }else if(Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU &&(
                    criticalPermission.equals(Manifest.permission.BLUETOOTH_SCAN) ||
                            criticalPermission.equals(Manifest.permission.BLUETOOTH_CONNECT))){
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
}
