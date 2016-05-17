package com.example.robin.pattern_search;

import android.app.Activity;
import android.os.Build;

import android.os.Bundle;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

public class LoserActivity extends Activity{

    private View mContentView;
    private TextView loseText;
    private Button endButton;

    //Animation
    Animation animationFadeIn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loser);
        mContentView = findViewById(R.id.loser);
        hide();

        //Get the TextView
        loseText = (TextView) findViewById(R.id.loseText);

        //Get the Button
        endButton = (Button) findViewById(R.id.endButton);
        endButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //Load animation
        animationFadeIn = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in);

        //Start the animation
        loseText.setAnimation(animationFadeIn);
        endButton.setAnimation(animationFadeIn);
        animationFadeIn.start();
    }

    private void hide() {
        if (Build.VERSION.SDK_INT >= 21) {
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }
}
