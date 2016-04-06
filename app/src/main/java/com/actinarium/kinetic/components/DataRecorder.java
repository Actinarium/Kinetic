package com.actinarium.kinetic.components;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Records and holds raw sensor data
 *
 * @author Paul Danyliuk
 */
public class DataRecorder {

    public static final int DEFAULT_RECORDING_TIME_MILLIS = 2000;
    public static final int DEFAULT_SAMPLING_MICROS = 5000;

    public static final int STATUS_FAILURE_GENERIC = -1;
    public static final int STATUS_DONE = 0;
    public static final int STATUS_TERMINATED = 1;
    public static final int STATUS_OUT_OF_BOUNDS = 2;

    private SensorManager mSensorManager;
    private Sensor mAccelSensor;
    private Sensor mGyroSensor;

    private Callback mCallback;
    private int mRecordingTimeMillis;
    private int mSamplingRateMicros;

    private final float[] mInitialOrientation = {0f, 0f, 1f};
    private DataSet mAccelDataSet;
    private DataSet mGyroDataSet;

    private SensorEventListener mAccelSensorListener;
    private SensorEventListener mGyroSensorListener;
    private Handler mHandler;
    private Runnable mRunnable;

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

        mCallback = callback;
        mRecordingTimeMillis = recordingTimeMillis;
        mSamplingRateMicros = samplingRateMicros;

        // Based on latency and recording times, how many values we ought to capture, with 20% safety overhead
        final int dataSize = recordingTimeMillis * 1200 / samplingRateMicros + 1;

        mAccelDataSet = new DataSet(dataSize);
        mGyroDataSet = new DataSet(dataSize);

        // Create listeners
        mAccelSensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (!mAccelDataSet.put(event)) {
                    doStop(STATUS_OUT_OF_BOUNDS);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) { /* no-op */ }
        };
        mGyroSensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (!mGyroDataSet.put(event)) {
                    doStop(STATUS_OUT_OF_BOUNDS);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) { /* no-op */ }
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

        // todo: determine initial rotation around X and Y axes (we don't care which direction the device is pointing)

        // Register the listeners
        // todo: implement delayed reporting (with H/W queue) for KitKat+
        mSensorManager.registerListener(mAccelSensorListener, mAccelSensor, mSamplingRateMicros);
        mSensorManager.registerListener(mGyroSensorListener, mGyroSensor, mSamplingRateMicros);

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

        mCallback.onDataRecordedResult(status, mAccelDataSet, mGyroDataSet, mInitialOrientation);
    }

    /**
     * Data recorder callback. Used to deliver recording result to the host (activity) for further processing.
     */
    public interface Callback {

        /**
         * Called when data recorder status is determined. All arguments are mutable and reusable, so you should neither
         * change nor hold onto them.
         *
         * @param status             Reported status
         * @param accelData          Holds recorded data for accelerometer values
         * @param gyroData           Holds recorded data for gyroscope values
         * @param initialOrientation Holds information regarding initial device orientation
         */
        void onDataRecordedResult(@Status int status, DataSet accelData, DataSet gyroData, float[] initialOrientation);
    }

    /**
     * A mutable sensor data set backed by two static reusable arrays (values and timestamps) of fixed lengths
     */
    public static class DataSet {
        public float[] values;
        public long[] times;
        public int valuesLength;
        public int timesLength;

        /**
         * Create a new data set for provided number of sensor events
         *
         * @param dataSize The number of sensor events this data set will be able to contain at max
         */
        private DataSet(int dataSize) {
            // Allocate arrays for values and timestamps. We're going to reuse these arrays to avoid memory churn
            // We're going to use a single-dimensional array to store values for three coordinates
            values = new float[dataSize * 3];
            times = new long[dataSize];
        }

        /**
         * Resets data end pointer to zero
         */
        public void reset() {
            valuesLength = 0;
            timesLength = 0;
        }

        /**
         * Appends event data (timestamp and 3 values) to this data set
         *
         * @param event Sensor event to take data from
         * @return true if data was added, false if array is overflowing
         */
        public boolean put(SensorEvent event) {
            // Check if we're not overflowing allocated arrays
            if (timesLength == times.length) {
                return false;
            }

            // If everything is OK, save the data
            times[timesLength++] = event.timestamp;
            values[valuesLength++] = event.values[0];
            values[valuesLength++] = event.values[1];
            values[valuesLength++] = event.values[2];

            return true;
        }
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
