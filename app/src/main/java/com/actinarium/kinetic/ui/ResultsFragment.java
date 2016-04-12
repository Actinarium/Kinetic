package com.actinarium.kinetic.ui;


import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import com.actinarium.kinetic.R;
import com.actinarium.kinetic.util.DataSet3;
import com.actinarium.kinetic.util.DataSet4;

/**
 * A simple {@link Fragment} subclass.
 */
public class ResultsFragment extends Fragment {

    public static final String TAG = "ResultsFragment";

    private Host mHost;
    private DataSet3 mAccelData;
    private DataSet3 mGyroData;
    private DataSet4 mRotVectorData;

    private ResultHolder[] mHolders = new ResultHolder[9];

    public ResultsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mHost = (Host) context;
        mAccelData = mHost.getAccelData();
        mGyroData = mHost.getGyroData();
        mRotVectorData = mHost.getRotVectorData();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_results, container, false);

        ImageButton discard = (ImageButton) view.findViewById(R.id.discard);
        discard.setImageAlpha(180);
        discard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDiscard();
            }
        });

        String[] resultTitles = getResources().getStringArray(R.array.result_titles);
        ViewGroup resultsContainer = (ViewGroup) view.findViewById(R.id.item_container);
        for (int i = 0; i < 9; i++) {
            View item = inflater.inflate(R.layout.item_measurement, resultsContainer, false);
            resultsContainer.addView(item);
            mHolders[i] = new ResultHolder(item, resultTitles[i], i >= 3);
        }
        inflater.inflate(R.layout.item_export_button, resultsContainer);

        fillChartsData();

        return view;
    }

    private void fillChartsData() {
        mHolders[0].plotData(mAccelData.times, mAccelData.valuesX, mAccelData.length);
        mHolders[1].plotData(mAccelData.times, mAccelData.valuesY, mAccelData.length);
        mHolders[2].plotData(mAccelData.times, mAccelData.valuesZ, mAccelData.length);
        mHolders[3].plotData(mGyroData.times, mGyroData.valuesX, mGyroData.length);
        mHolders[4].plotData(mGyroData.times, mGyroData.valuesY, mGyroData.length);
        mHolders[5].plotData(mGyroData.times, mGyroData.valuesZ, mGyroData.length);
        mHolders[6].plotData(mRotVectorData.times, mRotVectorData.valuesX, mRotVectorData.length);
        mHolders[7].plotData(mRotVectorData.times, mRotVectorData.valuesY, mRotVectorData.length);
        mHolders[8].plotData(mRotVectorData.times, mRotVectorData.valuesZ, mRotVectorData.length);
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

    public interface Host {
        DataSet3 getAccelData();
        DataSet3 getGyroData();
        DataSet4 getRotVectorData();
        void onRecordingDiscarded();
    }

}
