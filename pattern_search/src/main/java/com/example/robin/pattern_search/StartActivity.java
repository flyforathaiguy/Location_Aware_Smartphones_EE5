package com.example.robin.pattern_search;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;

import android.widget.TextView;

import be.groept.emedialab.util.GlobalResources;

public class StartActivity extends Activity{
    private View mContentView;

    TextView txtMessage;

    //Animation
    Animation animMove;
    Animation animBounce;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        GlobalResources.getInstance().getDevice().setMac(BluetoothAdapter.getDefaultAdapter().getAddress());
        mContentView = findViewById(R.id.Pattern_Search);
        //setContentView(R.layout.activity_start);
        txtMessage = (TextView) findViewById(R.id.textView);

        // load the animations
        animMove = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.move);

                txtMessage.setVisibility(View.VISIBLE);
                // start the animation
                txtMessage.startAnimation(animMove);

        animBounce = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.bounce);
        Button JoinButton = (Button) findViewById(R.id.join);
        JoinButton.setVisibility(View.VISIBLE);
        JoinButton.startAnimation(animBounce);

        Button CreateButton = (Button) findViewById(R.id.start_new);
        CreateButton.setVisibility(View.VISIBLE);
        CreateButton.startAnimation(animBounce);

        Button RULEButton = (Button) findViewById(R.id.game_rule);
        RULEButton.setVisibility(View.VISIBLE);
        RULEButton.startAnimation(animBounce);


        JoinButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                           Intent intent = new Intent(getBaseContext(), ConnectionActivity.class);
                            Bundle bundle = new Bundle();
                            bundle.putBoolean("create", false);
                            intent.putExtras(bundle);
                            StartActivity.this.startActivity(intent);

                        }
                    });

        CreateButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(getBaseContext(), ConnectionActivity.class);
                            Bundle bundle = new Bundle();
                            bundle.putBoolean("create", true);
                            intent.putExtras(bundle);
                            StartActivity.this.startActivity(intent);
                        }
                    });

        RULEButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(getBaseContext(), RuleActivity.class);
                            Bundle bundle = new Bundle();
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


