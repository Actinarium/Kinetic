package com.actinarium.kinetic.ui;


import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ShareCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Toast;
import com.actinarium.kinetic.R;
import com.actinarium.kinetic.pipeline.CodeGenerator;
import com.actinarium.kinetic.util.DataSet3;

import java.util.ArrayList;
import java.util.Random;

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
    private int mStartProgress;
    private int mEndProgress;
    private int mMax;
    private long mFullDuration;
    private String[] mEpithets;

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
        mFullDuration = (mAccelData.times[mAccelData.length - 1] - mAccelData.times[0]) / 1000000L;
        mPreviewHolder = new PreviewHolder(animatedView, mAccelData, mGyroData, linearMagnitude, elevationMagnitude, 180);
        mPreviewHolder.setDuration(mFullDuration);

        // Trim range
        mTrimStart = (SeekBar) view.findViewById(R.id.trim_start);
        mTrimStart.setOnSeekBarChangeListener(this);
        mTrimEnd = (SeekBar) view.findViewById(R.id.trim_end);
        mTrimEnd.setOnSeekBarChangeListener(this);
        mMax = mTrimStart.getMax();

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
        mEpithets = getResources().getStringArray(R.array.epithets);

        setData();
        mPreviewHolder.startAnimation();

        return view;
    }

    private void setData() {
        mHolders[0].setData(mAccelData.times, mAccelData.valuesX, mAccelData.length);
        mHolders[1].setData(mAccelData.times, mAccelData.valuesY, mAccelData.length);
        mHolders[2].setData(mAccelData.times, mAccelData.valuesZ, mAccelData.length);
        mHolders[3].setData(mGyroData.times, mGyroData.valuesX, mGyroData.length);
        mHolders[4].setData(mGyroData.times, mGyroData.valuesY, mGyroData.length);
        mHolders[5].setData(mGyroData.times, mGyroData.valuesZ, mGyroData.length);

        mPreviewHolder.setInterpolators(
                mHolders[0].getInterpolator(),
                mHolders[1].getInterpolator(),
                mHolders[2].getInterpolator(),
                mHolders[5].getInterpolator()
        );
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
        // Allocate string builder large enough
        StringBuilder exportBuilder = new StringBuilder(4096);

        // Remember the random names we already have in this export to avoid duplication
        ArrayList<Integer> usedInts = new ArrayList<>(6);
        Random random = new Random();

        for (ResultHolder holder : mHolders) {
            if (holder.isEnabled()) {
                // Pick a name
                int index;
                do {
                    index = random.nextInt(mEpithets.length);
                } while (usedInts.contains(index));
                usedInts.add(index);

                // Generate code and append to the builder
                exportBuilder.append(CodeGenerator.generateInterpolatorCode(
                        CodeGenerator.DEFAULT_PACKAGE_NAME,
                        getString(R.string.class_name_template, mEpithets[index]),
                        holder.getTitle(),
                        holder.getInterpolator().exportData()
                )).append("\n\n");
            }
        }

        if (exportBuilder.length() == 0) {
            Toast.makeText(getContext(), R.string.nothing_to_export, Toast.LENGTH_LONG).show();
        } else {
            // Share this to any app that can handle raw text (e.g. a mail app to send generated code to myself)
            ShareCompat.IntentBuilder.from(getActivity())
                    .setChooserTitle(R.string.export_to)
                    .setType("text/plain")
                    .setText(exportBuilder.toString())
                    .startChooser();
        }
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
        mStartProgress = mTrimStart.getProgress();
        mEndProgress = mTrimEnd.getProgress();

        // If one pushes another
        if (mStartProgress + mEndProgress >= mMax) {
            if (seekBar == mTrimStart) {
                mTrimEnd.setProgress(mMax - mStartProgress);
            } else {
                mTrimStart.setProgress(mMax - mEndProgress);
            }
        }

        for (ResultHolder h : mHolders) {
            h.setTrim(mStartProgress / (float) mMax, mEndProgress / (float) mMax);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mPreviewHolder.stopAnimation();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (mStartProgress + mEndProgress < mMax) {
            final long newDuration = mFullDuration * (mMax - mStartProgress - mEndProgress) / mMax;
            mPreviewHolder.setDuration(newDuration);
            mPreviewHolder.startAnimation();
        }
    }

    public interface Host {
        DataSet3 getAccelData();

        DataSet3 getGyroData();

        void onRecordingDiscarded();
    }

}
