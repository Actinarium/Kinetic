package com.actinarium.kinetic.util;

import android.view.animation.Interpolator;

/**
 * <p>An interpolator that uses a lookup table to compute interpolation.</p><p><b>Note:</b> it is assumed that values
 * have fixed time step, so you must ensure it beforehand.</p><p>Derives from Apache 2.0 licensed code from Android
 * Support v4 Library, specifically {@link android.support.v4.view.animation.LookupTableInterpolator
 * LookupTableInterpolator}</p></p>
 *
 * @author Paul Danyliuk
 */
public class LookupTableInterpolator implements Interpolator {

    private float[] mValues;
    private float mStepSize;
    private int mLengthMinusOne;
    private float mValueAdd;
    private float mValueMult;

    /**
     * Create a new interpolator for a given set of values
     *
     * @param values   Raw values for the lookup table, recorded at fixed time step
     * @param start    Index of the first value of the array to use, inclusively
     * @param end      Index of the last value of the array to use, inclusively
     * @param valueAdd Used to convert
     */
    public LookupTableInterpolator(float[] values, int start, int end, float valueAdd, float valueMult) {
        mValues = values;
        mLengthMinusOne = end - start - 2;
        mStepSize = 1f / mLengthMinusOne;
        mValueAdd = valueAdd;
        mValueMult = valueMult;
    }

//    public void setData(float[] values, int length, float valueAdd, float valueMult)

    @Override
    public float getInterpolation(float input) {
        if (input > 1.0f) {
            input = 1.0f;
        } else if (input < 0f) {
            input = 0f;
        }

        // Calculate left cell index (length - 2 at max)
        int index = Math.min((int) (input * mLengthMinusOne), mLengthMinusOne - 1);

        // Calculate values to account for small offsets as the lookup table has discrete values
        float quantized = index * mStepSize;
        float diff = input - quantized;
        float weight = diff / mStepSize;

        // Linearly interpolate between the table values
        float value = mValues[index];
        return mValueAdd + (value + weight * (mValues[index + 1] - value)) * mValueMult;
    }
}
