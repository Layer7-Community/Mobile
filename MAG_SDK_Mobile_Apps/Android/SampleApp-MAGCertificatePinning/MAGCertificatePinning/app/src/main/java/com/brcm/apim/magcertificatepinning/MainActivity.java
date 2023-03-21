package com.brcm.apim.magcertificatepinning;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.ca.mas.core.log.MASLoggerConfiguration;
import com.ca.mas.foundation.MAS;
import com.ca.mas.foundation.MASCallback;
import com.ca.mas.foundation.MASConfiguration;
import com.ca.mas.foundation.MASUser;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    Button mBtnStart = null;
    private static final int REQUEST_CODE_BLUETOOTH_SCAN = 1;

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBtnStart = findViewById(R.id.btn_start_app);
        mBtnStart.setOnClickListener(this);

        checkPermission();
    }

    private void checkPermission() {
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_CODE_BLUETOOTH_SCAN);
    }

    @Override
    public void onClick(View v) {

        MAS.start(this);
        MASLoggerConfiguration loggerConfiguration = MASConfiguration.getMASLoggerConfiguration();
        loggerConfiguration.setLogLevel(MASLoggerConfiguration.MASLogLevel.VERBOSE);

        MASUser.login(new MASCallback<MASUser>() {
            @Override
            public void onSuccess(MASUser result) {
                Log.d("ESCALATION", "Login in as: User Name:: " +MASUser.getCurrentUser().getUserName());
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, TestScenarioActivity.class);
                startActivity(intent);
            }

            @Override
            public void onError(Throwable e) {
                Log.d("ESCALATION", "Login failure: " + e);
            }
        });
    }


}