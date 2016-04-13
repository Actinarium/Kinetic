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

import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import com.actinarium.kinetic.R;
import com.actinarium.kinetic.util.LookupTableInterpolator;

/**
 * A holder for a single result entry (title, chart, output range etc). Also holds an interpolator for animation
 *
 * @author Paul Danyliuk
 */
public class ResultHolder implements AdapterView.OnItemSelectedListener, CompoundButton.OnCheckedChangeListener {

    private final int mId;
    private final String mTitle;
    private final Host mHost;

    private final boolean mIsRotation;

    private final Switch mToggle;
    private final KineticChart mChart;
    private final View mRangeHolder;
    private final Spinner mAnimatorSpinner;

    private boolean mIsSpinnerListenerLocked;

    private final LookupTableInterpolator mInterpolator;
    private float mMagnitude;

    private int mLength;

    /**
     * Create a holder for a single result row and wire up interactivity
     *
     * @param id               Integer to identify this holder in a callback
     * @param host             Callback to the fragment/activity/whatever
     * @param rootView         A row view inflated from R.layout.item_measurement
     * @param title            Title to display for this item
     * @param isRotation       If this chart is for rotation data as opposed to offset data
     * @param isEnabled        If this result is enabled (switch is turned on)
     * @param selectedAnimator Which animator renders this recording
     */
    public ResultHolder(int id, Host host, View rootView, String title, boolean isRotation, boolean isEnabled, int selectedAnimator) {
        mId = id;
        mHost = host;
        mTitle = title;
        mIsRotation = isRotation;

        mInterpolator = new LookupTableInterpolator();

        TextView titleLabel = (TextView) rootView.findViewById(R.id.title);
        titleLabel.setText(title);

        mChart = (KineticChart) rootView.findViewById(R.id.chart);
        mRangeHolder = rootView.findViewById(R.id.range_holder);

        mToggle = (Switch) rootView.findViewById(R.id.toggle);
        mAnimatorSpinner = (Spinner) rootView.findViewById(R.id.preview_as);

        // First set the values
        mToggle.setChecked(isEnabled);
        setExpanded(isEnabled);
        mAnimatorSpinner.setSelection(selectedAnimator + 1, false);

        // Then register listeners
        mToggle.setOnCheckedChangeListener(this);
        mAnimatorSpinner.setOnItemSelectedListener(ResultHolder.this);
    }

    private void setExpanded(boolean expanded) {
        if (expanded) {
            mChart.setVisibility(View.VISIBLE);
            mRangeHolder.setVisibility(View.VISIBLE);
        } else {
            mChart.setVisibility(View.GONE);
            mRangeHolder.setVisibility(View.GONE);
        }
    }

    public void setData(long[] times, float[] values, int length, float linearMagnitude) {
        mLength = length;

        // Calculate real min and max - used to normalize interpolator values
        float realMin = values[0];
        float realMax = values[0];
        for (int i = 1; i < length; i++) {
            if (values[i] < realMin) {
                realMin = values[i];
            } else if (values[i] > realMax) {
                realMax = values[i];
            }
        }

        mInterpolator.setData(values);
        mInterpolator.setRange(0, mLength - 1);
        if (realMax > -realMin && realMax > 0) {
            // Let the multiplier be the negative of the maximum value (so that maximum value maps to 1f)
            mInterpolator.setTransformation(0f, 1 / realMax);
        } else if (realMin < 0) {
            // If we're all about negative values, let the multiplier be -minimum (so that minimum value is -1f)
            mInterpolator.setTransformation(0f, 1 / -realMin);
        } else {
            // Both min and max are zero. Multiplier is 1
            mInterpolator.setTransformation(0f, 1f);
        }

        // Determine chart min/max - we don't want to look at over-magnified jitter
        float chartMin, chartMax;
        // And also determine magnitude for the preview
        if (mIsRotation) {
            // If those are rotation values, make them a multiple of pi/2
            chartMin = (float) (Math.floor(realMin / Math.PI * 2.0) * Math.PI / 2);
            chartMax = (float) (Math.ceil(realMax / Math.PI * 2.0) * Math.PI / 2);
            // Magnitude will be the exact value as a maximum (converted to degrees)
            mMagnitude = (float) (mInterpolator.getMultiplier() * 180 / Math.PI);
        } else {
            // If those are offset values, I want to make the minimum range of -5cm..5cm
            chartMin = Math.min(realMin, -0.05f);
            chartMax = Math.max(realMax, 0.05f);
            // Magnitude is always the constant
            // todo: no, it should be determined proportionally based on what's mapped to X / Y
            mMagnitude = linearMagnitude;
        }
        mMagnitude = 100f;

        mChart.setData(times, values, length, chartMin, chartMax);
    }

    public void setTrim(float trimStart, float trimEnd) {
        final int start = (int) (mLength * trimStart);
        final int end = (int) (mLength * (1 - trimEnd) + 0.5);
        mInterpolator.setRange(start, end);
        mChart.setTrim(trimStart, trimEnd);
    }

    public void setSelectedAnimator(int animator) {
        mIsSpinnerListenerLocked = true;
        mAnimatorSpinner.setSelection(animator + 1, false);
    }

    public LookupTableInterpolator getInterpolator() {
        return mInterpolator;
    }

    public float getMagnitude() {
        return mMagnitude;
    }

    public String getTitle() {
        return mTitle;
    }

    public boolean isEnabled() {
        return mToggle.isChecked();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (!mIsSpinnerListenerLocked) {
            mHost.onAnimatorSelected(mId, position - 1);
        } else {
            mIsSpinnerListenerLocked = false;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) { /* no-op */ }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        setExpanded(isChecked);
        mHost.onResultToggle(mId, isChecked);
    }

    public interface Host {
        void onResultToggle(int id, boolean isEnabled);
        void onAnimatorSelected(int id, int animator);
    }
}
