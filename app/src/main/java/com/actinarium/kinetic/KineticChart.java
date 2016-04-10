package com.actinarium.kinetic;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

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
    private float mMin;
    private float mMax;
    private float mStepY;
    private long mStepX;

    // Drawing data
    private Rect mChartArea;
    private Paint mLinePaint;
    private Path mPath;

    // Pre-calculated values
    private float mMultY;
    private double mDivX;

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
        mLinePaint.setColor(array.getColor(R.styleable.KineticChart_lineColor, Color.BLACK));

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
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mLength == 0) {
            return;
        }

        canvas.save();
        canvas.translate(mChartArea.top, mChartArea.left);
        canvas.drawPath(mPath, mLinePaint);
        canvas.restore();
    }

    /**
     * Set the data to draw in this chart. Recalculates everything that's required. <b>Heads up:</b> it's OK to pass
     * "live" arrays here (i.e. ones that will be externally changed) as long as you call this method again afterwards.
     *
     * @param times  Timestamps in nanos, for X axis
     * @param values Sensor readings (pre-transformed if required), for Y axis
     * @param length Number of entries to use from times and values arrays
     * @param min    Value to use as a minimum
     * @param max    Value to use as a maximum
     * @param stepX  Horizontal step of the grid
     * @param stepY  Vertical step of the grid
     */
    public void setData(long[] times, float[] values, int length, float min, float max, long stepX, float stepY) {
        mTimes = times;
        mValues = values;
        mLength = length;
        mMin = min;
        mMax = max;
        mStepY = stepY;
        mStepX = stepX;
        recalculateChartMetrics();
        recalculateChartPath();
        invalidate();
    }

    private void recalculateChartPath() {
        mPath.rewind();
        mPath.moveTo((mValues[0]) * mMultY, 0);
        for (int i = 1; i < mLength; i++) {
            mPath.lineTo((float) ((mTimes[i] - mTimes[0]) / mDivX), (mValues[i]) * mMultY);
        }
    }

    /**
     * Pre-calculate transformation variables
     */
    private void recalculateChartMetrics() {
        // Multiplier for transforming values into chart coords
        if (mMax != mMin) {
            // Subtracting max from min to invert Y axis (canvas' Y starts at the top)
            mMultY = mChartArea.height() / (mMin - mMax);
        }

        // Division factor for transforming times into chart x coords
        if (mTimes != null) {
            mDivX = (mTimes[mLength - 1] - mTimes[0]) / mChartArea.width();
        }
        if (mDivX == 0) {
            mDivX = 1L;
        }
    }


}
