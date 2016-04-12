package com.actinarium.kinetic.ui;

import android.content.Context;
import android.text.Layout;
import android.util.AttributeSet;
import com.actinarium.aligned.TextView;

/**
 * Blockquote limits TextView's width to the longest line
 *
 * @author Paul Danyliuk
 */
public class BlockquoteTextView extends TextView {
    public BlockquoteTextView(Context context) {
        super(context);
    }

    public BlockquoteTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BlockquoteTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // Now fix width
        float max = 0;
        Layout layout = getLayout();
        for (int i = 0, size = layout.getLineCount(); i < size; i++) {
            final float lineWidth = layout.getLineMax(i);
            if (lineWidth > max) {
                max = lineWidth;
            }
        }

        final int height = getMeasuredHeight();
        final int width = (int) Math.ceil(max) + getCompoundPaddingLeft() + getCompoundPaddingRight();

        setMeasuredDimension(width, height);
    }
}
