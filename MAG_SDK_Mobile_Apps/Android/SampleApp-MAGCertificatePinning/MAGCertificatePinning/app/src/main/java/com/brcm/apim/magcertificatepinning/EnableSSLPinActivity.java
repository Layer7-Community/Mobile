package com.brcm.apim.magcertificatepinning;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.brcm.apim.magcertificatepinning.model.Product;
import com.ca.mas.core.cert.PublicKeyHash;
import com.ca.mas.core.conf.ConfigurationManager;
import com.ca.mas.core.error.TargetApiException;
import com.ca.mas.foundation.MAS;
import com.ca.mas.foundation.MASCallback;
import com.ca.mas.foundation.MASConfiguration;
import com.ca.mas.foundation.MASRequest;
import com.ca.mas.foundation.MASResponse;
import com.ca.mas.foundation.MASSecurityConfiguration;
import com.ca.mas.foundation.MASSecurityPinningMode;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Iterator;

public class EnableSSLPinActivity extends AppCompatActivity implements View.OnClickListener, ProductListAdapter.ItemClickListener {
    RecyclerView recyclerView;
    ArrayList<Product> productArrayList;
    ProductListAdapter adapter = null;
    Button mBtnConnectEndPt = null;
    TextView mTvResult = null;
    TextView sslPinningStatus = null;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    Switch aSwitch;
    MASRequest request = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enable_sslpin);

        mBtnConnectEndPt = findViewById(R.id.enable_ssl_pin);
        mBtnConnectEndPt.setOnClickListener(this);
        mTvResult = findViewById(R.id.success_endpt_result);
        sslPinningStatus = findViewById(R.id.sslPinningStatus);

        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        aSwitch = findViewById(R.id.enableSslPinning);

        // layout for vertical orientation
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(linearLayoutManager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                linearLayoutManager.getOrientation());
        recyclerView.addItemDecoration(dividerItemDecoration);

        sslPinningStatus.setText("SSLPinningEnabled : " + MAS.isSSLPinningEnabled());

        aSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    recyclerView.setVisibility(View.INVISIBLE);
                    ConfigurationManager.getInstance().setSSLPinningEnabled(true);
                    sslPinningStatus.setText("SSLPinningEnabled : " + MAS.isSSLPinningEnabled());
                } else {
                    recyclerView.setVisibility(View.INVISIBLE);
                    ConfigurationManager.getInstance().setSSLPinningEnabled(false);
                    sslPinningStatus.setText("SSLPinningEnabled : " + MAS.isSSLPinningEnabled());
                }
            }
        });

    }

    @Override
    public void onClick(View v) {
        if (v == mBtnConnectEndPt) {
            recyclerView.setVisibility(View.VISIBLE);
            if (aSwitch.isChecked()) {
                final Certificate cert_B = ConfigurationManager.getInstance().getConnectedGatewayConfigurationProvider().getTrustedCertificateAnchors().iterator().next();
                String server_uri = ConfigurationManager.getInstance().getConnectedGateway().getHost();
                final String authority_A = server_uri.concat(":" +
                        Integer.toString(MASConfiguration.getCurrentConfiguration()
                                .getGatewayPort()));
                final Uri serverA_Uri = new Uri.Builder()
                        .scheme("https")
                        .encodedAuthority(authority_A)
                        .appendEncodedPath("retrieveFlights")
                        .build();
                Log.d("Testing", "uri is formed:: " + server_uri);
                try {
                    MASSecurityConfiguration configuration = new MASSecurityConfiguration.Builder()
                            .host(serverA_Uri)
                            .pinningMode(MASSecurityPinningMode.MAS_SECURITY_PINNING_MODE_INTERMEDIATE_CERTIFICATE)
                            .allowSSLPinning(MAS.isSSLPinningEnabled())
                            .add(PublicKeyHash.fromCertificate(cert_B).getHashString())
                            .build();
                    Log.d("Testing", "configuration object is created:: ");
                    MASConfiguration.getCurrentConfiguration().addSecurityConfiguration(configuration);
                    Log.d("Testing", "configuration object is added:: ");
                } catch (Exception e) {
                    Log.d("Testing", "Exception:: " + e.getMessage());
                }
                request = new MASRequest.MASRequestBuilder(serverA_Uri).build();
                Toast.makeText(EnableSSLPinActivity.this, "SSL Pinning is on", Toast.LENGTH_SHORT).show();
            } else {
                String server_uri = ConfigurationManager.getInstance().getConnectedGateway().getHost();
                final String authority_A = server_uri.concat(":" +
                        Integer.toString(MASConfiguration.getCurrentConfiguration()
                                .getGatewayPort()));
                final Uri serverA_Uri = new Uri.Builder()
                        .scheme("https")
                        .encodedAuthority(authority_A)
                        .appendEncodedPath("retrieveFlights")
                        .build();
                Log.d("Testing", "uri is formed:: " + server_uri);
                try {
                    MASSecurityConfiguration configuration = new MASSecurityConfiguration.Builder()
                            .host(serverA_Uri)
                            .pinningMode(MASSecurityPinningMode.MAS_SECURITY_PINNING_MODE_INTERMEDIATE_CERTIFICATE)
                            .allowSSLPinning(MAS.isSSLPinningEnabled())
                            .build();
                    Log.d("Testing", "configuration object is created:: ");
                    MASConfiguration.getCurrentConfiguration().addSecurityConfiguration(configuration);
                    Log.d("Testing", "configuration object is added:: ");
                } catch (Exception e) {
                    Log.d("Testing", "Exception:: " + e.getMessage());
                }
                request = new MASRequest.MASRequestBuilder(serverA_Uri).build();
                Toast.makeText(EnableSSLPinActivity.this, "SSL Pinning is off", Toast.LENGTH_SHORT).show();
            }
            getFlightList(request);
        }
    }

    public void getFlightList(MASRequest request) {
        try {

            MAS.invoke(request, new MASCallback<MASResponse<JSONObject>>() {

                @Override
                public Handler getHandler() {
                    return new Handler(Looper.getMainLooper());
                }

                @Override
                public void onSuccess(MASResponse<JSONObject> result) {
                    Log.d("ESCALATION", "Getting Flight List, got result: " + result.getBody().getContent());
                    try {
                        productArrayList = new ArrayList<>();
                        JSONObject json = new JSONObject(result.getBody().getContent().toString());
                        JSONArray jsonArray = json.getJSONArray("products");
                        for (int i = 0; i < jsonArray.length(); i++) {
                            Product product = new Product();
                            JSONObject object = jsonArray.getJSONObject(i);
                            Iterator<String> keys = object.keys();
                            product.setId(object.getInt(keys.next()));
                            product.setName(object.getString(keys.next()));
                            product.setArrivalTime(object.getString(keys.next()));
                            productArrayList.add(product);
                            adapter = new ProductListAdapter(getApplicationContext(), productArrayList);
                            adapter.setClickListener(EnableSSLPinActivity.this);
                            recyclerView.setAdapter(adapter);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onError(Throwable e) {
                    Log.d("ESCALATION", "Getting Flight List, got error: " + e);
                    if (e.getCause() instanceof TargetApiException) {
                        showMessage(new String(((TargetApiException) e.getCause()).getResponse()
                                .getBody().getRawContent()), Toast.LENGTH_SHORT);
                    } else {
                        showMessage("Error: " + e.getMessage(), Toast.LENGTH_LONG);
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
                Toast.makeText(EnableSSLPinActivity.this, message, toastLength).show();
            }
        });
    }

    @Override
    public void onItemClick(View view, int position) {

    }
}