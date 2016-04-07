package com.actinarium.kinetic.util;

import android.hardware.SensorEvent;

/**
 * A mutable sensor data set backed by four reusable arrays (timestamps and three value sets) of fixed lengths
 */
public class DataSet {

    public long[] times;
    public float[] valuesX;
    public float[] valuesY;
    public float[] valuesZ;
    /**
     * Number of fresh values in each of data set arrays, starting from index zero. Values with index equal or greater
     * than length are stale and should not be used.
     */
    public int length;
    private int mDataSize;

    /**
     * Create a new data set for provided number of sensor events
     *
     * @param dataSize The number of sensor events this data set will be able to contain at max
     */
    public DataSet(int dataSize) {
        mDataSize = dataSize;

        // Allocate arrays for values and timestamps. We're going to reuse these arrays to avoid memory churn
        // We're going to use three separate single-dimensional arrays to store values for three coordinates
        valuesX = new float[dataSize];
        valuesY = new float[dataSize];
        valuesZ = new float[dataSize];
        times = new long[dataSize];
    }

    /**
     * Resets data end pointer to zero
     */
    public void reset() {
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
        if (length == mDataSize) {
            return false;
        }

        // If everything is OK, save the data
        times[length] = event.timestamp;
        valuesX[length] = event.values[0];
        valuesY[length] = event.values[1];
        valuesZ[length] = event.values[2];
        length++;
        return true;
    }
}
