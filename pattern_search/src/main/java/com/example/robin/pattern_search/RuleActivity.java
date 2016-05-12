package com.example.robin.pattern_search;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;

/**
 * Created by yonas-Haregot on 5/11/2016.
 */
public class RuleActivity extends ActionBarActivity {

    private View mContentView;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rules);
       mContentView = findViewById(R.id.rule);

        //winner= (ImageView) findViewById(R.id.winner);


    Button Start_GameButton = (Button) findViewById(R.id.start);
    Start_GameButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //   finish();

            Intent intent = new Intent(getBaseContext(), GameWindow.class);
            Bundle bundle = new Bundle();

            intent.putExtras(bundle);
            RuleActivity.this.startActivity(intent);

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
