package com.actinarium.kinetic.util;

import android.hardware.SensorEvent;
import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A mutable sensor data set backed by two static reusable arrays (values and timestamps) of fixed lengths
 */
public class DataSet {

    /**
     * Number of values per stride
     */
    public static final int STRIDE = 3;

    public static final int OFFSET_X = 0;
    public static final int OFFSET_Y = 1;
    public static final int OFFSET_Z = 2;

    public float[] values;
    public long[] times;
    public int length;
    private int valuesLength;

    /**
     * Create a new data set for provided number of sensor events
     *
     * @param dataSize The number of sensor events this data set will be able to contain at max
     */
    public DataSet(int dataSize) {
        // Allocate arrays for values and timestamps. We're going to reuse these arrays to avoid memory churn
        // We're going to use a single-dimensional array to store values for three coordinates
        values = new float[dataSize * STRIDE];
        times = new long[dataSize];
    }

    /**
     * Resets data end pointer to zero
     */
    public void reset() {
        valuesLength = 0;
        length = 0;
    }

    /**
     * Appends event data (timestamp and 3 values) to this data set
     *
     * @param event Sensor event to take data from
     * @return true if data was added, false if array is overflowing
     */
    public boolean put(SensorEvent event) {
        // Check if we're not overflowing allocated arrays
        if (length == times.length) {
            return false;
        }

        // If everything is OK, save the data
        times[length++] = event.timestamp;
        values[valuesLength++] = event.values[OFFSET_X];
        values[valuesLength++] = event.values[OFFSET_Y];
        values[valuesLength++] = event.values[OFFSET_Z];
        return true;
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({OFFSET_X, OFFSET_Y, OFFSET_Z})
    public @interface Offset {}
}
