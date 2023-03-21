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

import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class WrongHostname extends AppCompatActivity implements View.OnClickListener{

    Button mBtnConnectEndPt = null;
    X509Certificate caCert = null;
    String called_hostname = null;
    TextView mTvResult = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wrong_hostname);

        mBtnConnectEndPt = findViewById(R.id.connect_wrong_hostname);
        mBtnConnectEndPt.setOnClickListener(this);
        mTvResult = findViewById(R.id.success_endpt_result);
    }

    @Override
    public void onClick(View v) {
        if(v == mBtnConnectEndPt){
            wrongHostnameVerify();
        }
    }

    private void wrongHostnameVerify() {

        try {
            InputStream resourceStream = getResources().openRawResource(R.raw.server);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            caCert = (X509Certificate)cf.generateCertificate(resourceStream);
            called_hostname = getFirstCn(caCert);
        } catch (CertificateException e) {
            e.printStackTrace();
        }

        Log.d("Testing", "certificate:: "+caCert);
        Log.d("Testing", "certificate DN:: "+called_hostname);


        final String authority_A = called_hostname.concat(":" +
                Integer.toString(MASConfiguration.getCurrentConfiguration()
                        .getGatewayPort()));

        final Uri serverA_Uri = new Uri.Builder()
                .scheme("https")
                .encodedAuthority(authority_A)
                .appendQueryParameter("operation", "listProducts")
                .build();

        final Certificate cert_B = ConfigurationManager.getInstance().getConnectedGatewayConfigurationProvider().getTrustedCertificateAnchors().iterator().next();

        Log.d("Testing", "uri is formed:: "+serverA_Uri.toString());
        try {
            MASSecurityConfiguration configuration = new MASSecurityConfiguration.Builder()
                    .host(serverA_Uri)
                    .pinningMode(MASSecurityPinningMode.MAS_SECURITY_PINNING_MODE_INTERMEDIATE_CERTIFICATE)
                    .enableHostnameVerifier(true)
                    .allowSSLPinning(false)
                    .build();
            Log.d("Testing", "configuration object is created:: ");
            MASConfiguration.getCurrentConfiguration().addSecurityConfiguration(configuration);
            Log.d("Testing", "configuration object is added:: ");
        }catch (Exception e) {
            Log.d("Testing", "Exception:: "+e.getMessage());
        }


        MASRequest request = new MASRequest.MASRequestBuilder(serverA_Uri).build();
        Log.d("Testing", "MASrequest object is created:: ");
        MAS.invoke(request, new MASCallback<MASResponse<JSONObject>>() {
            @Override
            public Handler getHandler() {
                return new Handler(Looper.getMainLooper());
            }

            @Override
            public void onSuccess(MASResponse<JSONObject> result) {
                Log.d("Testing", "Getting Flight List, got result: " + result.getBody().getContent());
                showMessage("Airline info: " + result.getBody().getContent(), Toast.LENGTH_LONG);
            }

            @Override
            public void onError(Throwable e) {
                Log.d("Testing", "Getting Flight List, got error: " + e);
                if (e.getCause() instanceof TargetApiException) {
                    showMessage(new String(((TargetApiException) e.getCause()).getResponse()
                            .getBody().getRawContent()), Toast.LENGTH_SHORT);
                } else {
                    mTvResult.setText(e.getMessage());
                }
            }
        });
    }

    public void showMessage(final String message, final int toastLength) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(WrongHostname.this, message, toastLength).show();
            }
        });
    }

    private String getFirstCn(X509Certificate cert) {
        String subjectPrincipal = cert.getSubjectX500Principal().toString();
        for (String token : subjectPrincipal.split(",")) {
            int x = token.indexOf("CN=");
            if (x >= 0) {
                return token.substring(x + 3);
            }
        }
        return null;
    }
}