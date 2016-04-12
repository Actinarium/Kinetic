package com.actinarium.kinetic.util;

import android.hardware.SensorEvent;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * A mutable sensor data set backed by four reusable arrays (timestamps and four value sets) of fixed lengths. Its
 * mutability is a trade-off aimed to reduce memory churn (allocations and GCs).
 */
public class DataSet4 extends DataSet3 {

    public final float[] values4;

    /**
     * Create a new data set for provided number of sensor events
     *
     * @param dataSize    The number of sensor events this data set will be able to contain at max
     * @param hasFourSets Whether this data set should remember {@link SensorEvent#values SensorEvent.values[3]}
     */
    public DataSet4(int dataSize) {
        super(dataSize);
        values4 = new float[dataSize];
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
        if (values4 != null) {
            values4[length] = event.values[3];
        }
        length++;
        return true;
    }

    /**
     * @inheritDoc
     *
     * @param out Array to fill with values, must be at least of length 4
     */
    @Override
    public void getForTime(long time, float[] out) {
        // Corner cases: before range start and after range end
        if (time <= times[0]) {
            out[0] = valuesX[0];
            out[1] = valuesY[0];
            out[2] = valuesZ[0];
            out[3] = values4[0];
            return;
        }
        if (time >= times[length - 1]) {
            out[0] = valuesX[length - 1];
            out[1] = valuesY[length - 1];
            out[2] = valuesZ[length - 1];
            out[3] = values4[length - 1];
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
        out[3] = values4[mSlidingIndex] + weight * (values4[mSlidingIndex + 1] - values4[mSlidingIndex]);
    }

    // Parcelable stuff

    protected DataSet4(Parcel in) {
        super(in);
        values4 = in.createFloatArray();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeFloatArray(values4);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<DataSet4> CREATOR = new Creator<DataSet4>() {
        @Override
        public DataSet4 createFromParcel(Parcel in) {
            return new DataSet4(in);
        }

        @Override
        public DataSet4[] newArray(int size) {
            return new DataSet4[size];
        }
    };

}
