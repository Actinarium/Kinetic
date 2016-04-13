/*
 * Copyright (C) 2016 Actinarium
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.actinarium.kinetic.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import com.actinarium.kinetic.R;
import com.actinarium.kinetic.util.DataSet3;
import com.actinarium.kinetic.util.DataSet4;

public class MainActivity extends AppCompatActivity implements RecordFragment.Host, ResultsFragment.Host {

    private static final String ARG_ACCEL = "com.actinarium.kinetic.bundle.ACCEL";
    private static final String ARG_GYRO = "com.actinarium.kinetic.bundle.GYRO";
    private static final String ARG_RV = "com.actinarium.kinetic.bundle.RV";
    private static final String ARG_RHS = "com.actinarium.kinetic.bundle.RHS";
    private static final String ARG_HAM = "com.actinarium.kinetic.bundle.HAM";

    private DataSet3 mAccelData;
    private DataSet3 mGyroData;
    private DataSet4 mRotVectorData;

    private boolean[] mResultHoldersState;
    private int[] mHolderToAnimatorMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            mAccelData = savedInstanceState.getParcelable(ARG_ACCEL);
            mGyroData = savedInstanceState.getParcelable(ARG_GYRO);
            mRotVectorData = savedInstanceState.getParcelable(ARG_RV);
            mResultHoldersState = savedInstanceState.getBooleanArray(ARG_RHS);
            mHolderToAnimatorMap = savedInstanceState.getIntArray(ARG_HAM);
            return;
        } else {
            mResultHoldersState = new boolean[]{true, true, true, true, true, true};
            mHolderToAnimatorMap = new int[]{
                    PreviewHolder.ANIMATOR_X, PreviewHolder.ANIMATOR_Y, PreviewHolder.NO_ANIMATOR,
                    PreviewHolder.NO_ANIMATOR, PreviewHolder.NO_ANIMATOR, PreviewHolder.ANIMATOR_ROTATION,
            };
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
    public void onRecordingDiscarded() {
        super.onBackPressed();
    }

    @Override
    public boolean[] getResultHoldersState() {
        return mResultHoldersState;
    }

    @Override
    public int[] getHolderToAnimatorMap() {
        return mHolderToAnimatorMap;
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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(ARG_ACCEL, mAccelData);
        outState.putParcelable(ARG_GYRO, mGyroData);
        outState.putParcelable(ARG_RV, mRotVectorData);
        outState.putBooleanArray(ARG_RHS, mResultHoldersState);
        outState.putIntArray(ARG_HAM, mHolderToAnimatorMap);
    }
}
