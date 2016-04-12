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

    public static final int STATUS_FAILURE_NO_SENSOR = -2;
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
    private boolean mIsRecording;

    private Callback mCallback;
    private int mRecordingTimeMillis;
    private int mSamplingRateMicros;

    private float[] mGravity = new float[3];
    private static final float ALPHA = 0.8f;

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

        if (mAccelSensor == null || mGyroSensor == null || mRotVectorSensor == null) {
            callback.onDataRecordedResult(STATUS_FAILURE_NO_SENSOR, null, null, null, null);
            return;
        }

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
                if (!mIsRecording) {
                    // While the listener is idle, let's adjust gravity
                    adjustGravity(event);
                }
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
                if (!mIsRecording) {
                    return;
                }
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
                if (!mIsRecording) {
                    return;
                }
                if (!mRotVectorDataSet.put(event)) {
                    doStop(STATUS_OUT_OF_BOUNDS);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) { Log.d(TAG, "Rotation vector accuracy: " + accuracy); }
        };
    }

    /**
     * Starts listening on sensors. Call this for warm-up. Will not start recording data - call {@link
     * #startRecording()} for that
     */
    public void startListening() {
        // todo: enable reporting latency and flush
        mSensorManager.registerListener(mAccelSensorListener, mAccelSensor, mSamplingRateMicros);
        mSensorManager.registerListener(mGyroSensorListener, mGyroSensor, mSamplingRateMicros);
        mSensorManager.registerListener(mRotVectorSensorListener, mRotVectorSensor, mSamplingRateMicros);
    }

    /**
     * Start recording data
     */
    public void startRecording() {
        // If started, throw exception
        if (mHandler != null || mRunnable != null) {
            throw new IllegalStateException("Cannot start data recorder - it appears to be started already");
        }

        mAccelDataSet.reset();
        mGyroDataSet.reset();
        mRotVectorDataSet.reset();

        mIsRecording = true;

        // Register a handler and a runnable to stop listening after the timeout
        mHandler = new Handler();
        mRunnable = new StopListeningRunnable();
        mHandler.postDelayed(mRunnable, mRecordingTimeMillis);
    }

    /**
     * Force stop recording data and unregister the listener
     */
    public void stop() {
        doStop(STATUS_TERMINATED);
    }

    /**
     * Called internally when data recording should be terminated for any reason
     */
    private void doStop(@Status int status) {
        if (!mIsRecording) {
            return;
        }

        mIsRecording = false;

        if (mHandler != null && mRunnable != null) {
            mHandler.removeCallbacks(mRunnable);
            mHandler = null;
            mRunnable = null;
        }

        mSensorManager.unregisterListener(mAccelSensorListener);
        mSensorManager.unregisterListener(mGyroSensorListener);
        mSensorManager.unregisterListener(mRotVectorSensorListener);

        mCallback.onDataRecordedResult(status, mAccelDataSet, mGyroDataSet, mRotVectorDataSet, mGravity);
    }

    private void adjustGravity(SensorEvent event) {
        mGravity[0] = ALPHA * mGravity[0] + (1 - ALPHA) * event.values[0];
        mGravity[1] = ALPHA * mGravity[1] + (1 - ALPHA) * event.values[1];
        mGravity[2] = ALPHA * mGravity[2] + (1 - ALPHA) * event.values[2];
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
         * @param gravity       Initial gravity as of the start of recording
         */
        void onDataRecordedResult(@Status int status, DataSet3 accelData, DataSet3 gyroData, DataSet4 rotVectorData, float[] gravity);
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({STATUS_DONE, STATUS_TERMINATED, STATUS_OUT_OF_BOUNDS, STATUS_FAILURE_GENERIC, STATUS_FAILURE_NO_SENSOR})
    public @interface Status {
    }

    private class StopListeningRunnable implements Runnable {
        @Override
        public void run() {
            DataRecorder.this.doStop(STATUS_DONE);
        }
    }
}
