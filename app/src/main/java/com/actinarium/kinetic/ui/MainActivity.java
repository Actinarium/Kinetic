package com.actinarium.kinetic.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import com.actinarium.kinetic.R;
import com.actinarium.kinetic.util.DataSet3;
import com.actinarium.kinetic.util.DataSet4;

public class MainActivity extends AppCompatActivity implements RecordFragment.Host, ResultsFragment.Host {

    private DataSet3 mAccelData;
    private DataSet3 mGyroData;
    private DataSet4 mRotVectorData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            return;
        }

        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.root_container, new RecordFragment(), RecordFragment.TAG)
                .commit();
    }

    @Override
    public void onDataRecorded(DataSet3 accelData, DataSet3 gyroData, DataSet4 rotVectorData) {
        mAccelData = accelData;
        mGyroData = gyroData;
        mRotVectorData = rotVectorData;

        getSupportFragmentManager()
                .beginTransaction()
                .addToBackStack(null)
                .setCustomAnimations(R.anim.slide_in_results, R.anim.fade_out_welcome, R.anim.fade_in_welcome, R.anim.slide_out_results)
                .replace(R.id.root_container, new ResultsFragment(), ResultsFragment.TAG)
                .commit();
    }

    @Override
    public DataSet3 getAccelData() {
        return mAccelData;
    }

    @Override
    public DataSet3 getGyroData() {
        return mGyroData;
    }

    @Override
    public DataSet4 getRotVectorData() {
        return mRotVectorData;
    }

    @Override
    public void onRecordingDiscarded() {
        super.onBackPressed();
    }

    @Override
    public void onBackPressed() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(ResultsFragment.TAG);
        if (fragment instanceof ResultsFragment) {
            ((ResultsFragment) fragment).onDiscard();
        } else {
            super.onBackPressed();
        }
    }
}
