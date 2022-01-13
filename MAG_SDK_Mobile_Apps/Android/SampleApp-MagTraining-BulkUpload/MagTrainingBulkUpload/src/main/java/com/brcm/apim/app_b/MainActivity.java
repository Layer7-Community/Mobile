package com.brcm.apim.app_b;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.ca.mas.core.error.MAGError;
import com.ca.mas.foundation.MAS;
import com.ca.mas.foundation.MASCallback;
import com.ca.mas.foundation.MASConstants;
import com.ca.mas.foundation.MASFileObject;
import com.ca.mas.foundation.MASProgressListener;
import com.ca.mas.foundation.MASRequest;
import com.ca.mas.foundation.MASResponse;
import com.ca.mas.foundation.MASSessionUnlockCallback;
import com.ca.mas.foundation.MASUser;
import com.ca.mas.foundation.MultiPart;
import com.ca.mas.foundation.auth.MASProximityLoginBLEPeripheralListener;
import com.ca.mas.foundation.auth.MASProximityLoginQRCode;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {


    private String mEmulatorLocation=null;
    private Button mLoginButton=null;
    private Button mLogoutButton=null;
    private TextView mUserAuthenticatedStatus=null;
    private ProgressBar mProgressBar=null;
    private ProgressBar mFileUploadProgressBar=null;
    private Button mFileUpLoadButton=null;

    private String mCurrentPhotoPath;

    protected MASProximityLoginBLEPeripheralListener mBLEPeripheralListener;

    static final int REQUEST_IMAGE_CAPTURE = 1;

    private static final String TAG = MainActivity.class.getSimpleName();

    //
    // Background and Foreground Processing
    //
    private Timer mActivityTransitionTimer;
    private TimerTask mActivityTransitionTimerTask;
    private final long MAX_ACTIVITY_TRANSITION_TIME_MS = 600000;



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
        mFileUploadProgressBar = findViewById( R.id.hProgressBar );
        mUserAuthenticatedStatus = findViewById( R.id.userStatusTextView );

        mProgressBar.setVisibility( View.INVISIBLE );
        mFileUploadProgressBar.setVisibility( View.GONE );

        mFileUpLoadButton = findViewById( R.id.fileUpLoadButton );

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

        MAS.start(this);

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

                // mJsonResponseTextView.setText( "" );

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

                    MASUser.getCurrentUser().logout( false, new MASCallback<Void>() {
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
        mFileUpLoadButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectImage();
            }
        } );
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
                Log.d( "State", e.toString() );
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
        // Are we processing a camera capture event
        //
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (resultCode == RESULT_OK)
            {
                Log.d( "STATE", "Processing the photo image returned" );

                //
                // Here we do the multipart transfer of the photo using the store captured image
                //

                MASFileObject filePart = new MASFileObject();
                MultiPart multiPart = new MultiPart();

                filePart.setFieldName("file1");

                //
                // Get the file name from the full pathand extract the file bytes for actual upload
                //

                if (data == null) {
                    filePart.setFileName( mCurrentPhotoPath.substring( mCurrentPhotoPath.lastIndexOf( "/" ) + 1 ) );

                    try {
                        String extension = MimeTypeMap.getFileExtensionFromUrl(mCurrentPhotoPath);
                        final String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);

                        //
                        // Set the Mime Type for upload
                        //

                        filePart.setFileType(mimeType);
                        filePart.setFileBytes( getFileBytes( mCurrentPhotoPath ) );

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                } else {
                    Uri selectedImage =  data.getData();
                    String[] filePathColumn = {MediaStore.Images.Media.DATA};
                    if (selectedImage != null) {
                        Cursor cursor = getContentResolver().query( selectedImage,
                                filePathColumn, null, null, null );
                        if (cursor != null) {
                            cursor.moveToFirst();

                            int columnIndex = cursor.getColumnIndex( filePathColumn[0] );
                            String picturePath = cursor.getString( columnIndex );
                            Log.d(TAG, "Select file path is" + picturePath);
                            try {
                                filePart.setFileName( picturePath.substring( picturePath.lastIndexOf( "/" ) + 1 ) );

                                //
                                // Set the Mime Type for upload
                                //
                                String extension = MimeTypeMap.getFileExtensionFromUrl(picturePath);
                                final String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                                filePart.setFileType(mimeType);
                                filePart.setFileBytes( getFileBytes( picturePath ) );
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    // Got a selected image from the Android device library instead
                }
                multiPart.addFilePart(filePart);

                Uri uri = new Uri.Builder().path("/test/multipart/").build();

                //Create object of class implementing MASProgressListener
                MASProgressListener progressListener = new MASProgressListener() {
                    @Override
                    public void onProgress(String progressPercent) {
                        Log.d( TAG, "Percentage complate is: [" + progressPercent + "]" );
                        final int intProgressPercentage = Integer.parseInt( progressPercent.toString());

                        runOnUiThread( new Runnable() {
                            @Override
                            public void run() {
                                if (intProgressPercentage == 1)
                                    mFileUploadProgressBar.setVisibility( View.VISIBLE );
                                mFileUploadProgressBar.setProgress( intProgressPercentage );
                            }
                        });

                    }

                    @Override
                    public void onComplete() {
                        //Upload complete
                        Log.d(TAG, "Upload complete");

                        runOnUiThread( new Runnable() {
                            @Override
                            public void run() {
                                mFileUploadProgressBar.setVisibility( View.GONE );
                            }
                        });
                    }

                    @Override
                    public void onError(MAGError error) {
                        //Upload error
                    }
                };

                try {
                    final MASRequest request = new MASRequest.MASRequestBuilder(uri).build();

                    mProgressBar.setVisibility( View.VISIBLE );
                    MAS.postMultiPartForm( request, multiPart, progressListener, new MASCallback<MASResponse>() {

                        @Override
                        public void onSuccess(MASResponse result) {
                            Log.d(TAG, "Upload of the capture photo succeeded");
                            runOnUiThread( new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        mProgressBar.setVisibility( View.GONE );
                                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
                                        alertDialog.setTitle("SUCCESS");
                                        alertDialog.setMessage("Captured Image was successfully Uploaded");
                                        alertDialog.setPositiveButton("OK",
                                                new DialogInterface.OnClickListener() {

                                                    @Override
                                                    public void onClick(DialogInterface arg0, int arg1) {
                                                    }
                                                });
                                        alertDialog.show();
                                    }
                                    catch (Exception e) {
                                        Log.d(TAG,e.getMessage().toString());
                                        e.printStackTrace();
                                    }
                                }
                            } );
                        }

                        @Override
                        public void onError(Throwable e) {
                            Log.d(TAG, "Upload of the capture photo failed");
                            runOnUiThread( new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        mProgressBar.setVisibility( View.GONE );
                                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
                                        alertDialog.setTitle("ALERT");
                                        alertDialog.setMessage("Captured Image failed to Uploaded");
                                        alertDialog.setPositiveButton("OK",
                                                new DialogInterface.OnClickListener() {

                                                    @Override
                                                    public void onClick(DialogInterface arg0, int arg1) {
                                                    }
                                                });
                                        alertDialog.show();
                                    }
                                    catch (Exception e) {
                                        Log.d("STATE",e.getMessage().toString());
                                        e.printStackTrace();
                                    }
                                }
                            } );
                        }
                    } );
                } catch ( com.ca.mas.foundation.MASException e) {
                    Log.d(TAG, "Upload of the capture photo failed with error: " + e.getMessage());
                }
            }
        }

        //
        // Got the QR Code, perform the remote login request.
        //

        if (requestCode == REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                // The session was successfully unlocked
                MASUser.getCurrentUser().unlockSession(getUnlockCallback(this));
                Log.d(TAG, "The User Session was successfully unlocked");
            } else if (resultCode == RESULT_CANCELED) {
                // The Android lockscreen Activity was cancelled
                Log.d(TAG, "The User Unlock process was cancelled");
                refreshDialogStatus();
            }
        }
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
                            mLogoutButton.setEnabled( true );
                            mFileUpLoadButton.setEnabled( true );

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
                            mFileUpLoadButton.setVisibility( View.VISIBLE );

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
                        mUserAuthenticatedStatus.setText( "User Session is Locked!" );
                        mFileUpLoadButton.setEnabled( false );
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
                    mFileUpLoadButton.setVisibility (View.INVISIBLE);
                    mLoginButton.setEnabled( true );
                    setTitle( "Please Authenticate" );
                    mUserAuthenticatedStatus.setText( "Not Authenticated" );
                }
            } );
        }
    }

    private void selectImage() {
        final CharSequence[] options = { "Take Photo", "Choose from Gallery","Cancel" };
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Add Photo!");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (options[item].equals("Take Photo"))
                {

                    Intent intent = new Intent( MediaStore.ACTION_IMAGE_CAPTURE);
                    if (intent.resolveActivity(getPackageManager()) != null) {


                        // Create the File where the photo should go
                        File photoFile = null;
                        try {
                            photoFile = createImageFile();

                        } catch (IOException ex) {
                            // Error occurred while creating the File
                        }

                        // Continue only if the File was successfully created
                        if (photoFile != null) {
                            Uri photoURI = FileProvider.getUriForFile( MainActivity.this,
                                    "com.brcm.apim.magtraining",
                                    photoFile );
                            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
                        }
                    }
                }
                else if (options[item].equals("Choose from Gallery"))
                {
                    Intent intent = new  Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
                }
                else if (options[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }


    private byte[] getFileBytes(String pFileName) throws IOException {

        File lFile = new File(pFileName);
        int lFileSize = (int) lFile.length();
        byte[] lBytes = new byte[lFileSize];

        try {
            BufferedInputStream buf = new BufferedInputStream( new FileInputStream( lFile ) );
            buf.read(lBytes, 0, lBytes.length);
            buf.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lBytes;
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir( Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
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

