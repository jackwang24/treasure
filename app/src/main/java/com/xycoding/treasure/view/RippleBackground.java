package com.xycoding.treasure.view;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

import com.xycoding.treasure.R;

public class RippleBackground extends RelativeLayout {

    private static final int DEFAULT_RIPPLE_RADIUS = 50;
    private static final int DEFAULT_DURATION_TIME = 100;
    private static final long DEFAULT_BREATHING_TIME = 800;
    private static final float DEFAULT_RIPPLE_SCALE = 1.f;
    private static final float DEFAULT_RIPPLE_SCALE_MAX = 1.4f;
    private static final float DEFAULT_BREATHING_MAX = 1.1f;

    private Handler mHandler = new Handler();
    private int mRippleColor;
    private float mRippleRadius;
    private int mRippleAnimationTime;
    private float mCurrentRippleScale = DEFAULT_RIPPLE_SCALE;
    private float mNextRippleScale = DEFAULT_RIPPLE_SCALE;
    private Paint mPaint;
    private AnimatorSet mVolumeAnimatorSet;
    private AnimatorSet mBreathingAnimatorSet;
    private RippleView mRippleView;

    private ObjectAnimator mVolumeScaleX;
    private ObjectAnimator mVolumeScaleY;
    private ObjectAnimator mBreathingScaleUpX;
    private ObjectAnimator mBreathingScaleUpY;

    public RippleBackground(Context context) {
        super(context);
    }

    public RippleBackground(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public RippleBackground(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopBreathingAnimation();
    }

    private void init(final Context context, final AttributeSet attrs) {
        if (isInEditMode()) {
            return;
        }
        if (attrs == null) {
            throw new IllegalArgumentException("Attributes should be provided to this view,");
        }
        final TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.RippleBackground);
        mRippleColor = typedArray.getColor(R.styleable.RippleBackground_rb_color, ContextCompat.getColor(getContext(), R.color.ripple_color));
        mRippleRadius = typedArray.getDimension(R.styleable.RippleBackground_rb_radius, DEFAULT_RIPPLE_RADIUS);
        mRippleAnimationTime = typedArray.getInt(R.styleable.RippleBackground_rb_duration, DEFAULT_DURATION_TIME);
        typedArray.recycle();

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(mRippleColor);

        LayoutParams layoutParams = new LayoutParams((int) (2 * mRippleRadius), (int) (2 * (mRippleRadius)));
        layoutParams.addRule(CENTER_IN_PARENT, TRUE);
        mRippleView = new RippleView(getContext());
        addView(mRippleView, layoutParams);


        mBreathingRunnable.run();
    }

    private void ensureObjectAnimator() {
        if (mVolumeScaleX == null) {
            mVolumeScaleX = ObjectAnimator.ofFloat(mRippleView, "ScaleX", DEFAULT_RIPPLE_SCALE, DEFAULT_RIPPLE_SCALE);
        }
        if (mVolumeScaleY == null) {
            mVolumeScaleY = ObjectAnimator.ofFloat(mRippleView, "ScaleY", DEFAULT_RIPPLE_SCALE, DEFAULT_RIPPLE_SCALE);
        }
        if (mBreathingScaleUpX == null) {
            mBreathingScaleUpX = ObjectAnimator.ofFloat(mRippleView, "ScaleX", DEFAULT_RIPPLE_SCALE, DEFAULT_BREATHING_MAX);
            mBreathingScaleUpX.setDuration(DEFAULT_BREATHING_TIME);
            mBreathingScaleUpX.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mCurrentRippleScale = DEFAULT_RIPPLE_SCALE + (DEFAULT_BREATHING_MAX - DEFAULT_RIPPLE_SCALE) * animation.getAnimatedFraction();
                }
            });
        }
        if (mBreathingScaleUpY == null) {
            mBreathingScaleUpY = ObjectAnimator.ofFloat(mRippleView, "ScaleY", DEFAULT_RIPPLE_SCALE, DEFAULT_BREATHING_MAX);
            mBreathingScaleUpY.setDuration(DEFAULT_BREATHING_TIME);
        }
    }

    public void stopVolumeAnimation() {
        if (mVolumeAnimatorSet != null && mVolumeAnimatorSet.isRunning()) {
            mVolumeAnimatorSet.end();
        }
        mBreathingRunnable.run();
    }

    public void stopBreathingAnimation() {
        mRippleView.setVisibility(INVISIBLE);
        if (mVolumeAnimatorSet != null && mVolumeAnimatorSet.isRunning()) {
            mVolumeAnimatorSet.end();
        }
        if (mBreathingAnimatorSet != null && mBreathingAnimatorSet.isRunning()) {
            mBreathingAnimatorSet.end();
        }
        if (mHandler != null) {
            mHandler.removeCallbacks(mBreathingRunnable);
        }
    }

    public void setRippleScale(float scale) {
        if (scale > 0.f) {
            mNextRippleScale = DEFAULT_BREATHING_MAX + scale * (DEFAULT_RIPPLE_SCALE_MAX - DEFAULT_BREATHING_MAX);
            if (mVolumeAnimatorSet == null || !mVolumeAnimatorSet.isRunning()) {
                mVolumeRunnable.run();
            }
        } else {
            if ((mBreathingAnimatorSet == null || !mBreathingAnimatorSet.isRunning())
                    && (mVolumeAnimatorSet == null || !mVolumeAnimatorSet.isRunning())) {
                mBreathingRunnable.run();
            }
        }
    }

    private Runnable mVolumeRunnable = new Runnable() {
        @Override
        public void run() {
            mRippleView.setVisibility(VISIBLE);
            mHandler.removeCallbacks(mBreathingRunnable);
            if (mBreathingAnimatorSet != null && mBreathingAnimatorSet.isRunning()) {
                mBreathingAnimatorSet.end();
            }
            ensureObjectAnimator();

            mVolumeScaleX.setFloatValues(mCurrentRippleScale, mNextRippleScale);
            mVolumeScaleX.removeAllUpdateListeners();
            mVolumeScaleX.setDuration(mRippleAnimationTime);

            mVolumeScaleY.setFloatValues(mCurrentRippleScale, mNextRippleScale);
            mVolumeScaleY.setDuration(mRippleAnimationTime);

            mCurrentRippleScale = mNextRippleScale;

            mVolumeAnimatorSet = new AnimatorSet();
            mVolumeAnimatorSet.playTogether(mVolumeScaleX, mVolumeScaleY);
            mVolumeAnimatorSet.start();
        }
    };

    private Runnable mBreathingRunnable = new Runnable() {
        @Override
        public void run() {
            mRippleView.setVisibility(VISIBLE);
            ensureObjectAnimator();

            final float startScale = mCurrentRippleScale;
            long startAnimationTime = startScale == DEFAULT_RIPPLE_SCALE ? 0 : DEFAULT_BREATHING_TIME;
            mVolumeScaleX.setFloatValues(startScale, DEFAULT_RIPPLE_SCALE);
            mVolumeScaleX.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mCurrentRippleScale = startScale + (DEFAULT_RIPPLE_SCALE - startScale) * animation.getAnimatedFraction();
                }
            });
            mVolumeScaleX.setDuration(startAnimationTime);

            mVolumeScaleY.setFloatValues(startScale, DEFAULT_RIPPLE_SCALE);
            mVolumeScaleY.setDuration(startAnimationTime);

            mBreathingScaleUpX.setStartDelay(startAnimationTime);
            mBreathingScaleUpY.setStartDelay(startAnimationTime);

            mBreathingAnimatorSet = new AnimatorSet();
            mBreathingAnimatorSet.playTogether(mVolumeScaleX, mVolumeScaleY, mBreathingScaleUpX, mBreathingScaleUpY);
            mBreathingAnimatorSet.start();
            mHandler.postDelayed(mBreathingRunnable, startAnimationTime + DEFAULT_BREATHING_TIME);
        }
    };

    private class RippleView extends View {

        public RippleView(Context context) {
            super(context);
            setVisibility(INVISIBLE);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int radius = (Math.min(getWidth(), getHeight())) / 2;
            canvas.drawCircle(radius, radius, radius, mPaint);
        }
    }

}
