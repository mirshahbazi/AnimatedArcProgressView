package com.kvolkov.animatedprogressviews;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.ColorInt;
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.View;

import com.kvolkov.animatedprogressviews.animations.OpacityAnimation;
import com.kvolkov.animatedprogressviews.animations.ProgressAnimation;

import java.util.ArrayList;
import java.util.List;

/**
 * This is an animated arc progress view for displaying indefinite progress animation.
 * Supports several types of animations, defined by"
 *  - {@code ProgressAnimation} for animating arcs.
 *  - {@code OpacityAnimation} for adding special effects generated by opacity animations for arcs.
 *
 * @author Kirill Volkov (https://github.com/vulko).
 *         Copyright (C). All rights reserved.
 */
public class ArcLoadingView extends View {

    /**
     * Drawing consts.
     */
    private static final long ANIMATION_START_DELAY = 0;
    private static final int DEFAULT_ARC_COUNT = 5;
    private static final float DEFAULT_ARC_SPACING = 5;
    private static final float DEFAULT_ARC_STROKE_WIDTH = 5;
    private static final int DEFAULT_ARC_COLOR = Color.argb(255, 0, 0, 200);

    /**
     * Animation stuff.
     */
    protected ProgressAnimation mProgressAnimation;
    protected OpacityAnimation mOpacityAnimation;

    /**
     * Drawing stuff.
     */
    @IntRange(from = 1, to = 30)
    protected int mArcCount;
    @FloatRange(from = 0.f, to = 100.f)
    protected float mArcSpacing = DEFAULT_ARC_SPACING;
    @FloatRange(from = 0.f, to = 500.f)
    protected float mArcStrokeWidth = DEFAULT_ARC_STROKE_WIDTH;
    @ColorInt
    protected int mPrimaryColor = DEFAULT_ARC_COLOR;

    protected Paint mArcPaint;
    protected List<Integer> mColorList = new ArrayList<>();
    protected List<RectF> mArcRectList = new ArrayList<>();


    /**
     * Self updating mechanism.
     */
    private boolean mInitialized = false;
    private Handler mHandler = new Handler();
    private Runnable mUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            mHandler.postDelayed(mUpdateRunnable, 16);
            invalidate();
        }
    };

    public ArcLoadingView(Context context) {
        super(context);
        initView(null);
    }

    public ArcLoadingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(attrs);
    }

    public ArcLoadingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(attrs);
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public ArcLoadingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView(attrs);
    }

    protected void initView(@Nullable AttributeSet attrs) {
        mProgressAnimation = new ProgressAnimation(ProgressAnimation.OPACITY_ANIMATION_TEST_STUB);
        mOpacityAnimation = new OpacityAnimation(OpacityAnimation.NONE);

        if (attrs != null) {
            TypedArray a = getContext().getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.ArcLoadingView,
                    0, 0);

            try {
                setArcCount(a.getInteger(R.styleable.ArcLoadingView_arcCount, DEFAULT_ARC_COUNT));
                setArcSpacing(a.getFloat(R.styleable.ArcLoadingView_arcSpacing, DEFAULT_ARC_SPACING));
                setArcStrokeWidth(a.getFloat(R.styleable.ArcLoadingView_arcStrokeWidth, DEFAULT_ARC_STROKE_WIDTH));
                setPrimaryColor(a.getColor(R.styleable.ArcLoadingView_defaultColor, DEFAULT_ARC_COLOR));
                setProgressAnimationType(a.getInt(R.styleable.ArcLoadingView_progressAnimation, ProgressAnimation.OPACITY_ANIMATION_TEST_STUB));
                setOpacityAnimationType(a.getInteger(R.styleable.ArcLoadingView_opacityAnimation, OpacityAnimation.NONE));
            } finally {
                a.recycle();
            }
        }

        if (mArcPaint == null) {
            mArcPaint = new Paint();
        }
        mArcPaint.setStyle(Paint.Style.STROKE);
        mArcPaint.setColor(Color.argb(100, 0, 0, 0));
        mArcPaint.setStrokeCap(Paint.Cap.ROUND);
        mArcPaint.setStrokeWidth(mArcStrokeWidth);
        mArcPaint.setAntiAlias(true);

        mInitialized = true;
    }

    /**
     * Init color list with single color. Since {@code mColorList} is used for drawing,
     * this should be called even when setting a single color.
     *
     * @param color A {@link ColorInt}.
     */
    protected void initColorList(@ColorInt int color) {
        mColorList.clear();
        for (int i = 0; i < mArcCount; i++) {
            mColorList.add(color);
        }
    }

    /**
     * Setup color list to use separate color for each arc.
     *
     * @param colorList List of colors for each arc.
     *
     * @throws IllegalStateException List size should be same as {@code mArcCount} value. Thrown in case it doesn't.
     */
    public void setColorList(List<Integer> colorList) throws IllegalStateException {
        if (colorList.size() != mArcCount) {
            throw new IllegalArgumentException("Color list should contain number of items equal to ArcCount.");
        }

        mColorList.clear();
        mColorList.addAll(colorList);
    }

    /**
     * Posts {@code mUpdateRunnable} to {@code mHandler}'s queue to trigger periodic redraws.
     */
    private void startUpdates() {
        mHandler.postDelayed(mUpdateRunnable, 16);
    }

    /**
     * Restarts animation.
     */
    private void restart() {
        if (mInitialized) {
            setPrimaryColor(mPrimaryColor);
            mProgressAnimation.restart();
            mOpacityAnimation.restart();
            startUpdates();
        }
    }

    /**
     * Restarts animation and remeasures layout.
     */
    private void restartAndReMeasure() {
        if (mInitialized) {
            restart();
            requestLayout();
        }
    }

    /**
     * Setup primary color for the arcs.
     * Basically initializes {@code mColorList} with provided color.
     *
     * @param color  A {@link ColorInt}.
     */
    protected void setPrimaryColor(@ColorInt int color) {
        if (mPrimaryColor != color) {
            mPrimaryColor = color;
        }
        initColorList(color);
    }

    /**
     * Setup progress animation type.
     *
     * @param animationType Should be of public static values in {@link ProgressAnimation}.
     */
    public void setProgressAnimationType(int animationType) {
        mProgressAnimation.setType(animationType);
        restart();
    }

    /**
     * Setup progress animation type.
     *
     * @param animationType Should be one of public static values from {@link ProgressAnimation}.
     */
    public void setOpacityAnimationType(int animationType) {
        mOpacityAnimation.setType(animationType);
        restart();
    }

    /**
     * Set number of arcs.
     *
     * @param arcs  Number in range [1..30].
     *
     * @throws IllegalArgumentException In case out of specified range.
     */
    public void setArcCount(@IntRange(from = 1, to = 30) int arcs) throws IllegalArgumentException {
        if (arcs < 1 || arcs > 30) {
            throw new IllegalArgumentException("Should be in range [1..30]");
        }

        mArcCount = arcs;

        // setup new number of animators before they will get reinitialized after calling restart()
        mProgressAnimation.setAnimatorsCount(mArcCount);
        mOpacityAnimation.setAnimatorsCount(mArcCount);

        restartAndReMeasure();
    }


    /**
     * Set spacing between arcs.
     *
     * @param spacing  Number in range [0.f .. 100.f].
     *
     * @throws IllegalArgumentException In case out of specified range.
     */
    public void setArcSpacing(@FloatRange(from = 0.f, to = 100.f) float spacing) {
        if (spacing < 0.f || spacing > 100.f) {
            throw new IllegalArgumentException("Should be in range [0.f .. 100.f]");
        }

        mArcSpacing = spacing;

        restartAndReMeasure();
    }


    /**
     * Set stroke width of the arc.
     *
     * @param width  Number in range [0.f .. 500.f].
     *
     * @throws IllegalArgumentException In case out of specified range.
     */
    public void setArcStrokeWidth(@FloatRange(from = 0.f, to = 500.f) float width) {
        if (width < 0.f || width > 500.f) {
            throw new IllegalArgumentException("Should be in range [0.f .. 1.f]");
        }

        mArcStrokeWidth = width;

        restartAndReMeasure();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        // start animating when attached to window
        restart();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        setMeasuredDimension(widthSize, heightSize);

        final int prefferedDimension = Math.min(widthSize, heightSize);
        // preffered stroke width to match drawing exactly with the number of arcs and specified padding
        final int arcs = mArcCount + 1; // + 1 since no need to see circle in the middle
        final float leftSpace = prefferedDimension - mArcSpacing * arcs;
        final float prefferedStrokeWidth; // TODO: so far ignored, but mb should be used so arcs can't overlap. Or mb leave overlap effect to play with params.
        if (leftSpace >= 0.f) {
            prefferedStrokeWidth = leftSpace / arcs;
        } else {
            prefferedStrokeWidth = mArcSpacing;
        }

        final boolean isWider = widthSize >= heightSize;
        final float sideDiff = isWider ? (widthSize - prefferedDimension) / 2.f
                                       : (heightSize - prefferedDimension) / 2.f;

        //mArcStrokeWidth = prefferedStrokeWidth;
        mArcPaint.setStrokeWidth(mArcStrokeWidth);

        // Init rects for arcs
        mArcRectList.clear();
        final float arcRadiusDiff = mArcSpacing * 2.f;
        final float initialLeft = isWider ? sideDiff : 0.f,
                    initialTop = isWider ? 0.f : sideDiff,
                    initialRight = widthSize - (isWider ? sideDiff : 0.f),
                    initialBottom = heightSize - (isWider ? 0.f : sideDiff);

        for (int i = 0; i < mArcCount; ++i) {
            final float left = initialLeft + arcRadiusDiff * (i + 1),
                        top = initialTop + arcRadiusDiff * (i + 1),
                        right = initialRight - arcRadiusDiff * (i + 1),
                        bottom = initialBottom - arcRadiusDiff * (i + 1);
            mArcRectList.add(new RectF(left, top, right, bottom));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!mInitialized) {
            // ignore draws until initialized
            return;
        }

        super.onDraw(canvas);

        for (int i = 0; i < mArcCount; ++i) {
            canvas.save();

            mArcPaint.setColor(mColorList.get(i));
            mArcPaint.setAlpha(mOpacityAnimation.getAnimatedValue(i));
            switch (mProgressAnimation.getType()) {
                case ProgressAnimation.RACE_CONDITION:
                    canvas.drawArc(mArcRectList.get(i), mProgressAnimation.getAlphaAnimatedValue(i), mProgressAnimation.getBetaAnimatedValue(i), false, mArcPaint);
                    break;

                case ProgressAnimation.SWIRLY:
                case ProgressAnimation.WHIRPOOL:
                case ProgressAnimation.HYPERLOOP:
                    canvas.drawArc(mArcRectList.get(i), mProgressAnimation.getAlphaAnimatedValue(i), mProgressAnimation.getBetaAnimatedValue(i), false, mArcPaint);
                    break;

                case ProgressAnimation.METRONOME_1:
                case ProgressAnimation.METRONOME_2:
                    canvas.drawArc(mArcRectList.get(i), mProgressAnimation.getInitialAlphaValue() + mProgressAnimation.getAlphaAnimatedValue(i), mProgressAnimation.getBetaAnimatedValue(i), false, mArcPaint);
                    break;

                case ProgressAnimation.METRONOME_3:
                case ProgressAnimation.METRONOME_4:
                    canvas.drawArc(mArcRectList.get(i), mProgressAnimation.getInitialAlphaValue() - mProgressAnimation.getBetaAnimatedValue(i), mProgressAnimation.getAlphaAnimatedValue(i), false, mArcPaint);
                    break;

                case ProgressAnimation.BUTTERFLY_KNIFE:
                    canvas.drawArc(mArcRectList.get(i), mProgressAnimation.getInitialAlphaValue() - mProgressAnimation.getBetaAnimatedValue(i), mProgressAnimation.getAlphaAnimatedValue(i), false, mArcPaint);
                    break;

                case ProgressAnimation.RAINBOW:
                case ProgressAnimation.GOTCHA:
                    canvas.drawArc(mArcRectList.get(i), mProgressAnimation.getInitialAlphaValue() + mProgressAnimation.getBetaAnimatedValue(i), -mProgressAnimation.getAlphaAnimatedValue(i), false, mArcPaint);
                    break;

                case ProgressAnimation.OPACITY_ANIMATION_TEST_STUB:
                    canvas.drawArc(mArcRectList.get(i), 0, 360, false, mArcPaint);
                    break;
            }

            canvas.restore();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        // TODO: manage mem stub
        mProgressAnimation.stop();
        mOpacityAnimation.stop();
        mInitialized = false;
    }



}