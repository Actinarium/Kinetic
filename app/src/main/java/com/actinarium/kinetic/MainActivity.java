package com.actinarium.kinetic;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import com.actinarium.kinetic.pipeline.DataRecorder;
import com.actinarium.kinetic.util.DataSet;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements DataRecorder.Callback {

    private LineChart mChartX;
    private LineChart mChartY;
    private LineChart mChartZ;
    private LineChart mChartRotX;
    private LineChart mChartRotY;
    private LineChart mChartRotZ;
    private Button mRecordButton;

    private DataRecorder mRecorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRecordButton = (Button) findViewById(R.id.record);
        mChartX = (LineChart) findViewById(R.id.chart_x);
        mChartY = (LineChart) findViewById(R.id.chart_y);
        mChartZ = (LineChart) findViewById(R.id.chart_z);
        mChartRotX = (LineChart) findViewById(R.id.chart_rot_x);
        mChartRotY = (LineChart) findViewById(R.id.chart_rot_y);
        mChartRotZ = (LineChart) findViewById(R.id.chart_rot_z);

        mRecorder = new DataRecorder(this, this, DataRecorder.DEFAULT_RECORDING_TIME_MILLIS, DataRecorder.DEFAULT_SAMPLING_MICROS);

        mRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRecordButton.setEnabled(false);
                mRecorder.start();
            }
        });
    }

    @Override
    public void onDataRecordedResult(@DataRecorder.Status int status, DataSet accelData,
                                     DataSet gyroData, float[] initialOrientation) {
        mRecordButton.setEnabled(true);
        plot(accelData, mChartX, mChartY, mChartZ);
        plot(gyroData, mChartRotX, mChartRotY, mChartRotZ);
    }

    private void plot(DataSet dataSet, LineChart chartX, LineChart chartY, LineChart chartZ) {
        int length = dataSet.length;
        long startTime = dataSet.times[0];

        ArrayList<Entry> entriesX = new ArrayList<>(length);
        ArrayList<Entry> entriesY = new ArrayList<>(length);
        ArrayList<Entry> entriesZ = new ArrayList<>(length);
        ArrayList<String> dataX = new ArrayList<>(length);

        for (int i = 0; i < length; i++) {
            dataX.add(Double.toString((dataSet.times[i] - startTime) / 1000000000.0));
            entriesX.add(new Entry(dataSet.valuesX[i], i));
            entriesY.add(new Entry(dataSet.valuesY[i], i));
            entriesZ.add(new Entry(dataSet.valuesZ[i], i));
        }

        chartX.setData(new LineData(dataX, new LineDataSet(entriesX, null)));
        chartY.setData(new LineData(dataX, new LineDataSet(entriesY, null)));
        chartZ.setData(new LineData(dataX, new LineDataSet(entriesZ, null)));
        chartX.invalidate();
        chartY.invalidate();
        chartZ.invalidate();
    }
}
