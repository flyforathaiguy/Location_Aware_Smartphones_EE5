package com.example.robin.mastermind;

//import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

/**
 * Created by Robin on 28/04/2016.
 */
public class WinActivity  extends Activity {
    ImageView fail,congra;
    private View mContentView;
    private boolean robin_argument;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_win);
        mContentView = findViewById(R.id.win);

        fail= (ImageView) findViewById(R.id.imageView2);
        congra= (ImageView) findViewById(R.id.imageView3);
/*

        //setting a listneer on robin

        fail.setOnClickListener(Feedback);//controlling this action lister in feedback.... when the feedback button is pressed
        // it will pop up either of the code
        congra.setOnClickListener(Feedback);
        //class that controls which picture will display:
    public void onClick(View view){

        if(view.listner()==pressed && win==true){
            fail.setVisibility(View.GONE);
            congra.setVisibility(View.VISIBLE);
        }
        else{

            congra.setVisibility(View.GONE);
            fail.setVisibility(View.VISIBLE);
        }

    }
*/

    Button restartButton = (Button) findViewById(R.id.restart);
    restartButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getBaseContext(), GameChoose.class);
            Bundle bundle = new Bundle();
            bundle.putBoolean("restart", true);
            intent.putExtras(bundle);
            WinActivity.this.startActivity(intent);
        }
    });

    Button exitButton = (Button) findViewById(R.id.exit);
    exitButton.setOnClickListener(new View.OnClickListener() {

        @Override
        public void onClick(View v) {


            finish();
            //intent.putExtras(bundle);
            //WinActivity.this.startActivity(intent);

        }
    });

    hide();

}


    public void chooseColor(Bundle bundle) {//this class should be based on the feedback of robin result

        int i = bundle.getInt("feedback");
        if (i == GameChoose.ALL_CORRECT) {

            congra.setVisibility(View.VISIBLE);
            fail.setVisibility(View.INVISIBLE);
       } else {
            congra.setVisibility(View.INVISIBLE);
            fail.setVisibility(View.VISIBLE);
        }
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


