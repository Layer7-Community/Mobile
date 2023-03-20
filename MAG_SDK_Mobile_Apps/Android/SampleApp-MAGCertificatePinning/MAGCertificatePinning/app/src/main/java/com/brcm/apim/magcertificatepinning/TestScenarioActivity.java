package com.brcm.apim.magcertificatepinning;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Arrays;

public class TestScenarioActivity extends AppCompatActivity implements TestListAdapter.ItemClickListener{

    RecyclerView recyclerView;
    ArrayList<String> testList;
    TestListAdapter adapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_scenario);

        // Getting reference of recyclerView
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        String[] arrTestList = getResources().getStringArray(R.array.test_list);
        testList = new ArrayList<>(Arrays.asList(arrTestList));

        // Setting the layout as linear
        // layout for vertical orientation
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(linearLayoutManager);

        // Sending reference and data to Adapter
        adapter = new TestListAdapter(TestScenarioActivity.this, testList);

        adapter.setClickListener(this);
        // Setting Adapter to RecyclerView
        recyclerView.setAdapter(adapter);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                linearLayoutManager.getOrientation());
        recyclerView.addItemDecoration(dividerItemDecoration);
    }

    @Override
    public void onItemClick(View view, int position) {
        Log.d("Testing", "You clicked " + adapter.getItem(position) + " on row number " + position);
        Intent intent = new Intent();
        switch (position){
            case 0:
                intent.setClass(this, SuccessEndpoint.class);
                break;

            case 1:
                intent.setClass(this, EnableSSLPinActivity.class);
                break;

            /*case 2:
                intent.setClass(this, DisableSslPinActivity.class);
                break;*/

            case 2:
                intent.setClass(this, WrongHostname.class);
                break;
            case 3:
                intent.setClass(this, ExpiredCertActivity.class);
                break;

            case 4:
                intent.setClass(this, IntermediatePinningCert.class);
                break;

            case 5:
                intent.setClass(this, AllCertPinningCert.class);
                break;

            case 6:
                intent.setClass(this, PublicKeyHashPinning.class);
                break;

        }
        startActivity(intent);

    }
}