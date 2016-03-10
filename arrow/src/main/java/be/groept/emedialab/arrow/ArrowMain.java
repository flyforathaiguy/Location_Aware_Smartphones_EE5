package be.groept.emedialab.arrow;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import org.opencv.android.OpenCVLoader;

import be.groept.emedialab.util.GlobalResources;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class ArrowMain extends AppCompatActivity {

    private View mContentView;

    private String TAG = "PatternDetector";

    /**
     * Creates the layout for the arrow application.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_arrow);

        if(!OpenCVLoader.initDebug()){
            Log.d(TAG, "Init error");
        }

        GlobalResources.getInstance().setContext(getBaseContext());

        mContentView = findViewById(R.id.parentContainer);
        Button createButton = (Button) findViewById(R.id.createGame);
        createButton.setOnClickListener(new View.OnClickListener() {

            /**
             * Switches to a new Intent containing the ArrowConnectionActivity class and interface.
             * This will bring the player to a new screen when 'clicking' on 'createGame'.
             * @param v
             */
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), ArrowConnectionActivity.class);
                Bundle bundle = new Bundle();
                bundle.putBoolean("create", true);
                intent.putExtras(bundle);
                ArrowMain.this.startActivity(intent);
            }
        });

        Button joinButton = (Button) findViewById(R.id.joinGame);
        joinButton.setOnClickListener(new View.OnClickListener() {

            /**
             * Switches to a new Intent containing the ArrowConnectionActivity class and interface.
             * This will bring the player to a new screen when 'clicking' on 'joinGame'.
             * @param v
             */
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), ArrowConnectionActivity.class);
                Bundle bundle = new Bundle();
                bundle.putBoolean("create", false);
                intent.putExtras(bundle);
                ArrowMain.this.startActivity(intent);
            }
        });

        hide();
    }

    /**
     * Hides the actionbar if the smartphone's SDK is too high.
     */
    @SuppressLint("InlinedApi")
    private void hide() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        // Lollipop and higher
        if(Build.VERSION.SDK_INT >= 21){
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }

    /**
     * Inflates the menu hierarchy from the specified XML resource.
     * @param menu
     * @return true when finished
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.about, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.action_about){
            AlertDialog alertDialog = new AlertDialog.Builder(ArrowMain.this).create();
            alertDialog.setTitle(getResources().getString(R.string.about_title));
            alertDialog.setMessage(String.format(getResources().getString(R.string.about_message), BuildConfig.VERSION_NAME));
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getResources().getString(R.string.about_button),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            alertDialog.show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
