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

package com.actinarium.kinetic.pipeline;

import android.hardware.SensorManager;
import android.opengl.Matrix;
import com.actinarium.kinetic.util.DataSet3;
import com.actinarium.kinetic.util.DataSet4;

/**
 * Utility class that performs various processing and transformation on provided sensor data, such as integrating
 * acceleration into velocity into offset, filtering out gravity, <s>reducing jitter</s> etc.
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
    public static void integrate(DataSet3 dataSet) {
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

    /**
     * Attempts to eliminate gravity bias from raw accelerometer recording. As of current version, it is not very
     * successful at its task
     *
     * @param accelDataIn   Input accelerometer data to filter
     * @param rotVectorData Rotation vector data, used to determine gravity vector
     * @param accelDataOut  Output data set. Can safely reuse input data set to overwrite data
     * @param gravity       Averaged gravity readings, accurate as of recording start
     */
    public static void removeGravityFromRaw(DataSet3 accelDataIn, DataSet4 rotVectorData, DataSet3 accelDataOut, float[] gravity) {
        // An array to hold the temporary rotation matrix for each measurement
        float[] matrix = new float[16];
        float[] transposed = new float[16];

        // An array to hold rotation vector (indices 0..3), then gravity vector (4..7) and resulting vector (8..11)
        float[] rv = new float[12];

        // Determine initial gravity vector
        rv[8] = gravity[0];
        rv[9] = gravity[1];
        rv[10] = gravity[2];
        rotVectorData.getForTime(0, rv);
        SensorManager.getRotationMatrixFromVector(matrix, rv);
        // Transposing a rotation matrix is the same as inverting one, but faster
        Matrix.transposeM(transposed, 0, matrix, 0);
        Matrix.multiplyMV(rv, 4, transposed, 0, rv, 8);

        // Now for each acceleration vector rotate it to match world coordinates
        rotVectorData.resetForInterpolatedRead();
        for (int i = 0; i < accelDataIn.length; i++) {
            rotVectorData.getForTime(accelDataIn.times[i], rv);
            SensorManager.getRotationMatrixFromVector(matrix, rv);
            Matrix.multiplyMV(rv, 8, matrix, 0, rv, 4);
            accelDataOut.valuesX[i] = accelDataIn.valuesX[i] - rv[8];
            accelDataOut.valuesY[i] = accelDataIn.valuesY[i] - rv[9];
            accelDataOut.valuesZ[i] = accelDataIn.valuesZ[i] - rv[10];
        }
    }
}
