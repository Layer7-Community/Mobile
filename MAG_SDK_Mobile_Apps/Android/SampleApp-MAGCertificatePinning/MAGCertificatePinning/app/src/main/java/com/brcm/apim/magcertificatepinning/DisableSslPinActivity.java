package com.brcm.apim.magcertificatepinning;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.ca.mas.core.conf.ConfigurationManager;
import com.ca.mas.core.error.TargetApiException;
import com.ca.mas.foundation.MAS;
import com.ca.mas.foundation.MASCallback;
import com.ca.mas.foundation.MASConfiguration;
import com.ca.mas.foundation.MASRequest;
import com.ca.mas.foundation.MASResponse;
import com.ca.mas.foundation.MASSecurityConfiguration;
import com.ca.mas.foundation.MASSecurityPinningMode;
import org.json.JSONObject;

public class DisableSslPinActivity extends AppCompatActivity implements View.OnClickListener{

    Button mBtnConnectEndPt = null;
    TextView mTvResult = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_disable_ssl_pin);

        mBtnConnectEndPt = findViewById(R.id.disable_ssl_pin);
        mBtnConnectEndPt.setOnClickListener(this);
        mTvResult = findViewById(R.id.success_endpt_result);
    }

    @Override
    public void onClick(View v) {
        if(v == mBtnConnectEndPt){
            String server_uri = ConfigurationManager.getInstance().getConnectedGateway().getHost();
            final String authority_A = server_uri.concat(":" +
                    Integer.toString(MASConfiguration.getCurrentConfiguration()
                            .getGatewayPort()));
            final Uri serverA_Uri = new Uri.Builder()
                    .scheme("https")
                    .encodedAuthority(authority_A)
                    .appendEncodedPath("retrieveFlights")
                    .build();
            Log.d("Testing", "uri is formed:: "+server_uri);
            try {
                MASSecurityConfiguration configuration = new MASSecurityConfiguration.Builder()
                        .host(serverA_Uri)
                        .pinningMode(MASSecurityPinningMode.MAS_SECURITY_PINNING_MODE_INTERMEDIATE_CERTIFICATE)
                        .allowSSLPinning(false)
                        .build();
                Log.d("Testing", "configuration object is created:: ");
                MASConfiguration.getCurrentConfiguration().addSecurityConfiguration(configuration);
                Log.d("Testing", "configuration object is added:: ");
            }catch (Exception e) {
                Log.d("Testing", "Exception:: "+e.getMessage());
            }

            MASRequest request = new MASRequest.MASRequestBuilder(serverA_Uri).build();

            MAS.invoke(request, new MASCallback<MASResponse<JSONObject>>() {

                @Override
                public Handler getHandler() {
                    return new Handler(Looper.getMainLooper());
                }

                @Override
                public void onSuccess(MASResponse<JSONObject> result) {
                   // Log.d("Testing", "ssl pinning enabled " +MASConfiguration.getCurrentConfiguration().isSslPinningEnabled());
                    mTvResult.setText(result.getBody().getContent().toString());
                }

                @Override
                public void onError(Throwable e) {
                    Log.d("Testing", "Getting Flight List, got error: " + e);
                    if (e.getCause() instanceof TargetApiException) {
                        showMessage(new String(((TargetApiException) e.getCause()).getResponse()
                                .getBody().getRawContent()), Toast.LENGTH_SHORT);
                    } else {
                        showMessage("Error: " + e.getMessage(), Toast.LENGTH_LONG);
                    }
                }
            });
        }
    }

    public void showMessage(final String message, final int toastLength) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(DisableSslPinActivity.this, message, toastLength).show();
            }
        });
    }
}