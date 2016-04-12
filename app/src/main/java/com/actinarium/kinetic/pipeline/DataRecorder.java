package com.actinarium.kinetic.pipeline;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.support.annotation.IntDef;
import android.util.Log;
import com.actinarium.kinetic.util.DataSet3;
import com.actinarium.kinetic.util.DataSet4;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Records and holds raw sensor data
 *
 * @author Paul Danyliuk
 */
public class DataRecorder {

    private static final String TAG = "DataRecorder";

    public static final int DEFAULT_RECORDING_TIME_MILLIS = 5000;
    public static final int DEFAULT_SAMPLING_MICROS = 5000;

    public static final int STATUS_FAILURE_GENERIC = -1;
    public static final int STATUS_DONE = 0;
    public static final int STATUS_TERMINATED = 1;
    public static final int STATUS_OUT_OF_BOUNDS = 2;

    private SensorManager mSensorManager;
    private Sensor mAccelSensor;
    private Sensor mGyroSensor;
    private Sensor mRotVectorSensor;

    private DataSet3 mAccelDataSet;
    private DataSet3 mGyroDataSet;
    private DataSet4 mRotVectorDataSet;

    private SensorEventListener mAccelSensorListener;
    private SensorEventListener mGyroSensorListener;
    private SensorEventListener mRotVectorSensorListener;

    private Handler mHandler;
    private Runnable mRunnable;

    private Callback mCallback;
    private int mRecordingTimeMillis;
    private int mSamplingRateMicros;

    /**
     * Create and initialize a data recorder component.
     *
     * @param context             Context, used to look up sensor manager service
     * @param callback            Callback to report data recording status to
     * @param recordingTimeMillis Time to record sensor values in millis, e.g. {@link #DEFAULT_RECORDING_TIME_MILLIS}
     * @param samplingRateMicros  Sensor sampling rate in micros, e.g. {@link #DEFAULT_SAMPLING_MICROS}
     */
    public DataRecorder(Context context, Callback callback, int recordingTimeMillis, int samplingRateMicros) {
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mAccelSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mRotVectorSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        mCallback = callback;
        mRecordingTimeMillis = recordingTimeMillis;
        mSamplingRateMicros = samplingRateMicros;

        // Based on latency and recording times, how many values we ought to capture, with 20% safety overhead
        final int dataSize = recordingTimeMillis * 1200 / samplingRateMicros + 1;

        mAccelDataSet = new DataSet3(dataSize);
        mGyroDataSet = new DataSet3(dataSize);
        mRotVectorDataSet = new DataSet4(dataSize);

        // Create listeners
        mAccelSensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (!mAccelDataSet.put(event)) {
                    doStop(STATUS_OUT_OF_BOUNDS);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) { Log.d(TAG, "Accel accuracy: " + accuracy); }
        };
        mGyroSensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (!mGyroDataSet.put(event)) {
                    doStop(STATUS_OUT_OF_BOUNDS);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) { Log.d(TAG, "Gyro accuracy: " + accuracy); }
        };
        mRotVectorSensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (!mRotVectorDataSet.put(event)) {
                    doStop(STATUS_OUT_OF_BOUNDS);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) { Log.d(TAG, "Rotation vector accuracy: " + accuracy); }
        };
    }

    /**
     * Start recording data. Starts listening on sensors and writing data to respective data sets
     */
    public void start() {
        // If started, throw exception
        if (mHandler != null || mRunnable != null) {
            throw new IllegalStateException("Cannot start data recorder - it appears to be started already");
        }

        mAccelDataSet.reset();
        mGyroDataSet.reset();
        mRotVectorDataSet.reset();

        // todo: determine initial rotation around X and Y axes (we don't care which direction the device is pointing)

        // Register the listeners
        // todo: implement delayed reporting (with H/W queue) for KitKat+
        mSensorManager.registerListener(mAccelSensorListener, mAccelSensor, mSamplingRateMicros);
        mSensorManager.registerListener(mGyroSensorListener, mGyroSensor, mSamplingRateMicros);
        mSensorManager.registerListener(mRotVectorSensorListener, mRotVectorSensor, mSamplingRateMicros);

        // Register a handler and a runnable to stop listening after the timeout
        mHandler = new Handler();
        mRunnable = new StopListeningRunnable();
        mHandler.postDelayed(mRunnable, mRecordingTimeMillis);
    }

    /**
     * Force stop recording data
     */
    public void stop() {
        doStop(STATUS_TERMINATED);
    }

    /**
     * Called internally when data recording should be terminated for any reason
     */
    private void doStop(@Status int status) {
        if (mHandler != null && mRunnable != null) {
            mHandler.removeCallbacks(mRunnable);
            mHandler = null;
            mRunnable = null;
        }
        mSensorManager.unregisterListener(mAccelSensorListener);
        mSensorManager.unregisterListener(mGyroSensorListener);
        mSensorManager.unregisterListener(mRotVectorSensorListener);

        mCallback.onDataRecordedResult(status, mAccelDataSet, mGyroDataSet, mRotVectorDataSet);
    }

    /**
     * Data recorder callback. Used to deliver recording result to the host (activity) for further processing.
     */
    public interface Callback {

        /**
         * Called when data recorder status is determined. All arguments are mutable and reusable, so you should neither
         * change nor hold onto them.
         *
         * @param status        Reported status
         * @param accelData     Holds recorded data for accelerometer values
         * @param gyroData      Holds recorded data for gyroscope values
         * @param rotVectorData Holds recorded data for rotation vector values
         */
        void onDataRecordedResult(@Status int status, DataSet3 accelData, DataSet3 gyroData, DataSet4 rotVectorData);
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({STATUS_DONE, STATUS_TERMINATED, STATUS_OUT_OF_BOUNDS, STATUS_FAILURE_GENERIC})
    public @interface Status {
    }

    private class StopListeningRunnable implements Runnable {
        @Override
        public void run() {
            DataRecorder.this.doStop(STATUS_DONE);
        }
    }
}
