package com.actinarium.kinetic.util;

/**
 * An utility interpolator that determines interpolated value from a lookup table with values that aren't necessarily
 * recorded at equal intervals. Optimized for lookups where next looked up time is >= previous one.
 *
 * @author Paul Danyliuk
 */
public class NonUniformDataInterpolator {

    private final float[] mValues;
    private final long[] mTimes;

    public NonUniformDataInterpolator(float[] values, long[] times) {
        mValues = values;
        mTimes = times;
    }
}
