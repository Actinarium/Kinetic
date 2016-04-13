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

package com.actinarium.kinetic.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import com.actinarium.kinetic.R;

/**
 * Own implementation of chart optimized for rendering data from Kinetic app
 *
 * @author Paul Danyliuk
 */
public class KineticChart extends View {

    // Chart data
    private long[] mTimes;
    private float[] mValues;
    private int mLength;
    private float mMinY;
    private float mMaxY;
    private float mTrimStart;
    private float mTrimEnd;

    // Drawing data
    private int mLineColor;
    private int mDimLineColor;
    private Rect mChartArea;
    private Paint mLinePaint;
    private Paint mAxisPaint;
    private Path mPath;
    private int mAxisThickness;

    // Pre-calculated values
    private float mMultY;
    private double mDivX;
    private float mZeroY;

    public KineticChart(Context context) {
        super(context);
        init();
    }

    public KineticChart(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KineticChart(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();

        TypedArray array = context.getTheme().obtainStyledAttributes(attrs, R.styleable.KineticChart, defStyleAttr, 0);

        mLinePaint.setStrokeWidth(array.getDimension(R.styleable.KineticChart_lineThickness, 0));
        mLineColor = array.getColor(R.styleable.KineticChart_lineColor, Color.BLACK);
        mDimLineColor = array.getColor(R.styleable.KineticChart_dimLineColor, Color.DKGRAY);
        mLinePaint.setColor(mLineColor);
        mAxisPaint.setColor(array.getColor(R.styleable.KineticChart_axisColor, Color.GRAY));
        mAxisThickness = array.getDimensionPixelSize(R.styleable.KineticChart_axisThickness, 1);

        array.recycle();
    }

    /**
     * Common initialization (e.g. object creation)
     */
    private void init() {
        mChartArea = new Rect();

        mLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setStrokeJoin(Paint.Join.ROUND);
        mLinePaint.setStrokeCap(Paint.Cap.ROUND);

        mAxisPaint = new Paint();
        mAxisPaint.setStyle(Paint.Style.FILL);

        mPath = new Path();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Since we're always using width=match_parent and height=xxxdp, just return provided metrics
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (!changed) {
            return;
        }

        mChartArea.set(
                getPaddingLeft(),
                getPaddingTop(),
                right - left - getPaddingRight(),
                bottom - top - getPaddingBottom()
        );
        recalculateChartMetrics();
        recalculateChartPath();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Draw horizontal axis on the zero
        canvas.drawRect(mChartArea.left, mZeroY - mAxisThickness / 2, mChartArea.right, mZeroY + mAxisThickness / 2, mAxisPaint);

        if (mLength != 0) {
            // Draw path
            canvas.save();
            canvas.translate(mChartArea.left, mZeroY);

            if (mTrimStart != 0 || mTrimEnd != 0) {
                final int width = mChartArea.width();
                // Gray parts
                mLinePaint.setColor(mDimLineColor);
                canvas.save();
                canvas.clipRect(0, -mZeroY, width * mTrimStart, Float.MAX_VALUE);
                canvas.drawPath(mPath, mLinePaint);
                canvas.restore();
                canvas.save();
                canvas.clipRect(width * (1 - mTrimEnd), -mZeroY, width, Float.MAX_VALUE);
                canvas.drawPath(mPath, mLinePaint);
                canvas.restore();

                mLinePaint.setColor(mLineColor);
                canvas.save();
                canvas.clipRect(width * mTrimStart, -mZeroY, width * (1 - mTrimEnd), Float.MAX_VALUE);
                canvas.drawPath(mPath, mLinePaint);
                canvas.restore();
            } else {
                canvas.drawPath(mPath, mLinePaint);
            }

            canvas.restore();
        }

        // Draw vertical axis on the left - over the path
        canvas.drawRect(mChartArea.left - mAxisThickness, mChartArea.top, mChartArea.left, mChartArea.bottom, mAxisPaint);
    }

    /**
     * Set the data to draw in this chart. Recalculates everything that's required. <b>Heads up:</b> it's OK to pass
     * "live" arrays here (i.e. ones that will be externally changed) as long as you call this method again afterwards.
     *
     * @param times  Timestamps in nanos, for X axis
     * @param values Sensor readings (pre-transformed if required), for Y axis
     * @param length Number of entries to use from times and values arrays
     * @param minY   Value to use as a minimum
     * @param maxY   Value to use as a maximum
     */
    public void setData(long[] times, float[] values, int length, float minY, float maxY) {
        mTimes = times;
        mValues = values;
        mLength = length;
        mMinY = minY;
        mMaxY = maxY;
        if (!mChartArea.isEmpty()) {
            recalculateChartMetrics();
            recalculateChartPath();
            invalidate();
        }
    }

    /**
     * Tell the chart how many of it from the start and the end to trim out
     *
     * @param trimStart Fraction to trim from the start, from 0f to 1f
     * @param trimEnd   Fraction to trim from the end, from 0f to 1f
     */
    public void setTrim(float trimStart, float trimEnd) {
        mTrimStart = trimStart;
        mTrimEnd = trimEnd;
        if (!mChartArea.isEmpty()) {
            invalidate();
        }
    }

    private void recalculateChartPath() {
        mPath.rewind();
        mPath.moveTo(0, (mValues[0]) * mMultY);
        for (int i = 1; i < mLength; i++) {
            mPath.lineTo((float) ((mTimes[i] - mTimes[0]) / mDivX), (mValues[i]) * mMultY);
        }
    }

    /**
     * Pre-calculate transformation variables
     */
    private void recalculateChartMetrics() {
        // Multiplier for transforming values into chart coords
        if (mMaxY != mMinY) {
            // Subtracting max from min to invert Y axis (canvas' Y starts at the top)
            mMultY = mChartArea.height() / (mMinY - mMaxY);
        }

        // Division factor for transforming times into chart x coords
        if (mTimes != null && mChartArea.width() != 0) {
            mDivX = (mTimes[mLength - 1] - mTimes[0]) / mChartArea.width();
        }
        if (mDivX == 0) {
            mDivX = 1L;
        }

        mZeroY = mChartArea.top + mChartArea.height() * mMaxY / (mMaxY - mMinY);
    }


}
