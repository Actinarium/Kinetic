package com.actinarium.kinetic.util;

/**
 * An utility interpolator that determines interpolated value from a lookup table with values that aren't necessarily
 * recorded at equal intervals. Optimized for lookups where next looked up time is >= previous one.
 *
 * @author Paul Danyliuk
 *
 * todo: since this was moved to DataSet classes, maybe this interpolator is now obsolete
 */
public class NonUniformDataInterpolator {

    private final float[] mValues;
    private final long[] mTimes;
    private final int mLength;

    private int mSlidingIndex = 0;

    public NonUniformDataInterpolator(float[] values, long[] times, int length) {
        mValues = values;
        mTimes = times;
        mLength = length;
    }

    public float getValueForTime(long time) {
        // Corner cases: before range start and after range end
        if (time <= mTimes[0]) {
            return mValues[0];
        }
        if (time >= mTimes[mLength - 1]) {
            return mValues[mLength - 1];
        }

        // Now since we know that times are always increasing, and this method will be probably called for sequential
        // times, we can try optimizing lookup to be close to O(1) by using a sliding index.
        while (mTimes[mSlidingIndex + 1] < time) {
            mSlidingIndex++;
        }
        // Just in case the time requested was an earlier one
        while (mTimes[mSlidingIndex] > time) {
            mSlidingIndex--;
        }

        // Now the sliding index should be the closest one which is <= time

        // Return linearly interpolated value between two data points
        float weight = (time - mTimes[mSlidingIndex]) / (float) (mTimes[mSlidingIndex + 1] - mTimes[mSlidingIndex]);
        return mValues[mSlidingIndex] + weight * (mValues[mSlidingIndex + 1] - mValues[mSlidingIndex]);
    }

    /**
     * Prepares for reading the values sequentially from start, resetting the sliding index to 0
     */
    public void prepareForReading() {
        mSlidingIndex = 0;
    }
}
