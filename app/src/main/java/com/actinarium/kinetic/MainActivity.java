package com.actinarium.kinetic;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final long RECORDING_TIME_MILLIS = 2000L;
    private static final long RECORDING_TIME_NANOS = RECORDING_TIME_MILLIS * 1000000L;
    private static final int RECORDING_LATENCY_MICROS = 5000;
    private static final float ACCEL_JITTER_THRESHOLD = 0.25f;

    private TextView mTimestamp;
    private LineChart mChartX;
    private LineChart mChartY;
    private LineChart mChartZ;
    private Button mRecordButton;

    private long mRecordingStartTime;

    private float[] mValues = new float[1800];
    private long[] mTimes = new long[600];
    private int mValuesLength;
    private int mTimesLength;
    private SensorManager mManager;
    private Sensor mAccelerometerSensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTimestamp = (TextView) findViewById(R.id.timestamp);
        mRecordButton = (Button) findViewById(R.id.record);
        mChartX = (LineChart) findViewById(R.id.chart_x);
        mChartY = (LineChart) findViewById(R.id.chart_y);
        mChartZ = (LineChart) findViewById(R.id.chart_z);

        mRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRecording();
            }
        });

        mManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometerSensor = mManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        final Sensor rotationVectorSensor = mManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
//        manager.registerListener(mRotationAdapter, rotationVectorSensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    private void startRecording() {
        mRecordingStartTime = 0;
        mValuesLength = 0;
        mTimesLength = 0;
        mRecordButton.setEnabled(false);
        mManager.registerListener(this, mAccelerometerSensor, RECORDING_LATENCY_MICROS);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                endRecording();
            }
        }, RECORDING_TIME_MILLIS);
    }

    private void endRecording() {
        mManager.unregisterListener(this, mAccelerometerSensor);
        mRecordButton.setEnabled(true);
        // draw chart

        ArrayList<Entry> entriesX = new ArrayList<>(mTimesLength);
        ArrayList<Entry> entriesY = new ArrayList<>(mTimesLength);
        ArrayList<Entry> entriesZ = new ArrayList<>(mTimesLength);
        ArrayList<String> dataX = new ArrayList<>(mTimesLength);
        int offset = 0;
        for (int i = 0; i < mTimesLength; i++) {
            dataX.add(Double.toString((mTimes[i] - mRecordingStartTime) / 1000000000.0));
            entriesX.add(new Entry(mValues[offset++], i));
            entriesY.add(new Entry(mValues[offset++], i));
            entriesZ.add(new Entry(mValues[offset++], i));
        }

        mChartX.setData(new LineData(dataX, new LineDataSet(entriesX, null)));
        mChartY.setData(new LineData(dataX, new LineDataSet(entriesY, null)));
        mChartZ.setData(new LineData(dataX, new LineDataSet(entriesZ, null)));
        mChartX.invalidate();
        mChartY.invalidate();
        mChartZ.invalidate();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (mRecordingStartTime == 0) {
            mRecordingStartTime = event.timestamp;
        } else if (mTimesLength == 599) {
            // ignore
            return;
        }

        mTimes[mTimesLength++] = event.timestamp;
        mValues[mValuesLength++] = event.values[0];
        mValues[mValuesLength++] = event.values[1];
        mValues[mValuesLength++] = event.values[2];
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // no-op
    }
}
