package com.actinarium.kinetic.pipeline;

import com.actinarium.kinetic.util.DataSet;

/**
 * Utility class that performs various processing and transformation on provided sensor data, such as double-integrating
 * acceleration into offsets, filtering out gravity, reducing jitter etc.
 *
 * @author Paul Danyliuk
 */
public final class DataTransformer {

    private static final long NANOS_IN_SECONDS = 1000000000;

    /**
     * Private constructor, to prevent instantiation
     */
    private DataTransformer() {}

    /**
     * Integrates the data set, replacing existing data with integrated values and assuming starting values are 0.
     * Used to calculate velocity from acceleration and then once again to get offsets from velocity
     *
     * todo: write to another data set instead of all this variable swapping
     *
     * @param dataSet The data set containing acceleration vectors, will be mutated to contain calculation result.
     */
    public static void integrate(DataSet dataSet) {
        // Remember previous acceleration, because we're going to overwrite values in the data set itself
        float ax = dataSet.valuesX[0];
        float ay = dataSet.valuesY[0];
        float az = dataSet.valuesZ[0];
        float vx, vy, vz;

        // Assume the device is at rest when we start recording. There's no way to determine initial velocity anyways
        dataSet.valuesX[0] = 0;
        dataSet.valuesY[0] = 0;
        dataSet.valuesZ[0] = 0;

        // Calculate the area under the data set function, assuming dt is small enough and a(x) is linear between points
        for (int i = 1; i < dataSet.length; i++) {
            // dv = a(t) * dt; v = v0 + (a + a0)(t - t0)/2
            vx = dataSet.valuesX[i - 1] + (ax + dataSet.valuesX[i]) * (dataSet.times[i] - dataSet.times[i - 1]) / 2 / NANOS_IN_SECONDS;
            vy = dataSet.valuesY[i - 1] + (ay + dataSet.valuesY[i]) * (dataSet.times[i] - dataSet.times[i - 1]) / 2 / NANOS_IN_SECONDS;
            vz = dataSet.valuesZ[i - 1] + (az + dataSet.valuesZ[i]) * (dataSet.times[i] - dataSet.times[i - 1]) / 2 / NANOS_IN_SECONDS;
            ax = dataSet.valuesX[i];
            ay = dataSet.valuesY[i];
            az = dataSet.valuesZ[i];
            dataSet.valuesX[i] = vx;
            dataSet.valuesY[i] = vy;
            dataSet.valuesZ[i] = vz;
        }
    }

}
