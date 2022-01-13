package com.brcm.apim.magtraining;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.ca.mas.foundation.MAS;
import com.ca.mas.foundation.MASFileObject;

import com.ca.mas.foundation.MASCallback;
import com.ca.mas.foundation.MASConstants;
import com.ca.mas.foundation.MASSessionUnlockCallback;
import com.ca.mas.foundation.MASUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
//import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.ca.mas.core.error.MAGError;
import com.ca.mas.core.error.MAGRuntimeException;
import com.ca.mas.foundation.MAS;
import com.ca.mas.foundation.MASCallback;
import com.ca.mas.foundation.MASConnectionListener;
import com.ca.mas.foundation.MASException;
import com.ca.mas.foundation.MASFileObject;
import com.ca.mas.foundation.MASProgressListener;
import com.ca.mas.foundation.MASRequest;
import com.ca.mas.foundation.MASResponse;
import com.ca.mas.foundation.MultiPart;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int READ_REQUEST_CODE = 42;
    EditText etKey, etValue;
    TextView tvFilePath;
    Button btnAddFormParam;
    MASFileObject filePart = new MASFileObject();
    MultiPart multiPart = new MultiPart();
    Uri uri;
    private String mEmulatorLocation = null;
    private Button mLoginButton = null;
    private Button mLogoutButton = null;
    private TextView mUserAuthenticatedStatus = null;

    String selectedFilePath;
    ProgressDialog progressDialog;
    Context context;
    String fileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnAddFormParam = (Button) findViewById(R.id.btn_addformparam);
        etKey = (EditText) findViewById(R.id.etxt_key);
        etValue = (EditText) findViewById(R.id.etxt_value);
        tvFilePath = (TextView) findViewById(R.id.tv_filePath);
        mLoginButton = findViewById(R.id.loginButton);
        mLogoutButton = findViewById(R.id.logoutButton);
        mUserAuthenticatedStatus = findViewById(R.id.userStatusTextView);
        this.context = this;
        mLogoutButton.setVisibility(View.INVISIBLE);

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
        // Check the App Permissions based on the contents of the Manifest File
        //

        checkAppPermissions();

        //
        // Start the MAS SDK
        //

        MAS.start(this);

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

        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Log.d(TAG, "Login Button has been clicked");
                MASUser.login("spock","StRonG5^)".toCharArray(), new MASCallback<MASUser>() {

                    //  MASUser.login(new MASCallback<MASUser>() {
                    @Override
                    public void onSuccess(MASUser masUser) {
                        Log.d(TAG, "User was successfully authenticated");
                        refreshDialogStatus();
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.d(TAG, "User was failed to authenticate successfully");
                    }
                });
            }
        });

        mLogoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Logout Button has been clicked");

                if (MASUser.getCurrentUser() != null) {

                    final String lAuthenticatedUserName = MASUser.getCurrentUser().getUserName();

                    MASUser.getCurrentUser().logout(false, new MASCallback<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "User " + lAuthenticatedUserName + " has been logged out");
                            refreshDialogStatus();
                        }

                        @Override
                        public void onError(Throwable throwable) {
                        }
                    });
                }
            }
        });
    }

    public void addFormParameter(View view) {
        String key = etKey.getText().toString();
        String value = etValue.getText().toString();
        if (key.length() == 0 || value.length() == 0) {
            showToast("Please enter Key, Value");
            return;
        }
        multiPart.addFormField(etKey.getText().toString(), etValue.getText().toString());
        etKey.setText("");
        etValue.setText("");
    }

    public void addFormPart(View view) {

    }

    public void pickFile(View view) {

        performFileSearch();
    }

    public void addFile(View view) throws InterruptedException {
        StringBuilder selectedFiles = new StringBuilder();

        for(MASFileObject fileObject: multiPart.getFilePart()){
            selectedFiles.append(fileObject.getFileName()+"\n");
        }

        Toast.makeText(context, "Files Added", Toast.LENGTH_LONG).show();

        if (uri == null || selectedFilePath == null) {
            showToast("Please Select a File to upload");
            return;
        }

    }

    /**
     * Fires an intent to spin up the "file chooser" UI and select an image.
     */
    public void performFileSearch() {

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        intent.addCategory(Intent.CATEGORY_OPENABLE);

        intent.setType("*/*");

        startActivityForResult(intent, READ_REQUEST_CODE);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        super.onActivityResult(requestCode, resultCode, resultData);
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {

            if (resultData != null) {
                uri = resultData.getData();
                try {
                    MASFileObject filePart1 = new MASFileObject();
                    fileName = getDisplayName(uri);
                    selectedFilePath = fileName;
                    tvFilePath.setText(tvFilePath.getText().toString() + " " + fileName + "\n");

                    //filePart1.setFileUri(uri);
                    filePart1.setFieldName(fileName);
                    // filePart1.setFilePath("/sdcard/1mbmb.png");
                    filePart1.setFileBytes(getBytesFromUri(uri));
                    filePart1.setFileName(fileName);
                    filePart1.setFileType(getContentResolver().getType(uri));
                    multiPart.addFilePart(filePart1);

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (MASException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Returns the file content as byte array.
     *
     * @param uri the file uri.
     * @return file content as byte array
     */
    private static byte[] getBytesFromUri(Uri uri) throws MASException, IOException {

        InputStream inputStream = null;
        byte[] bytes;
        try {
            inputStream = MAS.getContext().getContentResolver().openInputStream(uri);
            bytes = getBytes(inputStream);
        } catch (IOException e) {
            throw new MASException(e);
        } finally {
            if (inputStream != null)
                inputStream.close();
        }
        return bytes;
    }
    /**
     * Returns the file content as byte array.
     *
     * @param path the absolute path of file
     * @return file content as byte array
     */
    private static byte[] getBytesFromPath(String path) throws MASException, IOException {
        byte[] bytes = {};
        InputStream is = null;
        try {
            File file = new File(path);
            bytes = new byte[(int) file.length()];
            is = new FileInputStream(file);
            is.read(bytes);
        } catch (IOException e) {
            throw new MASException(e);
        } finally {
            if (is != null)
                is.close();
        }
        return bytes;
    }

    private static byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }


    public void upload(View view){

        progressDialog = new ProgressDialog(MAS.getCurrentActivity());
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setProgress(0);
        progressDialog.show();
        try {
            final MASProgressListener progressListener = new MASProgressListener() {
                @Override
                public void onProgress(String progressPercent) {
                    Log.d(TAG, progressPercent);
                    progressDialog.setProgress(Integer.valueOf(progressPercent));
                }

                @Override
                public void onComplete() {
                    //dismissProgress();
                }

                @Override
                public void onError(MAGError error) {
                    dismissProgress();

                }
            };

            final MASRequest request = new MASRequest.MASRequestBuilder(new URI("/test/multipart/"))
                    .build();
            MAS.postMultiPartForm(request, multiPart, progressListener, new MASCallback<MASResponse>() {
                @Override
                public void onSuccess(MASResponse result) {
                    clear();
                    showToast("Upload Successful");
                    Log.d(TAG, "Upload Response: "+new String(result.getBody().getRawContent()));
                }

                @Override
                public void onError(Throwable e) {
                    clear();
                    showToast("ERROR: " + e.getLocalizedMessage());
                    Log.d(TAG, "Upload Response: "+ e.getMessage());


                }
            });

        } catch (MAGRuntimeException | MASException | URISyntaxException e) {
            dismissProgress();
            showToast("ERROR: " + e.getLocalizedMessage());
            Log.d(TAG, "Upload Response: "+ e.getMessage());
        }  finally {
            multiPart.reset();
        }


    }

    private void clear() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dismissProgress();
                etKey.setText("");
                etValue.setText("");
                tvFilePath.setText("");
                selectedFilePath = "";
                uri = null;
                multiPart.reset();
            }
        });
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

                    mLoginButton.setVisibility(View.VISIBLE);

                    mLoginButton.setEnabled(true);
                    setTitle("Please Authenticate");
                    mUserAuthenticatedStatus.setText("Not Authenticated");
                }
            });
        }
    }

    private void showToast(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast toast = Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }
        });
    }

    private void dismissProgress() {
        if (progressDialog != null && progressDialog.isShowing())
            progressDialog.dismiss();
    }


    public String getDisplayName(Uri uri) {
        String displayName = null;

        Cursor cursor = context.getContentResolver()
                .query(uri, null, null, null, null, null);

        try {
            if (cursor != null && cursor.moveToFirst()) {
                displayName = cursor.getString(
                        cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
            }
        } finally {
            cursor.close();
        }
        return displayName;
    }


}

