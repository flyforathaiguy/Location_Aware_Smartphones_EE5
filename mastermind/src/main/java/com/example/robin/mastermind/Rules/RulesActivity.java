package com.example.robin.mastermind.Rules;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.View;

import com.example.robin.mastermind.R;

import java.util.List;
import java.util.Vector;

/**
 * Created by Jasper on 10/05/2016.
 */
public class RulesActivity extends Activity{

    private PagerAdapter mPagerAdapter;
    private View mContentView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.viewpager_layout);
        mContentView = findViewById(R.id.coverLayout);
        hide();
        initialisePaging();
    }

    private void initialisePaging() {
        List<Fragment> fragments = new Vector<Fragment>();
        fragments.add(Fragment.instantiate(this, fragment1.class.getName()));
        fragments.add(Fragment.instantiate(this, fragment2.class.getName()));
        fragments.add(Fragment.instantiate(this, fragment3.class.getName()));
        fragments.add(Fragment.instantiate(this, fragment4.class.getName()));
        fragments.add(Fragment.instantiate(this, fragment5.class.getName()));
        fragments.add(Fragment.instantiate(this, fragment6.class.getName()));

        mPagerAdapter = new PagerAdapter(this.getFragmentManager(), fragments);

        ViewPager pager = (ViewPager) findViewById(R.id.viewPager);
        pager.setAdapter(mPagerAdapter);
        pager.setOffscreenPageLimit(5);
        pager.setPadding(0, 0, 0, 0);
        pager.setPageMargin(0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @SuppressLint("InlinedApi")
    private void hide(){
        android.app.ActionBar actionBar = getActionBar();
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
}
