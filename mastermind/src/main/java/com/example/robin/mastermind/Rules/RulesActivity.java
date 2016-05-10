package com.example.robin.mastermind.Rules;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.Menu;

import com.example.robin.mastermind.R;

import java.util.List;
import java.util.Vector;

/**
 * Created by Jasper on 10/05/2016.
 */
public class RulesActivity extends Activity{

    private PagerAdapter mPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.viewpager_layout);
        initialisePaging();
    }

    private void initialisePaging() {
        List<Fragment> fragments = new Vector<Fragment>();
        fragments.add(Fragment.instantiate(this, fragment1.class.getName()));
        fragments.add(Fragment.instantiate(this, fragment2.class.getName()));
        fragments.add(Fragment.instantiate(this, fragment3.class.getName()));
        fragments.add(Fragment.instantiate(this, fragment4.class.getName()));
        fragments.add(Fragment.instantiate(this, fragment5.class.getName()));

        mPagerAdapter = new PagerAdapter(this.getFragmentManager(), fragments);

        ViewPager pager = (ViewPager) findViewById(R.id.viewPager);
        pager.setAdapter(mPagerAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }
}
