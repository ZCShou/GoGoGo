package com.zcshou.joystick;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.CountDownTimer;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.preference.PreferenceManager;

import com.zcshou.gogogo.R;

public class JoyStick extends View {

    final private Context mContext;
    private WindowManager.LayoutParams mWindowParams;
    private WindowManager mWindowManager;
    private final LayoutInflater inflater;
    private View mJoystickView;
    private LinearLayout mLatLngView;
    private JoyStickClickListener mListener;
    // 控制按键相关
    ImageButton btnInput;
    boolean isWalk;
    ImageButton btnWalk;
    boolean isRun;
    ImageButton btnRun;
    boolean isBike;
    ImageButton btnBike;

    // 移动
    private TimeCount time;
    double mAngle;
    double mSpeed;
    SharedPreferences sharedPreferences;

    public JoyStick(Context context) {
        super(context);
        this.mContext = context;

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

        initWindowManager();

        inflater = LayoutInflater.from(mContext);

        if (inflater != null) {
            initJoyStickView();

            initJoyStickLatLngView();
        }

        // 这里记录启动次数
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        long num = sharedPreferences.getLong("setting_startup_num", 0);
        sharedPreferences.edit()
                .putLong("setting_startup_num", ++num)
                .apply();

    }

    public JoyStick(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

        initWindowManager();

        inflater = LayoutInflater.from(mContext);

        if (inflater != null) {
            initJoyStickView();

            initJoyStickLatLngView();
        }
    }

    public JoyStick(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

        initWindowManager();

        inflater = LayoutInflater.from(mContext);

        if (inflater != null) {
            initJoyStickView();

            initJoyStickLatLngView();
        }
    }

    private void initWindowManager() {
        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        mWindowParams = new WindowManager.LayoutParams();
        mWindowParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        mWindowParams.format = PixelFormat.RGBA_8888;
        mWindowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        mWindowParams.gravity = Gravity.START | Gravity.TOP;
        mWindowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowParams.x = 300;
        mWindowParams.y = 300;

        Log.d("FLOAT", "initFloatWindow finish");
    }

    @SuppressLint("InflateParams")
    private void initJoyStickView() {
        time = new TimeCount(1000, 1000);
        String sSpeed = sharedPreferences.getString("setting_walk", "");
        if (sSpeed == null) {
            mSpeed = Double.parseDouble(getResources().getString(R.string.setting_walk_default));
        } else {
            mSpeed = Double.parseDouble(sSpeed);
        }

        mJoystickView = inflater.inflate(R.layout.joystick, null);
        mJoystickView.setOnTouchListener(new JoyStickOnTouchListener());

        btnInput = mJoystickView.findViewById(R.id.joystick_input);
        btnInput.setOnClickListener(v -> {
            if (mJoystickView != null) {
                mWindowManager.removeView(mJoystickView);
            }

            if (mLatLngView.getParent() == null) {
                mWindowParams.format = PixelFormat.RGBA_8888;
                mWindowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                mWindowParams.gravity = Gravity.START | Gravity.TOP;
                mWindowParams.width = WindowManager.LayoutParams.MATCH_PARENT;
                mWindowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
                mWindowParams.x = 300;
                mWindowParams.y = 300;
                mWindowManager.addView(mLatLngView, mWindowParams);
            }
        });

        isWalk = true;
        btnWalk = mJoystickView.findViewById(R.id.joystick_walk);
        btnWalk.setOnClickListener(v -> {
            if (!isWalk) {
                btnWalk.setImageResource(R.drawable.ic_walk_pressed);
                isWalk = true;
                btnRun.setImageResource(R.drawable.ic_run);
                isRun = false;
                btnBike.setImageResource(R.drawable.ic_bike);
                isBike = false;
                String sSpeed1 = sharedPreferences.getString("setting_walk", "");
                if (sSpeed1 == null) {
                    mSpeed = Double.parseDouble(getResources().getString(R.string.setting_walk_default));
                } else {
                    mSpeed = Double.parseDouble(sSpeed1);
                }
                mListener.setCurrentSpeed(mSpeed);
            }
        });

        isRun = false;
        btnRun = mJoystickView.findViewById(R.id.joystick_run);
        btnRun.setOnClickListener(v -> {
            if (!isRun) {
                btnRun.setImageResource(R.drawable.ic_run_pressed);
                isRun = true;
                btnWalk.setImageResource(R.drawable.ic_walk);
                isWalk = false;
                btnBike.setImageResource(R.drawable.ic_bike);
                isBike = false;
                String sSpeed12 = sharedPreferences.getString("setting_run", "");
                if (sSpeed12 == null) {
                    mSpeed = Double.parseDouble(getResources().getString(R.string.setting_run_default));
                } else {
                    mSpeed = Double.parseDouble(sSpeed12);
                }
                mListener.setCurrentSpeed(mSpeed);
            }
        });

        isBike = false;
        btnBike = mJoystickView.findViewById(R.id.joystick_bike);
        btnBike.setOnClickListener(v -> {
            if (!isBike) {
                btnBike.setImageResource(R.drawable.ic_bike_pressed);
                isBike = true;
                btnWalk.setImageResource(R.drawable.ic_walk);
                isWalk = false;
                btnRun.setImageResource(R.drawable.ic_run);
                isRun = false;
                String sSpeed13 = sharedPreferences.getString("setting_bike", "");
                if (sSpeed13 == null) {
                    mSpeed = Double.parseDouble(getResources().getString(R.string.setting_bike_default));
                } else {
                    mSpeed = Double.parseDouble(sSpeed13);
                }
                mListener.setCurrentSpeed(mSpeed);
            }
        });

        ButtonView btnView = mJoystickView.findViewById(R.id.joystick_view);
        btnView.setListener((auto, angle, r) -> {
            if (r <= 0) {
                time.cancel();
            } else {
                mAngle = angle;
                mSpeed = mSpeed * r;
                if (auto) {
                    time.start();
                } else {
                    time.cancel();
                    mListener.clickAngleInfo(mAngle, mSpeed);
                }
            }
        });
    }

    @SuppressLint({"InflateParams", "ClickableViewAccessibility"})
    private void initJoyStickLatLngView() {
        mLatLngView = (LinearLayout)inflater.inflate(R.layout.joystick_latlng, null);
        mLatLngView.setOnTouchListener(new JoyStickOnTouchListener());


        Button btnOk = mLatLngView.findViewById(R.id.joystick_latlng_ok);
        btnOk.setOnClickListener(v -> {
            if (mLatLngView.getParent() != null) {
                mWindowManager.removeView(mLatLngView);
            }

            if (mJoystickView.getParent() == null) {
                mWindowParams.format = PixelFormat.RGBA_8888;
                mWindowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                mWindowParams.gravity = Gravity.START | Gravity.TOP;
                mWindowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
                mWindowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
                mWindowParams.x = 300;
                mWindowParams.y = 300;

                mWindowManager.addView(mJoystickView, mWindowParams);
            }

        });
        Button btnCancel = mLatLngView.findViewById(R.id.joystick_latlng_cancel);
        btnCancel.setOnClickListener(v -> {
            if (mLatLngView.getParent() != null) {
                mWindowManager.removeView(mLatLngView);
            }

            if (mJoystickView.getParent() == null) {
                mWindowParams.format = PixelFormat.RGBA_8888;
                mWindowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                mWindowParams.gravity = Gravity.START | Gravity.TOP;
                mWindowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
                mWindowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
                mWindowParams.x = 300;
                mWindowParams.y = 300;

                mWindowManager.addView(mJoystickView, mWindowParams);
            }
        });

    }

    public void show() {
        if (mLatLngView.getParent() != null) {
            mWindowManager.removeView(mLatLngView);
        }

        if (mJoystickView.getParent() == null) {
            mWindowManager.addView(mJoystickView, mWindowParams);
        }
    }
    
    public void hide() {
        if (mLatLngView.getParent() != null) {
            mWindowManager.removeView(mLatLngView);
        }

        if (mJoystickView.getParent() != null) {
            mWindowManager.removeView(mJoystickView);
        }
    }

    public void setListener(JoyStickClickListener mListener) {
        this.mListener = mListener;
    }

    private class JoyStickOnTouchListener implements OnTouchListener {
        private int x;
        private int y;

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    x = (int) event.getRawX();
                    y = (int) event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    int nowX = (int) event.getRawX();
                    int nowY = (int) event.getRawY();
                    int movedX = nowX - x;
                    int movedY = nowY - y;
                    x = nowX;
                    y = nowY;
                    mWindowParams.x = mWindowParams.x + movedX;
                    mWindowParams.y = mWindowParams.y + movedY;
                    mWindowManager.updateViewLayout(view, mWindowParams);
                    break;
                case MotionEvent.ACTION_UP:
                    view.performClick();
                    break;
                default:
                    break;
            }
            return false;
        }
    }

    public interface JoyStickClickListener {
        void clickAngleInfo(double angle, double speed);

        void setCurrentSpeed(double speed);
    }

    class TimeCount extends CountDownTimer {
        public TimeCount(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);//参数依次为总时长,和计时的时间间隔
        }

        @Override
        public void onFinish() {//计时完毕时触发
            mListener.clickAngleInfo(mAngle, mSpeed);
            time.start();
        }

        @Override
        public void onTick(long millisUntilFinished) { //计时过程显示

        }
    }

}