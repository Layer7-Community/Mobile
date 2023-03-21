package com.ca.mas.massample;

import android.Manifest;
import android.content.Context;
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
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.ca.mas.core.error.TargetApiException;
import com.ca.mas.core.service.MssoIntents;
import com.ca.mas.foundation.MAS;
import com.ca.mas.foundation.MASAuthenticationListener;
import com.ca.mas.foundation.MASCallback;
import com.ca.mas.foundation.MASConstants;
import com.ca.mas.foundation.MASDevice;
import com.ca.mas.foundation.MASOtpAuthenticationHandler;
import com.ca.mas.foundation.MASRequest;
import com.ca.mas.foundation.MASResponse;
import com.ca.mas.foundation.MASUser;
import com.ca.mas.foundation.auth.MASAuthenticationProviders;
import com.ca.mas.ui.MASLoginActivity;

import org.json.JSONObject;

import java.net.URI;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_CODE_BLUETOOTH_SCAN = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//      Optional: To enable Debug and Verbose logs
//        MAS.debug();
//        MAS.start(this);


//      Optional: use the json configuration file name
//      which contains exported json data from oAuth manager
//        MAS.setConfigurationFileName("config.json");

//        useGrantFlowToAccessProtectedEndpoint();
        usePasswordFlowToAccessProtectedEndpoint();
    }

    /**
     * Uses Client Credentials flow for accessing the protected endpoint from server
     * 1. Set the Grant flow to client credentials
     * 2. Starts the SDK
     * 3. Calls a protected endpoint
     */
    private void useGrantFlowToAccessProtectedEndpoint() {
        MAS.setGrantFlow(MASConstants.MAS_GRANT_FLOW_CLIENT_CREDENTIALS);

        MAS.start(this);
        showMessage("MAS is started", Toast.LENGTH_LONG);

        getFlightList();
    }

    /**
     * Uses Password flow for accessing the protected endpoint from server
     * 1. Starts the SDK
     * 2. Calls a protected endpoint
     * <p>
     * Note: Default flow is Password Flow
     */
    private void usePasswordFlowToAccessProtectedEndpoint() {

        MAS.start(this);

//        useExplicitLogin("admin", "7layer".toCharArray());
        useImplicitLogin();
    }

    private void useImplicitLogin() {

        //From Android 13 BLUETOOTH_SCAN permission is required for NFC login.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_CODE_BLUETOOTH_SCAN);
        } else {
            loginUsingMASUI();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_BLUETOOTH_SCAN &&
                grantResults.length > 0 && // If request is cancelled, the result arrays are empty.
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loginUsingMASUI();
        }
    }

    private void loginUsingMASUI() {
        MAS.setAuthenticationListener(new MASAuthenticationListener() {
            @Override
            public void onAuthenticateRequest(Context context, long requestId, MASAuthenticationProviders providers) {
                try {
                    Intent intent = new Intent(context, MASLoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra(MssoIntents.EXTRA_REQUEST_ID, requestId);
                    intent.putExtra(MssoIntents.EXTRA_AUTH_PROVIDERS, providers);
                    context.startActivity(intent);
                } catch (Exception e) {
                    Log.i(TAG, "Exception while logging in using the Implicit Password flow, message: " + e.getMessage());
                }
            }

            @Override
            public void onOtpAuthenticateRequest(Context context, MASOtpAuthenticationHandler handler) {
                //Ignore for now
            }
        });

        MASUser.login(new MASCallback<MASUser>() {
            @Override
            public void onSuccess(MASUser result) {
                Log.w(TAG, "Logged in as " + MASDevice.getCurrentDevice().getIdentifier());
                getFlightList();
            }

            @Override
            public void onError(Throwable e) {
                Log.w(TAG, "Login failure: " + e);
            }
        });
    }

    /**
     * Uses explicit login, need to pass the username and password directly to MASUser.Login
     * On Successful login the app call the protected endpoint
     */
    private void useExplicitLogin(String username, char[] password) {

        MASUser.login(username, password, new MASCallback<MASUser>() {
            @Override
            public void onSuccess(MASUser result) {
                Log.w(TAG, "Logged in as " + MASDevice.getCurrentDevice().getIdentifier());
                getFlightList();
            }

            @Override
            public void onError(Throwable e) {
                Log.w(TAG, "Login failure: " + e);
            }
        });
    }

    /**
     * Makes a call to the protected endpoint
     * Invokes a endpoint and displays the response
     */
    public void getFlightList() {
        try {
            URI flightList = new URI("/retrieveFlights");
            final MASRequest request = new MASRequest.MASRequestBuilder(flightList).build();

            MAS.invoke(request, new MASCallback<MASResponse<JSONObject>>() {
                @Override
                public Handler getHandler() {
                    return new Handler(Looper.getMainLooper());
                }

                @Override
                public void onSuccess(MASResponse<JSONObject> result) {
                    String res = new String(result.getBody().getRawContent());
                    Log.w(TAG, "Getting Flight List, got result: " + res);
                    showMessage("Airline info: " + res, Toast.LENGTH_LONG);
                }

                @Override
                public void onError(Throwable e) {
                    Log.w(TAG, "Getting Flight List, got error: " + e);
                    if (e.getCause() instanceof TargetApiException) {
                        showMessage(new String(((TargetApiException) e.getCause()).getResponse()
                                .getBody().getRawContent()), Toast.LENGTH_SHORT);
                    } else {
                        showMessage("Error: " + e.getMessage(), Toast.LENGTH_LONG);
                    }
                }
            });
        } catch (Exception x) {
            Log.w(TAG, "ERROR Getting Flight List");
        }
    }

    @Override
    protected void onDestroy() {

        //Comment logout if you want to use single sign on
        logout();
        super.onDestroy();
    }

    /**
     * For logging out the user
     * SDK uses Single Sign On, if you need to login everytime the app is opened call this logout
     * before closing the app
     */
    private void logout() {
        MASUser loggedUser = MASUser.getCurrentUser();

        if (loggedUser != null) {
            loggedUser.logout(true, new MASCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    showMessage("logged out", Toast.LENGTH_SHORT);
                }

                @Override
                public void onError(Throwable e) {
                    showMessage("logged out failed", Toast.LENGTH_SHORT);
                }
            });
        }
    }

    public void showMessage(final String message, final int toastLength) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, message, toastLength).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
