package com.example.robin.pattern_search;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;


public class WinnerActivity extends Activity {
    TextView text;
    ImageView winner;
    private View mContentView;
    Animation animFadeIn;
    Animation animBounce;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_winner);
        mContentView = findViewById(R.id.winner_page);
        winner= (ImageView) findViewById(R.id.winner);
        text = (TextView) findViewById(R.id.text);
        // load the animations
        animFadeIn = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.fadein);

        winner.setVisibility(View.VISIBLE);
        // start the animation
        winner.startAnimation(animFadeIn);
        
        text = (TextView) findViewById(R.id.text);
        animBounce = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.bounce);
        text.setVisibility(View.VISIBLE);
        // start the animation
        text.startAnimation(animBounce);


        Button PlayAgainButton = (Button) findViewById(R.id.restart);
        PlayAgainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            Intent intent = new Intent(getBaseContext(), GameWindow.class);
            Bundle bundle = new Bundle();
            bundle.putBoolean("restart", true);
            intent.putExtras(bundle);
            WinnerActivity.this.startActivity(intent);

            }
        });

        Button quitButton = (Button) findViewById(R.id.exit);
        quitButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v){
                finish();
            }
        });
        hide();
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

