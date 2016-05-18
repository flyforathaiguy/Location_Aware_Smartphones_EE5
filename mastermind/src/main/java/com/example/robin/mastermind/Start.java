package com.example.robin.mastermind;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
//import android.media.Image;
import android.os.Build;
import android.os. Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.robin.mastermind.Rules.RulesActivity;

import be.groept.emedialab.util.GlobalResources;

public class Start extends AppCompatActivity{

private View mContentView;
    TextView txtMessage;
    ImageView image;
    Animation animMove;
    Animation animFadeIn;
    Animation animBounce;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_start);
        GlobalResources.getInstance().getDevice().setMac(BluetoothAdapter.getDefaultAdapter().getAddress());
        mContentView = findViewById(R.id.coverLayout);
        txtMessage = (TextView) findViewById(R.id.textView);
        animMove = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.move);
        txtMessage.setVisibility(View.VISIBLE);
        txtMessage.startAnimation(animMove);

        animFadeIn = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.fadein);
        image = (ImageView) findViewById(R.id.imageView);
        image.setVisibility(View.VISIBLE);
        image.startAnimation(animFadeIn);

        animBounce = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.bounce);
        Button joinButton = (Button) findViewById(R.id.join);
        joinButton.setVisibility(View.VISIBLE);
        joinButton.startAnimation(animBounce);

        Button NewGameButton = (Button) findViewById(R.id.create);
        NewGameButton.setVisibility(View.VISIBLE);
        NewGameButton.startAnimation(animBounce);

        Button rulesButton = (Button) findViewById(R.id.rules);
        rulesButton.setVisibility(View.VISIBLE);
        rulesButton.startAnimation(animBounce);



        NewGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), ConnectionActivity.class);
                Bundle bundle = new Bundle();
                bundle.putBoolean("create", true);
                intent.putExtras(bundle);
                Start.this.startActivity(intent);
            }
        });


        joinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), ConnectionActivity.class);
                Bundle bundle = new Bundle();
                bundle.putBoolean("create", false);
                intent.putExtras(bundle);
                Start.this.startActivity(intent);
            }
        });

        rulesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), RulesActivity.class);
                Start.this.startActivity(intent);
            }
        });

        hide();
    }

    @SuppressLint("InlinedApi")
    private void hide(){
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
            actionBar.hide();

        //Lollipop and higher
        if(Build.VERSION.SDK_INT >= 21){
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        hide();
    }
}
