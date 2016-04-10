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

    private final float[] mValues;
    private final float mStepSize;
    private final int mLengthMinusOne;

    /**
     * Create a new interpolator for a given set of values
     *
     * @param values Values for the lookup table, recorded at fixed time step
     * @param length Number of values to pick from the provided array
     */
    public LookupTableInterpolator(float[] values, int length) {
        mValues = values;
        mLengthMinusOne = length - 1;
        mStepSize = 1f / mLengthMinusOne;
    }

    @Override
    public float getInterpolation(float input) {
        if (input >= 1.0f) {
            return 1.0f;
        }
        if (input <= 0f) {
            return 0f;
        }

        // Calculate left cell index (length - 2 at max)
        int index = Math.min((int) (input * mLengthMinusOne), mLengthMinusOne - 1);

        // Calculate values to account for small offsets as the lookup table has discrete values
        float quantized = index * mStepSize;
        float diff = input - quantized;
        float weight = diff / mStepSize;

        // Linearly interpolate between the table values
        float valueLeft = mValues[index];
        return valueLeft + weight * (mValues[index + 1] - valueLeft);
    }
}
