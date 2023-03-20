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
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.brcm.apim.magcertificatepinning.model.Product;
import com.ca.mas.core.error.TargetApiException;
import com.ca.mas.foundation.MAS;
import com.ca.mas.foundation.MASCallback;
import com.ca.mas.foundation.MASRequest;
import com.ca.mas.foundation.MASResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class SuccessEndpoint extends AppCompatActivity implements View.OnClickListener,ProductListAdapter.ItemClickListener {

    Button mBtnConnectEndPt = null;
    TextView mTvResult = null;
    RecyclerView recyclerView;
    ArrayList<Product> productArrayList;
    ProductListAdapter adapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_success_endpoint);
        mBtnConnectEndPt = findViewById(R.id.connect_success);
        mBtnConnectEndPt.setOnClickListener(this);
        mTvResult = findViewById(R.id.success_endpt_result);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);

        // layout for vertical orientation
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(linearLayoutManager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                linearLayoutManager.getOrientation());
        recyclerView.addItemDecoration(dividerItemDecoration);

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
                            adapter.setClickListener(SuccessEndpoint.this);
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
                Toast.makeText(SuccessEndpoint.this, message, toastLength).show();
            }
        });
    }

    @Override
    public void onItemClick(View view, int position) {

    }
}