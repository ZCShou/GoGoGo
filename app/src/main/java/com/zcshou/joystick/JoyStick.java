package com.zcshou.joystick;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
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

import com.zcshou.gogogo.R;

public class JoyStick extends View {

    private Context mContext;
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
    boolean isAuto;
    double mAngle;
    double mSpeed;

    public JoyStick(Context context) {
        super(context);
        this.mContext = context;

        initWindowManager();

        inflater = LayoutInflater.from(mContext);

        if (inflater != null) {
            initJoyStickView();

            initJoyStickLatLngView();
        }

    }

    public JoyStick(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        initWindowManager();

        inflater = LayoutInflater.from(mContext);

        if (inflater != null) {
            initJoyStickView();

            initJoyStickLatLngView();
        }
    }

    public JoyStick(Context context, AttributeSet attrs) {
        super(context, attrs);

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

        if (Build.VERSION.SDK_INT >= 26) {//8.0新特性
            mWindowParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            mWindowParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }

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
        isAuto = false;
        mSpeed = 1.3;

        mJoystickView = inflater.inflate(R.layout.joystick, null);
        mJoystickView.setOnTouchListener(new JoyStickOnTouchListener());

        btnInput = mJoystickView.findViewById(R.id.joystick_input);
        btnInput.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
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
            }
        });

        isWalk = true;
        btnWalk = mJoystickView.findViewById(R.id.joystick_walk);
        btnWalk.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isWalk) {
                    btnWalk.setImageResource(R.drawable.ic_walk_pressed);
                    isWalk = true;
                    btnRun.setImageResource(R.drawable.ic_run);
                    isRun = false;
                    btnBike.setImageResource(R.drawable.ic_bike);
                    isBike = false;
//                    mSpeed = sharedPref.getFloat("setting_walk", (float) 0.00003);
                    mSpeed = 1.3;
                    //DisplayToast("Speed:" + mSpeed);
                    mListener.setCurrentSpeed(mSpeed);
                }
            }
        });

        isRun = false;
        btnRun = mJoystickView.findViewById(R.id.joystick_run);
        btnRun.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isRun) {
                    btnRun.setImageResource(R.drawable.ic_run_pressed);
                    isRun = true;
                    btnWalk.setImageResource(R.drawable.ic_walk);
                    isWalk = false;
                    btnBike.setImageResource(R.drawable.ic_bike);
                    isBike = false;
//                    mSpeed = sharedPref.getFloat("setting_run", (float) 0.00006);
                    mSpeed = 4.0;
                    //DisplayToast("Speed:" + mSpeed);
                    mListener.setCurrentSpeed(mSpeed);
                }
            }
        });

        isBike = false;
        btnBike = mJoystickView.findViewById(R.id.joystick_bike);
        btnBike.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isBike) {
                    btnBike.setImageResource(R.drawable.ic_bike_pressed);
                    isBike = true;
                    btnWalk.setImageResource(R.drawable.ic_walk);
                    isWalk = false;
                    btnRun.setImageResource(R.drawable.ic_run);
                    isRun = false;
//                    mSpeed = sharedPref.getFloat("setting_bike", (float) 0.00009);
                    mSpeed = 12.0;
                    //DisplayToast("Speed:" + mSpeed);
                    mListener.setCurrentSpeed(mSpeed);
                }
            }
        });

        ButtonView btnView = mJoystickView.findViewById(R.id.joystick_view);
        btnView.setListener(new ButtonView.ButtonViewClickListener() {
            @Override
            public void clickCenter() {
                if (isAuto) {
                    isAuto = false;
                    time.cancel();
                } else {
                    isAuto = true;
                }
            }

            @Override
            public void clickAngleInfo(double angle, double r) {
                mAngle = angle;
                mSpeed = mSpeed * r;
                if (isAuto) {
                    time.start();
                } else {
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
        btnOk.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
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

            }
        });
        Button btnCancel = mLatLngView.findViewById(R.id.joystick_latlng_cancel);
        btnCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
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

//     public void DisplayToast(String str) {
//         Toast toast = Toast.makeText(mContext, str, Toast.LENGTH_LONG);
//         toast.setGravity(Gravity.TOP, 0, 220);
//         toast.show();
//     }
}