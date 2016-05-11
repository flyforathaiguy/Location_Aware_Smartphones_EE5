package com.example.robin.pattern_search;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import android.view.View;
import android.widget.Button;

import be.groept.emedialab.util.GlobalResources;


public class StartActivity extends Activity {
    private View mContentView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        GlobalResources.getInstance().getDevice().setMac(BluetoothAdapter.getDefaultAdapter().getAddress());
        mContentView = findViewById(R.id.Pattern_Search);

        Button createButton = (Button) findViewById(R.id.join);
        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), ConnectionActivity.class);
                Bundle bundle = new Bundle();
                bundle.putBoolean("JOIN", true);
                intent.putExtras(bundle);
                StartActivity.this.startActivity(intent);

            }
        });
        Button joinButton = (Button) findViewById(R.id.start_new);
        joinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), ConnectionActivity.class);
                Bundle bundle = new Bundle();
                bundle.putBoolean("JOIN", false);
                intent.putExtras(bundle);
                StartActivity.this.startActivity(intent);
            }
        });

        hide();
    }

    @SuppressLint("InlinedApi")
    private void hide() {

        //Lollipop and higher
        if (Build.VERSION.SDK_INT >= 21) {
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hide();
    }
}




