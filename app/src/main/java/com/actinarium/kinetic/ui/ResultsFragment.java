package com.actinarium.kinetic.ui;


import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Toast;
import com.actinarium.kinetic.R;
import com.actinarium.kinetic.util.DataSet3;

/**
 * A fragment that displays recording result. Just like record fragment, implements a listener for the seek bars right
 * away
 */
public class ResultsFragment extends Fragment implements SeekBar.OnSeekBarChangeListener, ResultHolder.Host {

    public static final String TAG = "ResultsFragment";

    private static final int RESULT_OFFSET_X = 0;
    private static final int RESULT_OFFSET_Y = 1;
    private static final int RESULT_OFFSET_Z = 2;
    private static final int RESULT_ROT_PITCH = 3;
    private static final int RESULT_ROT_ROLL = 4;
    private static final int RESULT_ROT_YAW = 5;

    private Host mHost;
    private DataSet3 mAccelData;
    private DataSet3 mGyroData;

    private ResultHolder[] mHolders = new ResultHolder[6];
    private PreviewHolder mPreviewHolder;
    private SeekBar mTrimStart;
    private SeekBar mTrimEnd;

    public ResultsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mHost = (Host) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_results, container, false);

        mAccelData = mHost.getAccelData();
        mGyroData = mHost.getGyroData();

        // Discard button (X)
        ImageButton discard = (ImageButton) view.findViewById(R.id.discard);
        discard.setImageAlpha(180);
        discard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDiscard();
            }
        });

        // Preview animation
        View animatedView = view.findViewById(R.id.preview_sprite);
        int linearMagnitude = getResources().getDimensionPixelOffset(R.dimen.linear_magnitude);
        int elevationMagnitude = getResources().getDimensionPixelOffset(R.dimen.elevation_magnitude);
        int animDuration = (int) ((mAccelData.times[mAccelData.length - 1] - mAccelData.times[0]) / 1000000L);
        mPreviewHolder = new PreviewHolder(animatedView, mAccelData, mGyroData, linearMagnitude, elevationMagnitude, 180);
        mPreviewHolder.setDuration(animDuration);

        // Trim range
        mTrimStart = (SeekBar) view.findViewById(R.id.trim_start);
        mTrimStart.setOnSeekBarChangeListener(this);
        mTrimEnd = (SeekBar) view.findViewById(R.id.trim_end);
        mTrimEnd.setOnSeekBarChangeListener(this);

        String[] resultTitles = getResources().getStringArray(R.array.result_titles);
        ViewGroup resultsContainer = (ViewGroup) view.findViewById(R.id.item_container);
        for (int i = 0; i < 6; i++) {
            View item = inflater.inflate(R.layout.item_measurement, resultsContainer, false);
            resultsContainer.addView(item);
            mHolders[i] = new ResultHolder(i, this, item, resultTitles[i], i >= 3);
        }

        Button export = (Button) inflater.inflate(R.layout.item_export_button, resultsContainer, false);
        export.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onExport();
            }
        });
        resultsContainer.addView(export);

        fillChartsData();
        mPreviewHolder.startAnimation();

        return view;
    }

    private void fillChartsData() {
        mHolders[0].plotData(mAccelData.times, mAccelData.valuesX, mAccelData.length);
        mHolders[1].plotData(mAccelData.times, mAccelData.valuesY, mAccelData.length);
        mHolders[2].plotData(mAccelData.times, mAccelData.valuesZ, mAccelData.length);
        mHolders[3].plotData(mGyroData.times, mGyroData.valuesX, mGyroData.length);
        mHolders[4].plotData(mGyroData.times, mGyroData.valuesY, mGyroData.length);
        mHolders[5].plotData(mGyroData.times, mGyroData.valuesZ, mGyroData.length);
    }

    /**
     * Called when either X button or Back button is pressed
     */
    void onDiscard() {
        new AlertDialog.Builder(getContext())
                .setMessage(R.string.dialog_discard_message)
                .setPositiveButton(R.string.discard, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mHost.onRecordingDiscarded();
                    }
                })
                .setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .show();
    }

    private void onExport() {
        Toast.makeText(getContext(), "Export clicked!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResultToggle(int id, boolean enabled) {
        switch (id) {
            case RESULT_OFFSET_X:
                mPreviewHolder.stopAnimation();
                mPreviewHolder.setStatus(
                        enabled, mPreviewHolder.isYEnabled(),
                        mPreviewHolder.isZEnabled(), mPreviewHolder.isRotationEnabled()
                );
                mPreviewHolder.startAnimation();
                break;
            case RESULT_OFFSET_Y:
                mPreviewHolder.stopAnimation();
                mPreviewHolder.setStatus(
                        mPreviewHolder.isXEnabled(), enabled,
                        mPreviewHolder.isZEnabled(), mPreviewHolder.isRotationEnabled()
                );
                mPreviewHolder.startAnimation();
                break;
            case RESULT_OFFSET_Z:
                mPreviewHolder.stopAnimation();
                mPreviewHolder.setStatus(
                        mPreviewHolder.isXEnabled(), mPreviewHolder.isYEnabled(),
                        enabled, mPreviewHolder.isRotationEnabled()
                );
                mPreviewHolder.startAnimation();
                break;
            case RESULT_ROT_YAW:
                mPreviewHolder.stopAnimation();
                mPreviewHolder.setStatus(
                        mPreviewHolder.isXEnabled(), mPreviewHolder.isYEnabled(),
                        mPreviewHolder.isZEnabled(), enabled
                );
                mPreviewHolder.startAnimation();
                break;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mPreviewHolder.stopAnimation();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        mPreviewHolder.startAnimation();
    }

    public interface Host {
        DataSet3 getAccelData();
        DataSet3 getGyroData();
        void onRecordingDiscarded();
    }

}
