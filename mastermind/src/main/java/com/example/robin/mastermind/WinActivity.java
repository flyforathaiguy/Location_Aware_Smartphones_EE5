package com.example.robin.mastermind;

//import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

/**
 * Created by Robin on 6/05/2016.
 */
public class WinActivity  extends Activity {
    ImageView congra;
    private View mContentView;
    private boolean robin_argument;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_win);
        mContentView = findViewById(R.id.win);

        congra= (ImageView) findViewById(R.id.imageView3);

    Button restartButton = (Button) findViewById(R.id.restart);
    restartButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            finish();
            /*
            Intent intent = new Intent(getBaseContext(), GameChoose.class);
            Bundle bundle = new Bundle();
            bundle.putBoolean("restart", true);
            intent.putExtras(bundle);
            WinActivity.this.startActivity(intent);
            */
        }
    });

    hide();
}

    /*
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
    */

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


