package com.actinarium.kinetic.ui;

import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import com.actinarium.kinetic.R;
import com.actinarium.kinetic.util.LookupTableInterpolator;

/**
 * A holder for a single result entry (title, chart, output range etc). Also holds an interpolator for animation
 *
 * @author Paul Danyliuk
 */
public class ResultHolder {

    private final int mId;
    private final Host mHost;

    private final boolean mIsRotation;
    private boolean mIsRangeZeroModified;
    private boolean mIsRangeOneModified;

    private final Switch mToggle;
    private final KineticChart mChart;
    private final LookupTableInterpolator mInterpolator;
    private int mLength;

    /**
     * Create a holder for a single result row and wire up interactivity
     *
     * @param id         Integer to identify this holder in a callback
     * @param host       Callback to the fragment/activity/whatever
     * @param rootView   A row view inflated from R.layout.item_measurement
     * @param title      Title to display for this item
     * @param isRotation If this chart is for rotation data as opposed to offset data
     */
    public ResultHolder(int id, Host host, View rootView, String title, boolean isRotation) {
        mId = id;
        mHost = host;
        mIsRotation = isRotation;

        TextView titleLabel = (TextView) rootView.findViewById(R.id.title);
        titleLabel.setText(title);

        mChart = (KineticChart) rootView.findViewById(R.id.chart);

        final View rangeHolder = rootView.findViewById(R.id.range_holder);

        mToggle = (Switch) rootView.findViewById(R.id.toggle);
        mToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mChart.setVisibility(View.VISIBLE);
                    rangeHolder.setVisibility(View.VISIBLE);
                } else {
                    mChart.setVisibility(View.GONE);
                    rangeHolder.setVisibility(View.GONE);
                }
                mHost.onResultToggle(mId, isChecked);
            }
        });

        mInterpolator = new LookupTableInterpolator();
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

        if (mIsRotation) {
            // Now, if that's rotation values, I want to make them a multiple of pi/2 for easier application
            min = (float) (Math.floor(min / Math.PI * 2.0) * Math.PI / 2);
            max = (float) (Math.ceil(max / Math.PI * 2.0) * Math.PI / 2);
        } else {
            // Or if it's offset values, I want to make the minimum range of -5cm..5cm
            min = Math.min(min, -0.05f);
            max = Math.max(max, 0.05f);
        }

        mInterpolator.setData(values);
        mInterpolator.setRange(0, mLength - 1);
        mInterpolator.setTransformation(0f, 1 / (max - min));
        mChart.setData(times, values, length, min, max);
    }

    public void setTrim(float trimStart, float trimEnd) {
        final int start = (int) (mLength * trimStart);
        final int end = (int) (mLength * (1 - trimEnd) + 0.5);
        mInterpolator.setRange(start, end);
        mChart.setTrim(trimStart, trimEnd);
    }

    public LookupTableInterpolator getInterpolator() {
        return mInterpolator;
    }

    public interface Host {
        void onResultToggle(int id, boolean enabled);
    }
}
