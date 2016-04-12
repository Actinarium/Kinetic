package com.actinarium.kinetic.util;

import android.hardware.SensorEvent;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * A mutable sensor data set backed by four reusable arrays (timestamps and three value sets) of fixed lengths. Its
 * mutability is a trade-off aimed to reduce memory churn (allocations and GCs).
 */
public class DataSet3 implements Parcelable {

    public final long[] times;
    public final float[] valuesX;
    public final float[] valuesY;
    public final float[] valuesZ;
    /**
     * Number of fresh values in each of data set arrays, starting from index zero. Values with index equal or greater
     * than length are stale and should not be used.
     */
    public int length;
    protected final int mDataSize;

    /**
     * Used for faster interpolated lookup
     */
    protected int mSlidingIndex = 0;

    /**
     * Create a new data set for provided number of sensor events
     *
     * @param dataSize    The number of sensor events this data set will be able to contain at max
     */
    public DataSet3(int dataSize) {
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

    /**
     * Calculates the set of values for provided time. If time lands on exact measurement in the data set, it is
     * returned as is, otherwise this method will linearly interpolate between two closest points. If time is outside
     * the range, the first or the last value will be used. <b>Heads up:</b> this method is optimized to perform with
     * O(1) complexity for sequential lookups (i.e. when the next call to this method has <code>time</code> equal or
     * greater than previously provided time. Also you should use {@link #resetForInterpolatedRead()} each time before
     * reading the data from the start.
     *
     * @param time Timestamp in nanos to get value set for
     * @param out  Array to fill with values, must be at least of length 3
     */
    public void getForTime(long time, float[] out) {
        // Corner cases: before range start and after range end
        if (time <= times[0]) {
            out[0] = valuesX[0];
            out[1] = valuesY[0];
            out[2] = valuesZ[0];
            return;
        }
        if (time >= times[length - 1]) {
            out[0] = valuesX[length - 1];
            out[1] = valuesY[length - 1];
            out[2] = valuesZ[length - 1];
            return;
        }

        // Now since we know that times are always increasing, and this method will be probably called for sequential
        // times, we can try optimizing lookup to be close to O(1) by using a sliding index.
        while (times[mSlidingIndex + 1] < time) {
            mSlidingIndex++;
        }
        // Just in case the time requested was an earlier one
        while (times[mSlidingIndex] > time) {
            mSlidingIndex--;
        }

        // Now the sliding index should be the closest one which is <= time

        // Return linearly interpolated value between two data points
        float weight = (time - times[mSlidingIndex]) / (float) (times[mSlidingIndex + 1] - times[mSlidingIndex]);
        out[0] = valuesX[mSlidingIndex] + weight * (valuesX[mSlidingIndex + 1] - valuesX[mSlidingIndex]);
        out[1] = valuesY[mSlidingIndex] + weight * (valuesY[mSlidingIndex + 1] - valuesY[mSlidingIndex]);
        out[2] = valuesZ[mSlidingIndex] + weight * (valuesZ[mSlidingIndex + 1] - valuesZ[mSlidingIndex]);
    }

    public void resetForInterpolatedRead() {
        mSlidingIndex = 0;
    }

    // Parcelable stuff

    protected DataSet3(Parcel in) {
        length = in.readInt();
        times = in.createLongArray();
        valuesX = in.createFloatArray();
        valuesY = in.createFloatArray();
        valuesZ = in.createFloatArray();
        mDataSize = times.length;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(length);
        dest.writeLongArray(times);
        dest.writeFloatArray(valuesX);
        dest.writeFloatArray(valuesY);
        dest.writeFloatArray(valuesZ);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<DataSet3> CREATOR = new Creator<DataSet3>() {
        @Override
        public DataSet3 createFromParcel(Parcel in) {
            return new DataSet3(in);
        }

        @Override
        public DataSet3[] newArray(int size) {
            return new DataSet3[size];
        }
    };

}
