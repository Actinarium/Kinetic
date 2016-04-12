package com.actinarium.kinetic.ui;


import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.actinarium.kinetic.R;
import com.actinarium.kinetic.pipeline.DataRecorder;
import com.actinarium.kinetic.pipeline.DataTransformer;
import com.actinarium.kinetic.util.DataSet3;
import com.actinarium.kinetic.util.DataSet4;

/**
 * A fragment for welcome screen with record button. Since it's the only button on the screen, we can avoid anonymous
 * classes and make the fragment a listener for the button itself
 */
public class RecordFragment extends Fragment implements View.OnClickListener, DataRecorder.Callback {

    public static final String TAG = "RecordFragment";

    private Host mHost;
    private DataRecorder mRecorder;
    private boolean mIsRecording;

    private FloatingActionButton mRecordButton;

    public RecordFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mHost = (Host) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_welcome, container, false);

        mRecordButton = (FloatingActionButton) view.findViewById(R.id.record);
        mRecordButton.setOnClickListener(this);

        mRecorder = new DataRecorder(getContext(), this, DataRecorder.DEFAULT_RECORDING_TIME_MILLIS, DataRecorder.DEFAULT_SAMPLING_MICROS);

        return view;
    }

    @Override
    public void onClick(View v) {
        if (!mIsRecording) {
            // Start recording
            mIsRecording = true;
            mRecordButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause));
            mRecorder.start();
        } else {
            // Stop recording - the drawable and the boolean will be updated in a callback method
            mRecorder.stop();
        }
    }

    @Override
    public void onDataRecordedResult(@DataRecorder.Status int status, DataSet3 accelData, DataSet3 gyroData, DataSet4 rotVectorData) {
        mIsRecording = false;
        mRecordButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_record));

        if (status < 0) {
            Toast.makeText(getContext(), R.string.app_error, Toast.LENGTH_SHORT).show();
        } else {
            // Try to remove gravity from raw readings
//            DataTransformer.removeGravityFromRaw(accelData, rotVectorData, accelData);
//            DataTransformer.reduceJitter(accelData, 0.1f, accelData);

            // Integrate raw readings:

            // acceleration -> velocity
            DataTransformer.integrate(accelData);
            // velocity -> offset
            DataTransformer.integrate(accelData);

            // angular velocity -> phase
            DataTransformer.integrate(gyroData);

//            DataTransformer.offsetYaw(gyroData);

            mHost.onDataRecorded(accelData, gyroData, rotVectorData);
        }
    }

    public interface Host {
        void onDataRecorded(DataSet3 accelData, DataSet3 gyroData, DataSet4 rotVectorData);
    }
}
