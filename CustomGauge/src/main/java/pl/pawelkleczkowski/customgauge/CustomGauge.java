package pl.pawelkleczkowski.customgauge;

import android.animation.TimeInterpolator;
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
import android.view.animation.BaseInterpolator;
import android.view.animation.BounceInterpolator;

import java.sql.Time;

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
    private int mTargetValue;
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
    //default interpolator
    private TimeInterpolator interpolator = new AccelerateDecelerateInterpolator();

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
            animator.setInterpolator(interpolator);
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
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        // Account for padding
        float xpad = (float)(getPaddingLeft() + getPaddingRight());
        float ypad = (float)(getPaddingTop() + getPaddingBottom());

        //get the larger of the stroke paddings and halve it
        float strokePadding = getStrokeWidth() > getPointStrokeWidth() ? getStrokeWidth() : getPointStrokeWidth();
        float innerRectSize = 0, rectLeft = 0, rectRight = 0, rectTop = 0, rectBottom = 0;
        if (getWidth() < getHeight()) {
            innerRectSize = getWidth() - xpad;
            rectLeft = getPaddingLeft() + strokePadding;
            rectRight = getWidth() - getPaddingRight() - strokePadding;
            rectTop = (getHeight() / 2)  - (innerRectSize / 2) + strokePadding;
            rectBottom = (getHeight() / 2) + (innerRectSize / 2) - strokePadding;
        } else {
            innerRectSize = getHeight() - ypad;
            rectTop = getPaddingTop() + strokePadding;
            rectBottom = getHeight() - getPaddingBottom() - strokePadding;
            rectLeft = (getWidth() / 2)  - (innerRectSize / 2) + strokePadding;
            rectRight = (getWidth() / 2) + (innerRectSize / 2) - strokePadding;
        }
        mRect.set(rectLeft, rectTop, rectRight, rectBottom);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
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
        if (getDisplayValue()) {
            int xPos = canvas.getWidth() / 2;
            int yPos = canvas.getHeight() / 2;
            canvas.drawText(String.valueOf(getValue()), xPos, yPos, mTextPaint);
        }
    }

    public CustomGauge setTargetValue(int target) {
        mTargetValue = target;
        return this;
    }

    public int getTargetValue() {
        return mTargetValue;
    }

    private void setValue(int value) {
        mValue = value;
        mPoint = (int) (mStartAngle + (mValue-mStartValue) * mPointAngle);
        invalidate();
    }

    private int getValue() {
        return mValue;
    }

    public CustomGauge setInterpolator (TimeInterpolator interpolator) {
        this.interpolator = interpolator;
        return this;
    }

    public TimeInterpolator getInterpolator () {
        return interpolator;
    }


    @SuppressWarnings("unused")
    public float getStrokeWidth() {
        return mBaseStrokeWidth;
    }

    public CustomGauge setStrokeWidth(float strokeWidth) {
        mBaseStrokeWidth = strokeWidth;
        return this;
    }

    @SuppressWarnings("unused")
    public int getStrokeColor() {
        return mStrokeColor;
    }

    public CustomGauge setStrokeColor(int strokeColor) {
        mStrokeColor = strokeColor;
        return this;
    }

    @SuppressWarnings("unused")
    public String getStrokeCap() {
        return mStrokeCap;
    }

    public CustomGauge setStrokeCap(String strokeCap) {
        mStrokeCap = strokeCap;
        return this;
    }

    @SuppressWarnings("unused")
    public int getStartAngle() {
        return mStartAngle;
    }

    public CustomGauge setStartAngle(int startAngle) {
        mStartAngle = startAngle;
        return this;
    }

    @SuppressWarnings("unused")
    public int getSweepAngle() {
        return mSweepAngle;
    }

    public CustomGauge setSweepAngle(int sweepAngle) {
        mSweepAngle = sweepAngle;
        return this;
    }

    @SuppressWarnings("unused")
    public int getStartValue() {
        return mStartValue;
    }

    public CustomGauge setStartValue(int startValue) {
        mStartValue = startValue;
        return this;
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

    public CustomGauge setPointSize(int pointSize) {
        mPointSize = pointSize;
        return this;
    }

    @SuppressWarnings("unused")
    public int getPointStartColor() {
        return mPointStartColor;
    }

    public CustomGauge setPointStartColor(int pointStartColor) {
        mPointStartColor = pointStartColor;
        return this;
    }

    @SuppressWarnings("unused")
    public int getPointEndColor() {
        return mPointEndColor;
    }

    public CustomGauge setPointEndColor(int pointEndColor) {
        mPointEndColor = pointEndColor;
        return this;
    }

    @SuppressWarnings("unused")
    public int getDividerColor() {
        return mDividerColor;
    }

    public CustomGauge setDividerColor(int dividerColor) {
        mDividerColor = dividerColor;
        return this;
    }

    @SuppressWarnings("unused")
    public boolean isDividerDrawFirst() {
        return mDividerDrawFirst;
    }

    public CustomGauge setDividerDrawFirst(boolean dividerDrawFirst) {
        mDividerDrawFirst = dividerDrawFirst;
        return this;
    }

    @SuppressWarnings("unused")
    public boolean isDividerDrawLast() {
        return mDividerDrawLast;
    }

    public CustomGauge setDividerDrawLast(boolean dividerDrawLast) {
        mDividerDrawLast = dividerDrawLast;
        return this;
    }

    public float getPointStrokeWidth() {
        return mPointStrokeWidth;
    }

    public CustomGauge setPointStrokeWidth(float PointStrokeWidth) {
        mPointStrokeWidth = PointStrokeWidth;
        return this;
    }

    public boolean getDisplayValue() {
        return mDisplayValue;
    }

    public CustomGauge setDisplayValue(boolean mDisplayValue) {
        this.mDisplayValue = mDisplayValue;
        return this;
    }

    public boolean isAnimateValue() {
        return mAnimateValue;
    }

    public CustomGauge setAnimateValue(boolean mAnimateValue) {
        this.mAnimateValue = mAnimateValue;
        return this;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (animator != null) {
            animator.start();
        }
    }
}