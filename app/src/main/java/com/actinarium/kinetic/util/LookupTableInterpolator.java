/*
 * Copyright (C) 2016 Actinarium
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.actinarium.kinetic.util;

import android.view.animation.Interpolator;
import com.actinarium.kinetic.pipeline.CodeGenerator;

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
    private float mValueAdd = 0f;
    private float mValueMult = 1f;
    private int mStart;

    /**
     * Create a new table lookup interpolator for arbitrary sensor data
     */
    public LookupTableInterpolator() {
    }

    /**
     * Set lookup table data
     *
     * @param values Raw values for the lookup table, recorded at fixed time step
     */
    public void setData(float[] values) {
        mValues = values;
    }

    /**
     * Set lookup table range
     *
     * @param start Index of the first value of the array to use, inclusively
     * @param end   Index of the last value of the array to use, inclusively
     */
    public void setRange(int start, int end) {
        mStart = start;
        mLengthMinusOne = end - start - 2;
        mStepSize = 1f / mLengthMinusOne;
    }

    /**
     * Set extra and a multiplier to adjust how raw table data maps to 0f-1f range and beyond
     *
     * @param valueAdd  Extra, added after multiplication
     * @param valueMult Multiplier, used to pre-multiply values
     */
    public void setTransformation(float valueAdd, float valueMult) {
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
        float value = mValues[index + mStart];
        return mValueAdd + (value + weight * (mValues[index + mStart + 1] - value)) * mValueMult;
    }

    /**
     * Exports data from selected range, applying extra and multiplier to all copied values
     *
     * @return A new array of normalized values from selected range, ready to pass to {@link
     * CodeGenerator#generateInterpolatorCode(String, String, float[])}
     */
    public float[] exportData() {
        final int exportLength = mLengthMinusOne + 1;
        float[] result = new float[exportLength];
        for (int i = 0; i < exportLength; i++) {
            result[i] = mValueAdd + mValues[mStart + i] * mValueMult;
        }
        return result;
    }
}
