package com.actinarium.kinetic.ui;

import android.view.View;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import com.actinarium.kinetic.KineticChart;
import com.actinarium.kinetic.R;

/**
 * A holder for a single result entry (title, chart, output range etc)
 *
 * @author Paul Danyliuk
 */
public class ResultHolder implements SeekBar.OnSeekBarChangeListener {

    private final boolean mIsRotation;
    private boolean mIsRangeZeroModified;
    private boolean mIsRangeOneModified;

    private final Switch mToggle;
    private final KineticChart mChart;
    private final SeekBar mRangeZero;
    private final SeekBar mRangeOne;

    /**
     * Create a holder for a single result row and wire up interactivity
     *
     * @param rootView   A row view inflated from R.layout.item_measurement
     * @param title      Title to display for this item
     * @param isRotation If this chart is for rotation data as opposed to offset data
     */
    public ResultHolder(View rootView, String title, boolean isRotation) {
        mIsRotation = isRotation;

        TextView titleLabel = (TextView) rootView.findViewById(R.id.title);
        titleLabel.setText(title);

        mChart = (KineticChart) rootView.findViewById(R.id.chart);
        mRangeZero = (SeekBar) rootView.findViewById(R.id.range_0);
        mRangeOne = (SeekBar) rootView.findViewById(R.id.range_1);

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
            }
        });

        mRangeZero.setOnSeekBarChangeListener(this);
        mRangeOne.setOnSeekBarChangeListener(this);

    }

    public void plotData(long[] times, float[] values, int length) {
        float min = mIsRotation ? -1f : -0.1f;
        float max = mIsRotation ? 1f : 0.1f;
//        float min = -1f;
//        float max = 1f;
        for (int i = 1; i < length; i++) {
            if (values[i] < min) {
                min = values[i];
            } else if (values[i] > max) {
                max = values[i];
            }
        }

        mChart.setData(times, values, length, min, max, 0, 0);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (seekBar == mRangeZero) {
            if (fromUser) {
                mIsRangeZeroModified = true;
            }
            // todo: update chart zero line
        } else if (seekBar == mRangeOne) {
            if (fromUser) {
                mIsRangeOneModified = true;
            }
            // todo: update chart one line
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) { /* no-op */ }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) { /* no-op */ }
}
