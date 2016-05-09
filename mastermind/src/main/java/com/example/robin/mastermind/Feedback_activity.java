package com.example.robin.mastermind;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.graphics.Color;
import android.os.Build;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;


public class Feedback_activity extends Activity {

    //private RelativeLayout backgroundLayout;
    private View mContentView;

    private ViewGroup background;
    private TextView information;
    private ImageView colorize;
    private ImageView correct;
    private ImageView error;
    private ImageView position;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback_activity);

        background = (ViewGroup) findViewById(R.id.backgroundLayout);
        colorize = (ImageView) findViewById(R.id.colorize);
        correct = (ImageView) findViewById(R.id.correct);
        position = (ImageView) findViewById(R.id.position);
        error = (ImageView) findViewById(R.id.error);

        Bundle bundle = getIntent().getExtras();
        if(bundle != null){
           chooseColor(bundle);
        }
        hide();
    }

    private void hide(){
        if(Build.VERSION.SDK_INT >= 21){
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }

    public void chooseColor(Bundle bundle) {

        int i = bundle.getInt("Color");
        if (i == GameChoose.ALL_CORRECT) {
            background.setBackgroundColor(Color.GREEN);
            colorize.setVisibility(View.INVISIBLE);
            error.setVisibility(View.INVISIBLE);
            position.setVisibility(View.INVISIBLE);
            moveImage(correct);

        } else if (i == GameChoose.CORRECT_POS) {
            background.setBackgroundColor(Color.YELLOW);
            colorize.setVisibility(View.INVISIBLE);
            error.setVisibility(View.INVISIBLE);
            correct.setVisibility(View.INVISIBLE);
            moveImage(position);

        } else if (i == GameChoose.CORRECT_COLOR) {
            background.setBackgroundColor(Color.YELLOW);
            correct.setVisibility(View.INVISIBLE);
            error.setVisibility(View.INVISIBLE);
            position.setVisibility(View.INVISIBLE);
            moveImage(colorize);

        } else if (i == GameChoose.ALL_WRONG) {
            background.setBackgroundColor(Color.RED);
            colorize.setVisibility(View.INVISIBLE);
            correct.setVisibility(View.INVISIBLE);
            position.setVisibility(View.INVISIBLE);
            moveImage(error);

        }
    }

    public void moveImage(ImageView image) {

        //Change the position of the image
        RelativeLayout.LayoutParams positionRules = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        positionRules.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        image.setLayoutParams(positionRules);

        //Change the size of the image
        ViewGroup.LayoutParams sizeRules = image.getLayoutParams();
        sizeRules.width = 450;
        sizeRules.height = 300;
        image.setLayoutParams(sizeRules);

    }
}

