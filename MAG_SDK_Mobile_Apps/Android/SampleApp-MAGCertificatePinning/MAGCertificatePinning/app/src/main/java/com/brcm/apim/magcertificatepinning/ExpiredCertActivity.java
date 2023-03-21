package com.brcm.apim.magcertificatepinning;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.ca.mas.core.error.TargetApiException;
import com.ca.mas.foundation.MAS;
import com.ca.mas.foundation.MASCallback;
import com.ca.mas.foundation.MASConstants;
import com.ca.mas.foundation.MASRequest;
import com.ca.mas.foundation.MASResponse;
import com.ca.mas.foundation.MASUser;
import org.json.JSONObject;
import java.io.IOException;
import java.net.URI;

public class ExpiredCertActivity extends AppCompatActivity implements View.OnClickListener {

    Button mBtnConnectEndPt = null;
    TextView mTvResult = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expired_cert);

        mBtnConnectEndPt = findViewById(R.id.connect_expired);
        mBtnConnectEndPt.setOnClickListener(this);
        mTvResult = findViewById(R.id.success_endpt_result);

        try {
            getAssets().open("msso_config_2.json");
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (MAS.getState(this) == MASConstants.MAS_STATE_STARTED) {
            MAS.stop();
        }

        MAS.setConfigurationFileName("msso_config_2.json");
        MAS.start(this, true);

        MASUser.login(new MASCallback<MASUser>() {
            @Override
            public void onSuccess(MASUser result) {
                Log.d("Testing", "Login in as: User Name:: " +MASUser.getCurrentUser().getUserName());
            }

            @Override
            public void onError(Throwable e) {
                Log.d("Testing", "Login failure: " + e);
            }
        });
    }

    @Override
    public void onClick(View v) {
        if(v == mBtnConnectEndPt){
            getFlightList();
        }
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
                    Log.d("ESCALATION", "Getting Flight List, got result: " + result.getBody().getContent());
                    showMessage("Airline info: " + result.getBody().getContent(), Toast.LENGTH_LONG);
                }

                @Override
                public void onError(Throwable e) {
                    Log.d("ESCALATION", "Getting Flight List, got error: " + e);
                    if (e.getCause() instanceof TargetApiException) {
                        showMessage(new String(((TargetApiException) e.getCause()).getResponse()
                                .getBody().getRawContent()), Toast.LENGTH_SHORT);
                    } else {
                        mTvResult.setText(e.getMessage());
                    }
                }
            });

        } catch (Exception x) {
            Log.d("ESCALATION", "ERROR Getting Flight List");
        }

    }

    public void showMessage(final String message, final int toastLength) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ExpiredCertActivity.this, message, toastLength).show();
            }
        });
    }
}