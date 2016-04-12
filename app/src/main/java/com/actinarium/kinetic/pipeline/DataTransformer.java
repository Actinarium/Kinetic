package com.actinarium.kinetic.pipeline;

import android.hardware.SensorManager;
import android.opengl.Matrix;
import com.actinarium.kinetic.util.DataSet3;
import com.actinarium.kinetic.util.DataSet4;

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

    public static void offsetYaw(DataSet3 dataSet) {
        float initialYaw = dataSet.valuesZ[0];
        for (int i = 0; i < dataSet.length; i++) {
            dataSet.valuesZ[i] -= initialYaw;
        }
    }

    public static void transformAccelToWorld(DataSet3 accelDataIn, DataSet4 rotVectorData, DataSet3 accelDataOut) {
        // Arrays to hold the temporary rotation and transposed matrices for each measurement
        float[] matrix = new float[16];
        float[] transposed = new float[16];

        // An array to hold rotation vector (indices 0..3), then actual vector and resulting vector (4..7)
        float[] rv = new float[8];
        rv[3] = 0f;

        // Now for each acceleration vector rotate it to match world coordinates
        rotVectorData.resetForInterpolatedRead();
        for (int i = 0; i < accelDataIn.length; i++) {
            rotVectorData.getForTime(accelDataIn.times[i], rv);
            SensorManager.getRotationMatrixFromVector(matrix, rv);
            rv[0] = accelDataIn.valuesX[i];
            rv[1] = accelDataIn.valuesY[i];
            rv[2] = accelDataIn.valuesZ[i];
            // Transposing rotation matrix also inverts it!
            Matrix.transposeM(transposed, 0, matrix, 0);
            Matrix.multiplyMV(rv, 4, transposed, 0, rv, 0);
            accelDataOut.valuesX[i] = rv[4];
            accelDataOut.valuesY[i] = rv[5];
            accelDataOut.valuesZ[i] = rv[6];
        }
    }

    /**
     * Subtracts gravity impact from transformed data
     *
     * @param accelDataIn  Acceleration data in world coordinates
     * @param accelDataOut Data set to write to; it's safe to pass the same data set as input to overwrite it
     */
    public static void removeGravity(DataSet3 accelDataIn, DataSet3 accelDataOut) {
        for (int i = 0; i < accelDataIn.length; i++) {
            accelDataOut.valuesZ[i] = accelDataIn.valuesZ[i] - SensorManager.STANDARD_GRAVITY;
        }
    }

    public static void removeGravityFromRaw(DataSet3 accelDataIn, DataSet4 rotVectorData, DataSet3 accelDataOut) {
        // Arrays to hold the temporary rotation and transposed matrices for each measurement
        float[] matrix = new float[16];

        // An array to hold rotation vector (indices 0..3), then gravity vector (4..7) and resulting vector (8..11)
        float[] rv = new float[12];
        rv[4] = 0f;
        rv[5] = 0f;
        rv[6] = -SensorManager.STANDARD_GRAVITY;

        // Now for each acceleration vector rotate it to match world coordinates
        rotVectorData.resetForInterpolatedRead();
        for (int i = 0; i < accelDataIn.length; i++) {
            rotVectorData.getForTime(accelDataIn.times[i], rv);
            SensorManager.getRotationMatrixFromVector(matrix, rv);
            Matrix.multiplyMV(rv, 8, matrix, 0, rv, 4);
            accelDataOut.valuesX[i] = accelDataIn.valuesX[i] + rv[8];
            accelDataOut.valuesY[i] = accelDataIn.valuesY[i] + rv[9];
            accelDataOut.valuesZ[i] = accelDataIn.valuesZ[i] + rv[10];
        }
    }

    public static void reduceJitter(DataSet3 accelDataIn, float threshold, DataSet3 accelDataOut) {
        for (int i = 0; i < accelDataIn.length; i++) {
            accelDataOut.valuesX[i] = Math.abs(accelDataIn.valuesX[i]) > threshold ? accelDataIn.valuesX[i] : 0f;
            accelDataOut.valuesY[i] = Math.abs(accelDataIn.valuesY[i]) > threshold ? accelDataIn.valuesY[i] : 0f;
            accelDataOut.valuesZ[i] = Math.abs(accelDataIn.valuesZ[i]) > threshold ? accelDataIn.valuesZ[i] : 0f;
        }
    }
}
