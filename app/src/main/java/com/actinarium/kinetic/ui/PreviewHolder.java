package com.actinarium.kinetic.ui;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.util.Log;
import android.view.View;
import android.view.animation.Interpolator;

/**
 * A presenter for preview animation
 *
 * @author Paul Danyliuk
 */
public class PreviewHolder {

    private static final String TAG = "PreviewHolder";

    public static final int NO_ANIMATOR = -1;
    public static final int ANIMATOR_X = 0;
    public static final int ANIMATOR_Y = 1;
    public static final int ANIMATOR_ROTATION = 2;

    private final View mAnimatedView;

    private final ObjectAnimator[] mAnimators = new ObjectAnimator[3];
    private final boolean[] mEnabled = new boolean[3];

    public PreviewHolder(View animatedView) {
        mAnimatedView = animatedView;

        mAnimators[ANIMATOR_X] = ObjectAnimator.ofFloat(animatedView, "translationX", 0f);
        mAnimators[ANIMATOR_X].setRepeatCount(ValueAnimator.INFINITE);
        mAnimators[ANIMATOR_Y] = ObjectAnimator.ofFloat(animatedView, "translationY", 0f);
        mAnimators[ANIMATOR_Y].setRepeatCount(ValueAnimator.INFINITE);
        mAnimators[ANIMATOR_ROTATION] = ObjectAnimator.ofFloat(animatedView, "rotation", 0f);
        mAnimators[ANIMATOR_ROTATION].setRepeatCount(ValueAnimator.INFINITE);
    }

    public void setDuration(long durationMs) {
        for (ObjectAnimator animator : mAnimators) {
            animator.setDuration(durationMs);
        }
    }

    public void startAnimation() {
        Log.d(TAG, "startAnimation: called");
        if (mEnabled[ANIMATOR_X]) {
            mAnimators[ANIMATOR_X].start();
        } else {
            mAnimatedView.setTranslationX(0f);
        }
        if (mEnabled[ANIMATOR_Y]) {
            mAnimators[ANIMATOR_Y].start();
        } else {
            mAnimatedView.setTranslationY(0f);
        }
        if (mEnabled[ANIMATOR_ROTATION]) {
            mAnimators[ANIMATOR_ROTATION].start();
        } else {
            mAnimatedView.setRotation(0f);
        }
    }

    public void stopAnimation() {
        Log.d(TAG, "stopAnimation: called");
        for (ObjectAnimator animator : mAnimators) {
            animator.cancel();
        }
    }

    public void setEnabled(int animator, boolean isEnabled) {
        mEnabled[animator] = isEnabled;
    }

    public boolean isEnabled(int animator) {
        return mEnabled[animator];
    }

    public void setInterpolator(int animator, Interpolator interpolator, float magnitude) {
        mAnimators[animator].setInterpolator(interpolator);
        // Y and rotation are in the opposite direction of recorded data
        mAnimators[animator].setFloatValues(0f, animator == ANIMATOR_X ? magnitude : -magnitude);
    }
}
