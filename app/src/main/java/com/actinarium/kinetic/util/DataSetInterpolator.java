package com.actinarium.kinetic.util;

import android.view.animation.Interpolator;

/**
 * <p>An interpolator that uses values from a {@link DataSet} object as lookup table. Since {@link DataSet#values} array
 * contains strides of interleaved data sets, this interpolator will traverse the data set with step of {@link
 * DataSet#STRIDE} starting from specified offset.</p><p><b>Note:</b> it is assumed that values have fixed time
 * step, so you must ensure it beforehand.</p>
 *
 * @author Paul Danyliuk
 */
public class DataSetInterpolator implements Interpolator {

    private final DataSet mDataSet;
    @DataSet.Offset
    private final int mOffset;
    private final float mStepSize;
    private final int mLengthMinusOne;

    /**
     * Create a new interpolator for a given data set and tell which values are to use
     *
     * @param dataSet Reusable data set. Warning: since the data set is mutable for perf reasons, it's your
     *                responsibility to ensure that this interpolator instance is disposed of as soon as the data set is
     *                stale
     * @param offset  Since the data set holds strides of data, offset specifies which values should be used. Possible
     *                values are {@link DataSet#OFFSET_X}, {@link DataSet#OFFSET_Y}, and {@link DataSet#OFFSET_Z}.
     */
    public DataSetInterpolator(DataSet dataSet, @DataSet.Offset int offset) {
        mDataSet = dataSet;
        mOffset = offset;
        mLengthMinusOne = mDataSet.length - 1;
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

        // Get appropriate values from the data set...
        int realIndexLeft = index * DataSet.STRIDE + mOffset;
        float valueLeft = mDataSet.values[realIndexLeft];
        float valueRight = mDataSet.values[realIndexLeft + DataSet.STRIDE];

        // ...and linearly interpolate between them
        return valueLeft + weight * (valueRight - valueLeft);
    }
}
