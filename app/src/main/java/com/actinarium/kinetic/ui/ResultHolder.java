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

    public void setData(long[] times, float[] values, int length) {
        mLength = length;

        // Calculate real min and max
        float min = values[0];
        float max = values[0];
        for (int i = 1; i < length; i++) {
            if (values[i] < min) {
                min = values[i];
            } else if (values[i] > max) {
                max = values[i];
            }
        }

        mInterpolator.setData(values);
        mInterpolator.setRange(0, mLength - 1);
        mInterpolator.setTransformation(0f, 1 / (max - min));

        // todo: calculate magnitude for interpolation
        if (mIsRotation) {
            // Now, if that's rotation values, I want to make them a multiple of pi/2 for easier application
//            min = (float) (Math.floor(min / Math.PI * 2.0) * Math.PI / 2);
//            max = (float) (Math.ceil(max / Math.PI * 2.0) * Math.PI / 2);
        } else {
            // Or if it's offset values, I want to make the minimum range of -5cm..5cm
//            min = Math.min(min, -0.05f);
//            max = Math.max(max, 0.05f);
        }
        mMagnitude = 100f;

        mChart.setData(times, values, length, min, max);
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
