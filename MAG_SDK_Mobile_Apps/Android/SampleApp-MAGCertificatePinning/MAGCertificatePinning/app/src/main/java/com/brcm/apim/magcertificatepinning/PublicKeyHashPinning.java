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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.ca.mas.core.cert.PublicKeyHash;
import com.ca.mas.core.error.TargetApiException;
import com.ca.mas.foundation.MAS;
import com.ca.mas.foundation.MASCallback;
import com.ca.mas.foundation.MASConfiguration;
import com.ca.mas.foundation.MASRequest;
import com.ca.mas.foundation.MASResponse;
import com.ca.mas.foundation.MASSecurityConfiguration;
import org.json.JSONObject;
import java.net.URL;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;

public class PublicKeyHashPinning extends AppCompatActivity {
    private final List<Uri> hosts = new ArrayList<>();
    Button mBtnConnectEndPt;
    TextView success_result;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_with_public_key_hash_pinning);
        mBtnConnectEndPt = findViewById(R.id.connect);
        success_result = findViewById(R.id.success_result);

        mBtnConnectEndPt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            verifyAllCertPinning();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });
    }

    public void verifyAllCertPinning() throws Exception {
        final Uri externalUri = buildAndGetExternalUri();
        final Uri externalHost = getExternalHost(externalUri);
        final URL securityUrl = new URL("https://www.googleapis.com:443");
        final Certificate[] mCert = CertificateUtils.getCertificates(securityUrl);
        MASSecurityConfiguration configuration = new MASSecurityConfiguration.Builder()
                .host(externalHost)
                .add(mCert[0])
                .add(mCert[1])
                .add(mCert[2])
                .add(PublicKeyHash.fromCertificate(mCert[0]).getHashString())
                .add(PublicKeyHash.fromCertificate(mCert[1]).getHashString())
                .add(PublicKeyHash.fromCertificate(mCert[2]).getHashString())
                .build();
        validateRequestWithSecurityConfiguration(externalUri, configuration);
    }

    Uri getExternalHost(Uri externalUri) {
        final Uri externalHost = new Uri.Builder().encodedAuthority(externalUri.getEncodedAuthority()).build();
        hosts.add(externalHost);
        return externalHost;
    }

    Uri buildAndGetExternalUri() {
        return new Uri.Builder()
                .scheme("https")
                .encodedAuthority("www.googleapis.com:443")
                .appendEncodedPath("books")
                .appendEncodedPath("v1")
                .appendEncodedPath("volumes")
                .appendQueryParameter("q", "patrick+rothfuss")
                .build();
    }

    void validateRequestWithSecurityConfiguration(Uri externalUri, MASSecurityConfiguration configuration) {
        MASConfiguration.getCurrentConfiguration().addSecurityConfiguration(configuration);

        MASRequest request = new MASRequest.MASRequestBuilder(externalUri)
                .get()
                .setPublic()
                .build();
        MAS.invoke(request, new MASCallback<MASResponse<JSONObject>>() {

            @Override
            public Handler getHandler() {
                return new Handler(Looper.getMainLooper());
            }

            @Override
            public void onSuccess(MASResponse<JSONObject> result) {
                Log.d("ESCALATION", "Getting Books List, got result : " + result.getBody().getContent());
                success_result.setText(getResources().getString(R.string.all_public_key_hashes));
                //showMessage("Request Successful: All Public key hashes are verified.", Toast.LENGTH_LONG);
            }

            @Override
            public void onError(Throwable e) {
                Log.d("ESCALATION", "Getting Books List, got error: " + e);
                if (e.getCause() instanceof TargetApiException) {
                    showMessage(new String(((TargetApiException) e.getCause()).getResponse()
                            .getBody().getRawContent()), Toast.LENGTH_SHORT);
                } else {
                    showMessage("Error: " + e.getMessage(), Toast.LENGTH_LONG);
                }
            }
        });

    }

    public void showMessage(final String message, final int toastLength) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(PublicKeyHashPinning.this, message, toastLength).show();
            }
        });
    }
}
