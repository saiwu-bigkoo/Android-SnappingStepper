package com.bigkoo.snappingstepper;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.AccelerateInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bigkoo.snappingstepper.listener.SnappingStepperValueChangeListener;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.ValueAnimator;

import java.lang.ref.WeakReference;

/**
 * Created by Sai on 16/1/15.
 */
public class SnappingStepper extends RelativeLayout implements View.OnTouchListener, ValueAnimator.AnimatorListener, ValueAnimator.AnimatorUpdateListener {
    private SnappingStepperValueChangeListener listener;
    private TextView tvStepperContent;
    private ImageView ivStepperMinus, ivStepperPlus;
    public static int ANIMATIONDURATION = 300;//恢复动画时间
    public boolean animationing = false;//动画中,不能进行滑动
    private UpdateRunnable updateRunnable;
    private boolean stepTouch = false;//是否按着，判断是否还要继续更新数值和界面

    //按下后多少间隔触发快速改变模式
    private static final long STEPSPEEDCHANGEDURATION = 1000;
    private static long UPDATEDURATIONSLOW = 300;//数值更新频率-慢
    private static long UPDATEDURATIONFAST = 100;//数值更新频率-快
    private int valueSlowStep = 1;//慢速递增值 步长

    //按下的初始x值
    private float startX = 0;
    private float startStepperContentLeft = 0;
    private boolean hasStepperContentLeft = false;
    //按下时间
    private long startTime = 0;


    //当前状态
    private int status = STATUS_NORMAL;
    private static final int STATUS_MIMNUS = -1;
    private static final int STATUS_PLUS = 1;
    private static final int STATUS_NORMAL = 0;
    //当前模式
    private Mode mode = Mode.AUTO;

    public enum Mode {
        AUTO(0), CUSTOM(1);
        private final int value;

        Mode(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static Mode valueOf(int value) {    //    手写的从int到enum的转换函数
            switch (value) {
                case 0:
                    return AUTO;
                case 1:
                    return CUSTOM;
            }
            return AUTO;
        }
    }

    private int value = 0;
    private int minValue = 0;
    private int maxValue = 100;

    public SnappingStepper(Context context) {
        this(context, null);
    }

    public SnappingStepper(Context context, AttributeSet attrs) {
        super(context, attrs);
        initViews(attrs);
    }

    private void initViews(AttributeSet attrs) {


        LayoutInflater.from(getContext()).inflate(R.layout.view_snappingstepper, this, true);
        tvStepperContent = (TextView) findViewById(R.id.tvStepperContent);
        ivStepperMinus = (ImageView) findViewById(R.id.ivStepperMinus);
        ivStepperPlus = (ImageView) findViewById(R.id.ivStepperPlus);

        String text = "";
        Drawable background = null;
        Drawable contentBackground = null;
        Drawable leftButtonResources = null;
        Drawable rightButtonResources = null;
        Drawable leftButtonBackground = null;
        Drawable rightButtonBackground = null;
        int contentTextColor = getResources().getColor(R.color.cl_snappingstepper_text);
        float contentTextSize = 0;
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.SnappingStepper);
            int modeValue = a.getInt(R.styleable.SnappingStepper_mode, Mode.AUTO.getValue());
            mode = Mode.valueOf(modeValue);
            minValue = a.getInt(R.styleable.SnappingStepper_min, minValue);
            maxValue = a.getInt(R.styleable.SnappingStepper_max, maxValue);
            value = valueRangeCheck(a.getInt(R.styleable.SnappingStepper_value, value));
            valueSlowStep = a.getInt(R.styleable.SnappingStepper_step, valueSlowStep);
            if (valueSlowStep <= 0) valueSlowStep = 1;
            text = a.getString(R.styleable.SnappingStepper_text);

            background = a.getDrawable(R.styleable.SnappingStepper_stepper_background);
            contentBackground = a.getDrawable(R.styleable.SnappingStepper_stepper_contentBackground);
            leftButtonResources = a.getDrawable(R.styleable.SnappingStepper_stepper_leftButtonResources);
            rightButtonResources = a.getDrawable(R.styleable.SnappingStepper_stepper_rightButtonResources);
            leftButtonBackground = a.getDrawable(R.styleable.SnappingStepper_stepper_leftButtonBackground);
            rightButtonBackground = a.getDrawable(R.styleable.SnappingStepper_stepper_rightButtonBackground);

            contentTextColor = a.getColor(R.styleable.SnappingStepper_stepper_contentTextColor, contentTextColor);

            contentTextSize = a.getFloat(R.styleable.SnappingStepper_stepper_contentTextSize, 0);
            a.recycle();
        }

        if (background != null) {
            setBackgroundDrawable(background);
        } else {
            setBackgroundResource(R.color.cl_snappingstepper_button_press);
        }


        if (contentBackground != null) {
            setContentBackground(contentBackground);
        }
        tvStepperContent.setTextColor(contentTextColor);
        if (contentTextSize > 0)
            setContentTextSize(contentTextSize);

        if (leftButtonBackground != null) {
            ivStepperMinus.setBackgroundDrawable(leftButtonBackground);
        }
        if (rightButtonBackground != null) {
            ivStepperPlus.setBackgroundDrawable(rightButtonBackground);
        }

        if (leftButtonResources != null) {
            setLeftButtonResources(leftButtonResources);
        }
        if (rightButtonResources != null) {
            setRightButtonResources(rightButtonResources);
        }

        if (mode == Mode.AUTO)//AUTO模式，写数值到滑动条上
            tvStepperContent.setText(String.valueOf(value));
        else
            tvStepperContent.setText(text);

        //设置后onclick产生的点击状态会失效
        ivStepperMinus.setOnTouchListener(this);
        ivStepperPlus.setOnTouchListener(this);
        setOnTouchListener(this);

        updateRunnable = new UpdateRunnable(this);
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                stepTouch = true;
                postDelayed(updateRunnable, UPDATEDURATIONSLOW);
                //非按钮则记录位置
                startX = event.getX();
                initStartStepperContentLeft();
                startTime = System.currentTimeMillis();
                //如果是两边的按钮，分别设置为点击状态
                if (v == ivStepperMinus) {
                    ivStepperMinus.setPressed(true);
                    status = STATUS_MIMNUS;
                    break;
                } else if (v == ivStepperPlus) {
                    ivStepperPlus.setPressed(true);
                    status = STATUS_PLUS;
                    break;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                //是按钮则不能移动,恢复位置的动画中也不能移动
                if (v == ivStepperMinus || v == ivStepperPlus || animationing) break;
                //非按钮则进行移动
                float moveX = event.getX() - startX;
                float x = moveX + startStepperContentLeft;
                moveStepperContent(x);
                moveEffectStatus(moveX);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                stepTouch = false;
                //如果是两边的按钮，分别设置为点击状态
                if (v == ivStepperMinus) {
                    ivStepperMinus.setPressed(false);
                    break;
                } else if (v == ivStepperPlus) {
                    ivStepperPlus.setPressed(false);
                    break;
                }
                restoreStepperContent();
                break;
        }
        return true;
    }

    private void initStartStepperContentLeft() {
        if (hasStepperContentLeft) return;
        hasStepperContentLeft = true;
        startStepperContentLeft = tvStepperContent.getLeft();
    }

    /**
     * 中间滑条恢复原位置
     */
    private void restoreStepperContent() {
        if (animationing) return;
        animationing = true;
        ValueAnimator restoreTranslateAnimation = ValueAnimator.ofFloat(tvStepperContent.getLeft(), (int) startStepperContentLeft);
        restoreTranslateAnimation.setDuration(ANIMATIONDURATION);
        restoreTranslateAnimation.addListener(this);
        restoreTranslateAnimation.addUpdateListener(this);
        restoreTranslateAnimation.setInterpolator(new AccelerateInterpolator());
        restoreTranslateAnimation.start();
    }

    /**
     * 移动位置
     *
     * @param x
     */
    private void moveStepperContent(float x) {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        params.leftMargin = (int) x;
        //限制子控件移动必须在视图范围内
        if (params.leftMargin < 0 || (params.leftMargin + tvStepperContent.getWidth()) > getWidth())
            return;
        params.topMargin = 0;
        params.width = tvStepperContent.getWidth();
        params.height = tvStepperContent.getHeight();

        tvStepperContent.setLayoutParams(params);
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        Float value = (Float) animation.getAnimatedValue();
        moveStepperContent(value);
    }

    private void moveEffectStatus(float x) {
        int scaledTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        if (x > scaledTouchSlop) {
            status = STATUS_PLUS;
        } else if (x < -scaledTouchSlop) {
            status = STATUS_MIMNUS;
        } else {
            status = STATUS_NORMAL;
        }
    }

    private int getNextValue() {
        switch (status) {
            case STATUS_MIMNUS:
                return value - valueSlowStep;
            case STATUS_PLUS:
                return value + valueSlowStep;
            case STATUS_NORMAL:
                return value;
        }
        return value;
    }

    private void updateUI() {
        int nextValue = getNextValue();
        //判断是否在范围之内
        if (nextValue < minValue) {
            nextValue = minValue;
        }
        if (nextValue > maxValue) {
            nextValue = maxValue;
        }
        value = nextValue;
        if (mode == Mode.AUTO)//AUTO模式，写数值到滑动条上
            tvStepperContent.setText(String.valueOf(value));
        if (listener != null)
            listener.onValueChange(this, value);
        if (stepTouch)
            postDelayed(updateRunnable, (System.currentTimeMillis() - startTime > STEPSPEEDCHANGEDURATION) ? UPDATEDURATIONFAST : UPDATEDURATIONSLOW);
    }

    @Override
    public void onAnimationStart(Animator animation) {

    }

    @Override
    public void onAnimationEnd(Animator animation) {
        animationing = false;
    }

    @Override
    public void onAnimationCancel(Animator animation) {

    }

    @Override
    public void onAnimationRepeat(Animator animation) {

    }


    static class UpdateRunnable implements Runnable {
        private WeakReference<SnappingStepper> view;

        public UpdateRunnable(SnappingStepper view) {
            this.view = new WeakReference<SnappingStepper>(view);
        }

        public void run() {
            SnappingStepper stepper = view.get();
            if (stepper != null) {
                stepper.updateUI();
            }
        }
    }

    public void setOnValueChangeListener(SnappingStepperValueChangeListener listener) {
        this.listener = listener;
    }

    /**
     * 返回当前模式类型
     *
     * @return
     */
    public Mode getMode() {
        return mode;
    }

    /**
     * 模式设置 AUTO(0) 数值写到滑动条, CUSTOM(1) 自定义文字;
     *
     * @param mode
     */
    public void setMode(Mode mode) {
        this.mode = mode;
    }

    /**
     * 获取当前值
     *
     * @return
     */
    public int getValue() {
        return value;
    }

    /**
     * 设置当前值
     *
     * @param value
     */
    public void setValue(int value) {
        this.value = valueRangeCheck(value);
        if (mode == Mode.AUTO)//AUTO模式，写数值到滑动条上
            tvStepperContent.setText(String.valueOf(value));
    }

    public int valueRangeCheck(int value) {
        if (value > maxValue) value = maxValue;
        else if (value < minValue) value = minValue;
        return value;
    }

    /**
     * 获取最小值
     *
     * @return
     */
    public int getMinValue() {
        return minValue;
    }

    /**
     * 设置最小值
     *
     * @return
     */
    public void setMinValue(int minValue) {
        this.minValue = minValue;
    }

    /**
     * 获取最大值
     *
     * @return
     */
    public int getMaxValue() {
        return maxValue;
    }

    /**
     * 设置最大值
     *
     * @return
     */
    public void setMaxValue(int maxValue) {
        this.maxValue = maxValue;
    }

    /**
     * 获取步长
     *
     * @return
     */
    public int getValueSlowStep() {
        return valueSlowStep;
    }

    /**
     * 设置步长
     *
     * @return
     */
    public void setValueSlowStep(int valueSlowStep) {
        this.valueSlowStep = valueSlowStep;
    }

    /**
     * 设置中间内容滑条颜色
     *
     * @param resId
     */
    public void setContentBackground(int resId) {
        tvStepperContent.setBackgroundResource(resId);
    }

    public void setContentBackground(Drawable drawable) {
        tvStepperContent.setBackgroundDrawable(drawable);
    }

    /**
     * 设置中间内容文字颜色
     *
     * @param resId
     */
    public void setContentTextColor(int resId) {
        tvStepperContent.setTextColor(getResources().getColor(resId));
    }

    /**
     * 设置中间内容文字,mode需为Custom才支持
     *
     * @param text
     */
    public void setText(String text) {
        tvStepperContent.setText(text);
    }

    /**
     * 设置中间内容文字大小
     *
     * @param size
     */
    public void setContentTextSize(float size) {
        tvStepperContent.setTextSize(size);
    }

    /**
     * 设置按钮背景
     *
     * @param resId
     */
    public void setButtonBackGround(int resId) {
        ivStepperMinus.setBackgroundResource(resId);
        ivStepperPlus.setBackgroundResource(resId);
    }

    /**
     * 设置按钮资源
     *
     * @param resId
     */
    public void setLeftButtonResources(int resId) {
        ivStepperMinus.setImageResource(resId);
    }

    /**
     * 设置按钮资源
     *
     * @param drawable
     */
    public void setLeftButtonResources(Drawable drawable) {
        ivStepperMinus.setImageDrawable(drawable);
    }

    /**
     * 设置按钮资源
     *
     * @param resId
     */
    public void setRightButtonResources(int resId) {
        ivStepperPlus.setImageResource(resId);
    }

    /**
     * 设置按钮资源
     *
     * @param drawable
     */
    public void setRightButtonResources(Drawable drawable) {
        ivStepperPlus.setImageDrawable(drawable);
    }
}
