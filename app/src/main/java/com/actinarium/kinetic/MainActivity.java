package com.actinarium.kinetic;

import android.os.Bundle;
import android.support.v4.app.ShareCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import com.actinarium.kinetic.pipeline.CodeGenerator;
import com.actinarium.kinetic.pipeline.DataRecorder;
import com.actinarium.kinetic.pipeline.DataTransformer;
import com.actinarium.kinetic.util.DataSet;

public class MainActivity extends AppCompatActivity implements DataRecorder.Callback {

    private KineticChart mChartX;
    private KineticChart mChartY;
    private KineticChart mChartZ;
    private KineticChart mChartRotX;
    private KineticChart mChartRotY;
    private KineticChart mChartRotZ;
    private Button mRecordButton;

    private DataRecorder mRecorder;
    private DataSet mAccelData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRecordButton = (Button) findViewById(R.id.record);
        mChartX = (KineticChart) findViewById(R.id.chart_x);
        mChartY = (KineticChart) findViewById(R.id.chart_y);
        mChartZ = (KineticChart) findViewById(R.id.chart_z);
        mChartRotX = (KineticChart) findViewById(R.id.chart_x_rot);
        mChartRotY = (KineticChart) findViewById(R.id.chart_y_rot);
        mChartRotZ = (KineticChart) findViewById(R.id.chart_z_rot);

        mRecorder = new DataRecorder(this, this, 2000, DataRecorder.DEFAULT_SAMPLING_MICROS);

        mRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRecordButton.setEnabled(false);
                mRecorder.start();
            }
        });

        Button saveButton = (Button) findViewById(R.id.save);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mAccelData == null) {
                    return;
                }

                String javaCode = CodeGenerator.generateInterpolatorCode("com.example.kinetic", "FantasticInterpolator", mAccelData.valuesX, mAccelData.length);
                ShareCompat.IntentBuilder.from(MainActivity.this)
                        .setType("text/plain")
                        .setText(javaCode)
                        .startChooser();
            }
        });
    }

    @Override
    public void onDataRecordedResult(@DataRecorder.Status int status, DataSet accelData,
                                     DataSet gyroData, float[] initialOrientation) {
        mAccelData = accelData;

        // acceleration -> velocity
        DataTransformer.integrate(accelData);
        // velocity -> offset
        DataTransformer.integrate(accelData);

//         angular velocity -> phase
        DataTransformer.integrate(gyroData);

        mRecordButton.setEnabled(true);

        plot(accelData.times, accelData.valuesX, accelData.length, mChartX, false);
        plot(accelData.times, accelData.valuesY, accelData.length, mChartY, false);
        plot(accelData.times, accelData.valuesZ, accelData.length, mChartZ, false);
        plot(gyroData.times, gyroData.valuesX, gyroData.length, mChartRotX, true);
        plot(gyroData.times, gyroData.valuesY, gyroData.length, mChartRotY, true);
        plot(gyroData.times, gyroData.valuesZ, gyroData.length, mChartRotZ, true);
    }

    private void plot(long[] times, float[] values, int length, KineticChart chart, boolean isRotation) {
        float min = isRotation ? (float) (-Math.PI / 2) : -0.1f;
        float max = isRotation ? (float) (Math.PI / 2) : 0.1f;
        for (int i = 1; i < length; i++) {
            if (values[i] < min) {
                min = values[i];
            } else if (values[i] > max) {
                max = values[i];
            }
        }

        chart.setData(times, values, length, min, max, 0, 0);
    }
}
