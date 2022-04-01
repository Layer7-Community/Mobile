package com.ca.mas.massample;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.ca.mas.core.error.TargetApiException;
import com.ca.mas.core.service.MssoIntents;
import com.ca.mas.foundation.MAS;
import com.ca.mas.foundation.MASAuthenticationListener;
import com.ca.mas.foundation.MASCallback;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MAS.start(this);

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

                getFlightList();

            }

            @Override
            public void onError(Throwable e) {
                Log.w("TUTORIAL", "Login failure: " + e);
            }
        });

    }

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
                    Log.w("TUTORIAL", "Getting Flight List, got result: " + result.getBody().getContent());
                    showMessage("Airline info: " + result.getBody().getContent(), Toast.LENGTH_LONG);
                }

                @Override
                public void onError(Throwable e) {
                    Log.w("TUTORIAL", "Getting Flight List, got error: " + e);
                    if (e.getCause() instanceof TargetApiException) {
                        showMessage(new String(((TargetApiException) e.getCause()).getResponse()
                                .getBody().getRawContent()), Toast.LENGTH_SHORT);
                    } else {
                        showMessage("Error: " + e.getMessage(), Toast.LENGTH_LONG);
                    }
                }
            });

        } catch (Exception x) {
            Log.w("TUTORIAL", "ERROR Getting Flight List");
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
