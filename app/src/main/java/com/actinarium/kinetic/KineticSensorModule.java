package com.actinarium.kinetic;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.annotation.NonNull;

/**
 * A module that encapsulates sensor reading logic and transforming it into the data Kinetic needs
 *
 * @author Paul Danyliuk
 */
public class KineticSensorModule {

    private Context mContext;

    private final SensorManager mManager;
    private final LinearMovementAdapter mLinearMovementAdapter;
    private final RotationAdapter mRotationAdapter;

    public KineticSensorModule(Context context) {
        mContext = context;

        // Initialize sensor listeners
        mLinearMovementAdapter = new LinearMovementAdapter();
        mRotationAdapter = new RotationAdapter();

        // Attach listeners to sensors
        mManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        final Sensor linearAccelSensor = mManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        final Sensor rotationVectorSensor = mManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mManager.registerListener(mLinearMovementAdapter, linearAccelSensor, SensorManager.SENSOR_DELAY_FASTEST);
        mManager.registerListener(mRotationAdapter, rotationVectorSensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    public void setKineticSensorListener(@NonNull KineticSensorListener listener) {
        mLinearMovementAdapter.setListener(listener);
    }

    public void flush() {
        mManager.unregisterListener(mLinearMovementAdapter);
        mManager.unregisterListener(mRotationAdapter);
    }

    public interface KineticSensorListener {
        void onMovementEvent(long timestamp, float x, float y, float z);
        void onRotationEvent(long timestamp, float x, float y, float z);
    }

    /**
     * Null object sensor listener
     */
    public static final KineticSensorListener NO_OP_SENSOR_LISTENER = new KineticSensorListener() {
        @Override
        public void onMovementEvent(long timestamp, float x, float y, float z) { /* no-op */ }
        @Override
        public void onRotationEvent(long timestamp, float x, float y, float z) { /* no-op */ }
    };

    /**
     * Common functionality for adapters that convert raw sensor data into significant measurements
     */
    private static abstract class BaseAdapter implements SensorEventListener {

        protected KineticSensorListener mListener;

        public BaseAdapter() {
            mListener = NO_OP_SENSOR_LISTENER;
        }

        public void setListener(@NonNull KineticSensorListener listener) {
            mListener = listener;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // no-op
        }
    }

    /**
     * An event adapter that translates device's linear acceleration to linear movement
     */
    private static class LinearMovementAdapter extends BaseAdapter {

        private long prevTimestamp = 0;
        private float prevX;
        private float prevY;
        private float prevZ;

        private static final float EPSILON = 0.2f;

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (prevTimestamp == 0) {
                prevTimestamp = event.timestamp;
                return;
            }

            final long dt = event.timestamp - prevTimestamp;
            final float dtSqrDivByTwo = dt * dt / 2000000f / 1000000000f;
            if (Math.abs(event.values[0]) > EPSILON) {
                prevX += event.values[0] * dtSqrDivByTwo;
            }
            if (Math.abs(event.values[1]) > EPSILON) {
                prevY += event.values[1] * dtSqrDivByTwo;
            }
            if (Math.abs(event.values[2]) > EPSILON) {
                prevZ += event.values[2] * dtSqrDivByTwo;
            }
            prevTimestamp = event.timestamp;
            mListener.onMovementEvent(prevTimestamp, prevX, prevY, prevZ);
        }
    }

    /**
     * An event adapter that translates device's rotation
     */
    private static class RotationAdapter extends BaseAdapter {

        private long prevTimestamp;
        private float prevX;
        private float prevY;
        private float prevZ;

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (prevTimestamp == 0) {
                prevTimestamp = event.timestamp;
                return;
            }

            final long dt = event.timestamp - prevTimestamp;
            final long dtSqrDivByTwo = dt * dt / 2;
            prevX += event.values[0] * dtSqrDivByTwo;
            prevY += event.values[1] * dtSqrDivByTwo;
            prevZ += event.values[2] * dtSqrDivByTwo;
            prevTimestamp = event.timestamp;
            mListener.onMovementEvent(prevTimestamp, prevX, prevY, prevZ);
        }
    }

}
