package pl.pawelkleczkowski.customgauge;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

public class CustomGauge extends View {

    private static final int DEFAULT_LONG_POINTER_SIZE = 1;

    private Paint mBasePaint;
    private Paint mPointPaint;
    private Paint mTextPaint;
    private float mBaseStrokeWidth;
    private float mPointStrokeWidth;//how large the inital point is
    private int mStrokeColor;
    private RectF mRect;
    private String mStrokeCap;
    private int mStartAngle;
    private int mSweepAngle;
    private int mStartValue;
    private int mEndValue;
    private int mValue;
    private double mPointAngle;//equal to the number of degrees of one increment/decrement in value
    private int mPoint;//conversion of the value into the degrees
    private int mPointSize;//the length of the initial point
    private int mPointStartColor;
    private int mPointEndColor;
    private int mDividerColor;
    private int mDividerSize;
    private int mDividerStepAngle;
    private int mDividersCount;
    private boolean mDividerDrawFirst;
    private boolean mDividerDrawLast;
    private boolean mDisplayValue;
    private boolean mAnimateValue;

    private ValueAnimator animator;
    public CustomGauge(Context context) {
        super(context);
        init();
    }

    public CustomGauge(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CustomGauge, 0, 0);

        // stroke style
        setStrokeWidth(a.getDimension(R.styleable.CustomGauge_gaugeStrokeWidth, 10));
        setStrokeColor(a.getColor(R.styleable.CustomGauge_gaugeStrokeColor, ContextCompat.getColor(context, android.R.color.darker_gray)));
        setStrokeCap(a.getString(R.styleable.CustomGauge_gaugeStrokeCap));

        // angle start and sweep (opposite direction 0, 270, 180, 90)
        setStartAngle(a.getInt(R.styleable.CustomGauge_gaugeStartAngle, 0));
        setSweepAngle(a.getInt(R.styleable.CustomGauge_gaugeSweepAngle, 360));

        // scale (from mStartValue to mEndValue)
        setStartValue(a.getInt(R.styleable.CustomGauge_gaugeStartValue, 0));
        setEndValue(a.getInt(R.styleable.CustomGauge_gaugeEndValue, 1000));

        // pointer size and color
        setPointSize(a.getInt(R.styleable.CustomGauge_gaugePointSize, 0));
        setPointStartColor(a.getColor(R.styleable.CustomGauge_gaugePointStartColor, ContextCompat.getColor(context, android.R.color.white)));
        setPointEndColor(a.getColor(R.styleable.CustomGauge_gaugePointEndColor, ContextCompat.getColor(context, android.R.color.white)));

        // divider options
        int dividerSize = a.getInt(R.styleable.CustomGauge_gaugeDividerSize, 0);
        setDividerColor(a.getColor(R.styleable.CustomGauge_gaugeDividerColor, ContextCompat.getColor(context, android.R.color.white)));
        int dividerStep = a.getInt(R.styleable.CustomGauge_gaugeDividerStep, 0);
        setDividerDrawFirst(a.getBoolean(R.styleable.CustomGauge_gaugeDividerDrawFirst, true));
        setDividerDrawLast(a.getBoolean(R.styleable.CustomGauge_gaugeDividerDrawLast, true));

        // calculating one point sweep
        mPointAngle = ((double) Math.abs(mSweepAngle) / (mEndValue - mStartValue));

        // calculating divider step
        if (dividerSize > 0) {
            mDividerSize = mSweepAngle / (Math.abs(mEndValue - mStartValue) / dividerSize);
            mDividersCount = 100 / dividerStep;
            mDividerStepAngle = mSweepAngle / mDividersCount;
        }

        setDisplayValue(a.getBoolean(R.styleable.CustomGauge_gaugeDisplayValue, true));
        setAnimateValue(a.getBoolean(R.styleable.CustomGauge_gaugeAnimateValue, false));
        setPointStrokeWidth(a.getDimension(R.styleable.CustomGauge_gaugePointStrokeWidth, 10));

        a.recycle();
        init();
    }

    private void init() {
        //main Paint
        mBasePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBasePaint.setColor(mStrokeColor);
        mBasePaint.setStrokeWidth(mBaseStrokeWidth);
        if (!TextUtils.isEmpty(mStrokeCap)) {
            if (mStrokeCap.equals("BUTT"))
                mBasePaint.setStrokeCap(Paint.Cap.BUTT);
            else if (mStrokeCap.equals("ROUND"))
                mBasePaint.setStrokeCap(Paint.Cap.ROUND);
        } else
            mBasePaint.setStrokeCap(Paint.Cap.BUTT);
        mBasePaint.setStyle(Paint.Style.STROKE);
        mRect = new RectF();
        mPointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPointPaint.setShader(new LinearGradient(getWidth(), getHeight(), 0, 0, mPointEndColor, mPointStartColor, Shader.TileMode.CLAMP));
        mPointPaint.setStrokeWidth(mPointStrokeWidth);
        if (!TextUtils.isEmpty(mStrokeCap)) {
            if (mStrokeCap.equals("BUTT"))
                mPointPaint.setStrokeCap(Paint.Cap.BUTT);
            else if (mStrokeCap.equals("ROUND"))
                mPointPaint.setStrokeCap(Paint.Cap.ROUND);
        } else
            mPointPaint.setStrokeCap(Paint.Cap.BUTT);
        mPointPaint.setStyle(Paint.Style.STROKE);

        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setTextSize(100);

        mValue = mStartValue;
        mPoint = mStartAngle;

        if (isAnimateValue()) {
            animator = ValueAnimator.ofFloat(1f);
            animator.setDuration(5000);
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    int diff = getEndValue() - getStartValue();
                    float value = animation.getAnimatedFraction();
                    int newValue = (int)(diff * value);
                    setValue(getStartValue() + newValue);
                }
            });
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float padding = getStrokeWidth();
        float size = getWidth()<getHeight() ? getWidth() : getHeight();
        float width = size - (2*padding);
        float height = size - (2*padding);
//        float radius = (width > height ? width/2 : height/2);
        float radius = (width < height ? width / 2 : height / 2);
        float rectLeft = (getWidth() - (2 * padding)) / 2 - radius + padding;
        float rectTop = (getHeight() - (2  *padding)) / 2 - radius + padding;
        float rectRight = (getWidth() - (2 * padding)) / 2 - radius + padding + width;
        float rectBottom = (getHeight() - (2 * padding)) / 2 - radius + padding + height;

        mRect.set(rectLeft, rectTop, rectRight, rectBottom);

        mBasePaint.setColor(mStrokeColor);
        mBasePaint.setShader(null);
        canvas.drawArc(mRect, mStartAngle, mSweepAngle, false, mBasePaint);

        //if size of pointer is defined, meaning we move the sized pointer and not fill the base track
        if (mPointSize > 0) {
            if (mPoint > mStartAngle + mPointSize / 2) {
                canvas.drawArc(mRect, mPoint - mPointSize / 2, mPointSize, false, mPointPaint);
            }
            //to avoid excedding start/zero point
            else {
                canvas.drawArc(mRect, mPoint, mPointSize, false, mPointPaint);
            }
        }
        //draw from start point to value point (long pointer)
        else {
            //use non-zero default value for start point (to avoid lack of pointer for start/zero value)
            if (mValue == mStartValue)
                canvas.drawArc(mRect, mStartAngle, DEFAULT_LONG_POINTER_SIZE, false, mPointPaint);
            else
                canvas.drawArc(mRect, mStartAngle, mPoint - mStartAngle, false, mPointPaint);
        }
        if (mDividerSize > 0) {
            mBasePaint.setColor(mDividerColor);
            mBasePaint.setShader(null);
            int i = mDividerDrawFirst ? 0 : 1;
            int max = mDividerDrawLast ? mDividersCount + 1 : mDividersCount;
            for (; i < max; i++) {
                canvas.drawArc(mRect, mStartAngle + i* mDividerStepAngle, mDividerSize, false, mBasePaint);
            }
        }
        if (mDisplayValue) {
            int xPos = canvas.getWidth() / 2;
            int yPos = canvas.getHeight() / 2;
            canvas.drawText(String.valueOf(getValue()), xPos, yPos, mTextPaint);
        }
    }

    public void setValue(int value) {
        mValue = value;
        mPoint = (int) (mStartAngle + (mValue-mStartValue) * mPointAngle);
        invalidate();
    }

    public int getValue() {
        return mValue;
    }

    @SuppressWarnings("unused")
    public float getStrokeWidth() {
        return mBaseStrokeWidth;
    }

    public void setStrokeWidth(float strokeWidth) {
        mBaseStrokeWidth = strokeWidth;
    }

    @SuppressWarnings("unused")
    public int getStrokeColor() {
        return mStrokeColor;
    }

    public void setStrokeColor(int strokeColor) {
        mStrokeColor = strokeColor;
    }

    @SuppressWarnings("unused")
    public String getStrokeCap() {
        return mStrokeCap;
    }

    public void setStrokeCap(String strokeCap) {
        mStrokeCap = strokeCap;
    }

    @SuppressWarnings("unused")
    public int getStartAngle() {
        return mStartAngle;
    }

    public void setStartAngle(int startAngle) {
        mStartAngle = startAngle;
    }

    @SuppressWarnings("unused")
    public int getSweepAngle() {
        return mSweepAngle;
    }

    public void setSweepAngle(int sweepAngle) {
        mSweepAngle = sweepAngle;
    }

    @SuppressWarnings("unused")
    public int getStartValue() {
        return mStartValue;
    }

    public void setStartValue(int startValue) {
        mStartValue = startValue;
    }

    @SuppressWarnings("unused")
    public int getEndValue() {
        return mEndValue;
    }

    public void setEndValue(int endValue) {
        mEndValue = endValue;
        mPointAngle = ((double) Math.abs(mSweepAngle) / (mEndValue - mStartValue));
        invalidate();
    }

    @SuppressWarnings("unused")
    public int getPointSize() {
        return mPointSize;
    }

    public void setPointSize(int pointSize) {
        mPointSize = pointSize;
    }

    @SuppressWarnings("unused")
    public int getPointStartColor() {
        return mPointStartColor;
    }

    public void setPointStartColor(int pointStartColor) {
        mPointStartColor = pointStartColor;
    }

    @SuppressWarnings("unused")
    public int getPointEndColor() {
        return mPointEndColor;
    }

    public void setPointEndColor(int pointEndColor) {
        mPointEndColor = pointEndColor;
    }

    @SuppressWarnings("unused")
    public int getDividerColor() {
        return mDividerColor;
    }

    public void setDividerColor(int dividerColor) {
        mDividerColor = dividerColor;
    }

    @SuppressWarnings("unused")
    public boolean isDividerDrawFirst() {
        return mDividerDrawFirst;
    }

    public void setDividerDrawFirst(boolean dividerDrawFirst) {
        mDividerDrawFirst = dividerDrawFirst;
    }

    @SuppressWarnings("unused")
    public boolean isDividerDrawLast() {
        return mDividerDrawLast;
    }

    public void setDividerDrawLast(boolean dividerDrawLast) {
        mDividerDrawLast = dividerDrawLast;
    }

    public float getPointStrokeWidth() {
        return mPointStrokeWidth;
    }

    public void setPointStrokeWidth(float mPointStrokeWidth) {
        this.mPointStrokeWidth = mPointStrokeWidth;
    }

    public boolean isDisplayValue() {
        return mDisplayValue;
    }

    public void setDisplayValue(boolean mDisplayValue) {
        this.mDisplayValue = mDisplayValue;
    }

    public boolean isAnimateValue() {
        return mAnimateValue;
    }

    public void setAnimateValue(boolean mAnimateValue) {
        this.mAnimateValue = mAnimateValue;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (animator != null) {
            animator.start();
        }
    }
}