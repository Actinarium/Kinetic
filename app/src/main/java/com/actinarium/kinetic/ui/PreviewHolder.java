package com.actinarium.kinetic.ui;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Build;
import android.view.View;
import android.view.animation.Interpolator;
import com.actinarium.kinetic.util.DataSet3;

/**
 * A presenter for preview animation
 *
 * @author Paul Danyliuk
 */
public class PreviewHolder {


    private final View mAnimatedView;
    private final DataSet3 mAccelData;
    private final DataSet3 mGyroData;

    private final ObjectAnimator mAnimX;
    private final ObjectAnimator mAnimY;
    private final ObjectAnimator mAnimZ;
    private final ObjectAnimator mAnimRotation;

    private boolean mXEnabled = true;
    private boolean mYEnabled = true;
    private boolean mZEnabled = true;
    private boolean mRotationEnabled = true;

    public PreviewHolder(View animatedView, DataSet3 accelData, DataSet3 gyroData,
                         float linearMagnitude, float elevationMagnitude, float angularMagnitude) {
        mAnimatedView = animatedView;
        mAccelData = accelData;
        mGyroData = gyroData;

        mAnimX = ObjectAnimator.ofFloat(animatedView, "translationX", 0, linearMagnitude);
        mAnimX.setRepeatCount(ValueAnimator.INFINITE);
        mAnimY = ObjectAnimator.ofFloat(animatedView, "translationY", 0, -linearMagnitude);
        mAnimY.setRepeatCount(ValueAnimator.INFINITE);
        mAnimZ = ObjectAnimator.ofFloat(animatedView, "translationZ", 0, elevationMagnitude);
        mAnimZ.setRepeatCount(ValueAnimator.INFINITE);
        mAnimRotation = ObjectAnimator.ofFloat(animatedView, "rotation", 0, -angularMagnitude);
        mAnimRotation.setRepeatCount(ValueAnimator.INFINITE);
    }

    public void setDuration(long durationMs) {
        mAnimX.setDuration(durationMs);
        mAnimY.setDuration(durationMs);
        mAnimZ.setDuration(durationMs);
        mAnimRotation.setDuration(durationMs);
    }

    public void startAnimation() {
        if (mXEnabled) {
            mAnimX.start();
        } else {
            mAnimatedView.setTranslationX(0f);
        }
        if (mYEnabled) {
            mAnimY.start();
        } else {
            mAnimatedView.setTranslationY(0f);
        }
        if (mZEnabled) {
            mAnimZ.start();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mAnimatedView.setTranslationZ(0f);
            }
        }
        if (mRotationEnabled) {
            mAnimRotation.start();
        } else {
            mAnimatedView.setRotation(0f);
        }
    }

    public void stopAnimation() {
        mAnimX.cancel();
        mAnimY.cancel();
        mAnimZ.cancel();
        mAnimRotation.cancel();
    }

    public void setStatus(boolean xEnabled, boolean yEnabled, boolean zEnabled, boolean rotationEnabled) {
        mXEnabled = xEnabled;
        mYEnabled = yEnabled;
        mZEnabled = zEnabled;
        mRotationEnabled = rotationEnabled;
    }

    public void setInterpolators(Interpolator x, Interpolator y, Interpolator z, Interpolator rot) {
        mAnimX.setInterpolator(x);
        mAnimY.setInterpolator(y);
        mAnimZ.setInterpolator(z);
        mAnimRotation.setInterpolator(rot);
    }

    public boolean isRotationEnabled() {
        return mRotationEnabled;
    }

    public boolean isZEnabled() {
        return mZEnabled;
    }

    public boolean isYEnabled() {
        return mYEnabled;
    }

    public boolean isXEnabled() {
        return mXEnabled;
    }
}
