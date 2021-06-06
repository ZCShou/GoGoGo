package com.zcshou.joystick;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.CountDownTimer;
import android.util.AttributeSet;
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
    private static final int DivGo = 1000;    /* 移动的时间间隔，单位 ms */
    final private Context mContext;

    private WindowManager.LayoutParams mWindowParams;
    private WindowManager mWindowManager;
    private final LayoutInflater inflater;
    private View mJoystickView;
    private LinearLayout mLatLngView;
    private JoyStickClickListener mListener;
    private boolean isWalk;
    private ImageButton btnWalk;
    private boolean isRun;
    private ImageButton btnRun;
    private boolean isBike;
    private ImageButton btnBike;

    // 移动
    private TimeCount time;
    private boolean isMove;
    private double mSpeed = 1.2;        /* 默认的速度，单位 m/s */
    private double mAngle = 0;
    private double mR = 0;
    private double disLng = 0;
    private double disLat = 0;
    private SharedPreferences sharedPreferences;

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
    }

    @SuppressLint("InflateParams")
    private void initJoyStickView() {
        /* 移动计时器 */
        time = new TimeCount(DivGo, DivGo);
        // 获取参数区设置的速度
        mSpeed = Double.parseDouble(sharedPreferences.getString("setting_walk", getResources().getString(R.string.setting_walk_default)));

        mJoystickView = inflater.inflate(R.layout.joystick, null);
        /* 整个摇杆拖动事件处理 */
        mJoystickView.setOnTouchListener(new JoyStickOnTouchListener());
        /* 输入按钮点击事件处理 */
        // 控制按键相关
        ImageButton btnInput = mJoystickView.findViewById(R.id.joystick_input);
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
        /* 步行按键的点击处理 */
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
                mSpeed = Double.parseDouble(sharedPreferences.getString("setting_walk", getResources().getString(R.string.setting_walk_default)));
            }
        });
        /* 跑步按键的点击处理 */
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
                mSpeed = Double.parseDouble(sharedPreferences.getString("setting_run", getResources().getString(R.string.setting_run_default)));
            }
        });
        /* 自行车按键的点击处理 */
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
                mSpeed = Double.parseDouble(sharedPreferences.getString("setting_bike", getResources().getString(R.string.setting_bike_default)));
            }
        });
        /* 方向键点击处理 */
        RockerView rckView = mJoystickView.findViewById(R.id.joystick_rocker);
        rckView.setListener(this::processDirection);

        /* 方向键点击处理 */
        ButtonView btnView = mJoystickView.findViewById(R.id.joystick_button);
        btnView.setListener(this::processDirection);

        /* 这里用来绝对摇杆类型 */
        if (sharedPreferences.getString("joystick_type", "0").equals("0")) {
            rckView.setVisibility(VISIBLE);
            btnView.setVisibility(GONE);
        } else {
            rckView.setVisibility(GONE);
            btnView.setVisibility(VISIBLE);
        }
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

    private void processDirection(boolean auto, double angle, double r) {
        if (r <= 0) {
            time.cancel();
            isMove = false;
        } else {
            mAngle = angle;
            mR = r;
            if (auto) {
                if (!isMove) {
                    time.start();
                    isMove = true;
                }
            } else {
                time.cancel();
                isMove = false;
                // 注意：这里的 x y 与 圆中角度的对应问题（以 X 轴正向为 0 度）且转换为 km
                disLng = mSpeed * (double)(DivGo / 1000) * mR * Math.cos(mAngle * 2 * Math.PI / 360) / 1000;// 注意安卓中的三角函数使用的是弧度
                disLat = mSpeed * (double)(DivGo / 1000) * mR * Math.sin(mAngle * 2 * Math.PI / 360) / 1000;// 注意安卓中的三角函数使用的是弧度
                mListener.moveInfo(disLng, disLat);
            }
        }
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
        void moveInfo(double disLng, double disLat);
    }

    class TimeCount extends CountDownTimer {
        public TimeCount(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);//参数依次为总时长,和计时的时间间隔
        }

        @Override
        public void onFinish() {//计时完毕时触发
            // 注意：这里的 x y 与 圆中角度的对应问题（以 X 轴正向为 0 度）且转换为 km
            disLng = mSpeed * (double)(DivGo / 1000) * mR * Math.cos(mAngle * 2 * Math.PI / 360) / 1000;// 注意安卓中的三角函数使用的是弧度
            disLat = mSpeed * (double)(DivGo / 1000) * mR * Math.sin(mAngle * 2 * Math.PI / 360) / 1000;// 注意安卓中的三角函数使用的是弧度
            mListener.moveInfo(disLng, disLat);
            time.start();
        }

        @Override
        public void onTick(long millisUntilFinished) { //计时过程显示

        }
    }
}