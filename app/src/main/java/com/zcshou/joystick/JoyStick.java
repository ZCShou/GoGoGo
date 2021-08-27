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
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.SearchView;

import androidx.preference.PreferenceManager;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.zcshou.gogogo.R;
import com.zcshou.utils.MapUtils;

public class JoyStick extends View {
    private static final int DivGo = 1000;    /* 移动的时间间隔，单位 ms */
    private final Context mContext;

    private WindowManager.LayoutParams mWindowParamJoyStick;
    private WindowManager.LayoutParams mWindowParamMap;
//    private WindowManager.LayoutParams mWindowParamHistory;
    private WindowManager mWindowManager;
    private final LayoutInflater inflater;
    private View mJoystickLayout;
    private FrameLayout mMapLayout;
//    private LinearLayout mHistoryLayout;
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
    private final SharedPreferences sharedPreferences;

    private final BitmapDescriptor mMapIndicator = BitmapDescriptorFactory.fromResource(R.drawable.icon_position);
    private MapView mMapView;
    private BaiduMap mBaiduMap;
    private double mLng;
    private double mLat;
    private LatLng mCurMapLngLat;
    private WINDOW_TYPE mCurWin = WINDOW_TYPE.JOYSTICK;

    public JoyStick(Context context) {
        super(context);
        this.mContext = context;

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

        initWindowManager();

        inflater = LayoutInflater.from(mContext);

        if (inflater != null) {
            initJoyStickView();

            initJoyStickMapView();

//            initHistoryView();
        }
    }

    public JoyStick(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

        initWindowManager();

        inflater = LayoutInflater.from(mContext);

        if (inflater != null) {
            initJoyStickView();

            initJoyStickMapView();

//            initHistoryView();
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

            initJoyStickMapView();

//            initHistoryView();
        }
    }

    public void setCurrentPosition(double lng, double lat) {

        double[] lngLat = MapUtils.wgs2bd09(lng, lat);
        mLng = lngLat[0];
        mLat = lngLat[1];

        MyLocationData locData = new MyLocationData.Builder()
                .latitude(mLat)
                .longitude(mLng)
                .build();
        mBaiduMap.setMyLocationData(locData);

        LatLng pos = new LatLng(mLat, mLng);
        MapStatus.Builder builder = new MapStatus.Builder();
        builder.target(pos).zoom(18.0f);
        mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
    }

    public void show() {
        switch (mCurWin) {
            case MAP:
                if (mJoystickLayout.getParent() != null) {
                    mWindowManager.removeView(mJoystickLayout);
                }
//                if (mHistoryLayout.getParent() != null) {
//                    mWindowManager.removeView(mHistoryLayout);
//                }
                if (mMapLayout.getParent() == null) {
                    mWindowManager.addView(mMapLayout, mWindowParamMap);
                }
                break;
//            case HISTORY:
//                if (mMapLayout.getParent() != null) {
//                    mWindowManager.removeView(mMapLayout);
//                }
//                if (mJoystickLayout.getParent() != null) {
//                    mWindowManager.removeView(mJoystickLayout);
//                }
//                if (mHistoryLayout.getParent() == null) {
//                    mWindowManager.addView(mHistoryLayout, mWindowParamHistory);
//                }
//                break;
            case JOYSTICK:
                if (mMapLayout.getParent() != null) {
                    mWindowManager.removeView(mMapLayout);
                }
//                if (mHistoryLayout.getParent() != null) {
//                    mWindowManager.removeView(mHistoryLayout);
//                }
                if (mJoystickLayout.getParent() == null) {
                    mWindowManager.addView(mJoystickLayout, mWindowParamJoyStick);
                }
                break;
        }
    }

    public void hide() {
        if (mMapLayout.getParent() != null) {
            mWindowManager.removeViewImmediate(mMapLayout);
        }

        if (mJoystickLayout.getParent() != null) {
            mWindowManager.removeViewImmediate(mJoystickLayout);
        }

//        if (mHistoryLayout.getParent() != null) {
//            mWindowManager.removeViewImmediate(mHistoryLayout);
//        }
    }

    public void destroy() {
        if (mMapLayout.getParent() != null) {
            mWindowManager.removeViewImmediate(mMapLayout);
        }

        if (mJoystickLayout.getParent() != null) {
            mWindowManager.removeViewImmediate(mJoystickLayout);
        }

//        if (mHistoryLayout.getParent() != null) {
//            mWindowManager.removeViewImmediate(mHistoryLayout);
//        }

        mBaiduMap.setMyLocationEnabled(false);
        mMapView.onDestroy();
    }

    public void setListener(JoyStickClickListener mListener) {
        this.mListener = mListener;
    }

    private void initWindowManager() {
        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        mWindowParamJoyStick = new WindowManager.LayoutParams();
        mWindowParamJoyStick.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        mWindowParamJoyStick.format = PixelFormat.RGBA_8888;
        mWindowParamJoyStick.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE      // 不添加这个将导致游戏无法启动（MIUI12）,添加之后导致键盘无法显示
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        mWindowParamJoyStick.gravity = Gravity.START | Gravity.TOP;
        mWindowParamJoyStick.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowParamJoyStick.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowParamJoyStick.x = 300;
        mWindowParamJoyStick.y = 300;

        mWindowParamMap = mWindowParamJoyStick;
//        mWindowParamHistory = mWindowParamJoyStick;
    }

    @SuppressLint("InflateParams")
    private void initJoyStickView() {
        /* 移动计时器 */
        time = new TimeCount(DivGo, DivGo);
        // 获取参数区设置的速度
        mSpeed = Double.parseDouble(sharedPreferences.getString("setting_walk", getResources().getString(R.string.setting_walk_default)));

        mJoystickLayout = inflater.inflate(R.layout.joystick, null);

        /* 整个摇杆拖动事件处理 */
        mJoystickLayout.setOnTouchListener(new JoyStickOnTouchListener());

        /* 位置按钮点击事件处理 */
        ImageButton btnPosition = mJoystickLayout.findViewById(R.id.joystick_position);
        btnPosition.setOnClickListener(v -> {
            if (mMapLayout.getParent() == null) {
                mCurWin = WINDOW_TYPE.MAP;
                show();
                MapStatus.Builder builder = new MapStatus.Builder();
                builder.target(mCurMapLngLat).zoom(18.0f);
                mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
            }
        });

//        /* 历史按钮点击事件处理 */
//        ImageButton btnHistory = mJoystickLayout.findViewById(R.id.joystick_history);
//        btnHistory.setOnClickListener(v -> {
//            if (mHistoryLayout.getParent() == null) {
//                mCurWin = WINDOW_TYPE.HISTORY;
//                show();
//            }
//        });

        /* 步行按键的点击处理 */
        btnWalk = mJoystickLayout.findViewById(R.id.joystick_walk);
        btnWalk.setOnClickListener(v -> {
            if (!isWalk) {
                btnWalk.setColorFilter(getResources().getColor(R.color.colorAccent, mContext.getTheme()));
                isWalk = true;
                btnRun.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                isRun = false;
                btnBike.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                isBike = false;
                mSpeed = Double.parseDouble(sharedPreferences.getString("setting_walk", getResources().getString(R.string.setting_walk_default)));
            }
        });
        /* 默认为步行 */
        isWalk = true;
        btnWalk.setColorFilter(getResources().getColor(R.color.colorAccent, mContext.getTheme()));
        /* 跑步按键的点击处理 */
        isRun = false;
        btnRun = mJoystickLayout.findViewById(R.id.joystick_run);
        btnRun.setOnClickListener(v -> {
            if (!isRun) {
                btnRun.setColorFilter(getResources().getColor(R.color.colorAccent, mContext.getTheme()));
                isRun = true;
                btnWalk.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                isWalk = false;
                btnBike.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                isBike = false;
                mSpeed = Double.parseDouble(sharedPreferences.getString("setting_run", getResources().getString(R.string.setting_run_default)));
            }
        });
        /* 自行车按键的点击处理 */
        isBike = false;
        btnBike = mJoystickLayout.findViewById(R.id.joystick_bike);
        btnBike.setOnClickListener(v -> {
            if (!isBike) {
                btnBike.setColorFilter(getResources().getColor(R.color.colorAccent, mContext.getTheme()));
                isBike = true;
                btnWalk.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                isWalk = false;
                btnRun.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                isRun = false;
                mSpeed = Double.parseDouble(sharedPreferences.getString("setting_bike", getResources().getString(R.string.setting_bike_default)));
            }
        });
        /* 方向键点击处理 */
        RockerView rckView = mJoystickLayout.findViewById(R.id.joystick_rocker);
        rckView.setListener(this::processDirection);

        /* 方向键点击处理 */
        ButtonView btnView = mJoystickLayout.findViewById(R.id.joystick_button);
        btnView.setListener(this::processDirection);

        /* 这里用来决定摇杆类型 */
        if (sharedPreferences.getString("setting_joystick_type", "0").equals("0")) {
            rckView.setVisibility(VISIBLE);
            btnView.setVisibility(GONE);
        } else {
            rckView.setVisibility(GONE);
            btnView.setVisibility(VISIBLE);
        }
    }

    @SuppressLint({"InflateParams", "ClickableViewAccessibility"})
    private void initJoyStickMapView() {
        mMapLayout = (FrameLayout)inflater.inflate(R.layout.joystick_map, null);
        mMapLayout.setOnTouchListener(new JoyStickOnTouchListener());

        TextView tips = mMapLayout.findViewById(R.id.joystick_map_tips);
        SearchView mSearchView = mMapLayout.findViewById(R.id.joystick_map_searchView);
        mSearchView.setOnSearchClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                tips.setVisibility(GONE);
            }
        });
        mSearchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                tips.setVisibility(VISIBLE);
                return false;       /* 这里必须返回false，否则需要自行处理搜索框的折叠 */
            }
        });

        ImageButton btnOk = mMapLayout.findViewById(R.id.btnGo);
        btnOk.setOnClickListener(v -> {
            mCurWin = WINDOW_TYPE.JOYSTICK;
            show();
            mListener.onPositionInfo(mLng, mLat);
        });
        btnOk.setColorFilter(getResources().getColor(R.color.colorAccent, mContext.getTheme()));

        ImageButton btnCancel = mMapLayout.findViewById(R.id.map_close);
        btnCancel.setOnClickListener(v -> {
            mCurWin = WINDOW_TYPE.JOYSTICK;
            show();
        });

        mMapView = mMapLayout.findViewById(R.id.map_joystick);
        mMapView.showZoomControls(false);
        mBaiduMap = mMapView.getMap();
        mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
        mBaiduMap.setMyLocationEnabled(true);

        mBaiduMap.setOnMapTouchListener(event -> {

        });
        mBaiduMap.setOnMapClickListener(new BaiduMap.OnMapClickListener() {
            /**
             * 单击地图
             */
            public void onMapClick(LatLng point) {
                mCurMapLngLat = point;
                MarkerOptions ooA = new MarkerOptions().position(mCurMapLngLat).icon(mMapIndicator);
                mBaiduMap.clear();
                mBaiduMap.addOverlay(ooA);
                /*  */
                double[] lngLat = MapUtils.bd2wgs(mCurMapLngLat.longitude, mCurMapLngLat.latitude);
                mLng = lngLat[0];
                mLat = lngLat[1];
            }

            /**
             * 单击地图中的POI点
             */
            public void onMapPoiClick(MapPoi poi) {
                mCurMapLngLat = poi.getPosition();
                MarkerOptions ooA = new MarkerOptions().position(mCurMapLngLat).icon(mMapIndicator);
                mBaiduMap.clear();
                mBaiduMap.addOverlay(ooA);
                /*  */
                double[] lngLat = MapUtils.bd2wgs(mCurMapLngLat.longitude, mCurMapLngLat.latitude);
                mLng = lngLat[0];
                mLat = lngLat[1];
            }
        });

        mBaiduMap.setOnMapLongClickListener(new BaiduMap.OnMapLongClickListener() {
            /**
             * 长按地图
             */
            public void onMapLongClick(LatLng point) {
                mCurMapLngLat = point;
                MarkerOptions ooA = new MarkerOptions().position(mCurMapLngLat).icon(mMapIndicator);
                mBaiduMap.clear();
                mBaiduMap.addOverlay(ooA);
                /*  */
                double[] lngLat = MapUtils.bd2wgs(mCurMapLngLat.longitude, mCurMapLngLat.latitude);
                mLng = lngLat[0];
                mLat = lngLat[1];
            }
        });

        mBaiduMap.setOnMapDoubleClickListener(new BaiduMap.OnMapDoubleClickListener() {
            /**
             * 双击地图
             */
            public void onMapDoubleClick(LatLng point) {
                mCurMapLngLat = point;
                MarkerOptions ooA = new MarkerOptions().position(mCurMapLngLat).icon(mMapIndicator);
                mBaiduMap.clear();
                mBaiduMap.addOverlay(ooA);
                /*  */
                double[] lngLat = MapUtils.bd2wgs(mCurMapLngLat.longitude, mCurMapLngLat.latitude);
                mLng = lngLat[0];
                mLat = lngLat[1];
            }
        });
    }

//    @SuppressLint({"ClickableViewAccessibility", "InflateParams"})
//    private void initHistoryView() {
//        mHistoryLayout = (LinearLayout)inflater.inflate(R.layout.input_latlng, null);
//        mHistoryLayout.setOnTouchListener(new JoyStickOnTouchListener());
//    }

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
                mListener.onMoveInfo(mSpeed, disLng, disLat);
            }
        }
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
                    switch (mCurWin) {
                        case MAP:
                            mWindowParamMap.x = mWindowParamMap.x + movedX;
                            mWindowParamMap.y = mWindowParamMap.y + movedY;
                            mWindowManager.updateViewLayout(view, mWindowParamMap);
                            break;
//                        case HISTORY:
//                            mWindowParamHistory.x = mWindowParamHistory.x + movedX;
//                            mWindowParamHistory.y = mWindowParamHistory.y + movedY;
//                            mWindowManager.updateViewLayout(view, mWindowParamHistory);
//                            break;
                        case JOYSTICK:
                            mWindowParamJoyStick.x = mWindowParamJoyStick.x + movedX;
                            mWindowParamJoyStick.y = mWindowParamJoyStick.y + movedY;
                            mWindowManager.updateViewLayout(view, mWindowParamJoyStick);
                            break;
                    }
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
        void onMoveInfo(double speed, double disLng, double disLat);

        void onPositionInfo(double lng, double lat);
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
            mListener.onMoveInfo(mSpeed, disLng, disLat);
            time.start();
        }

        @Override
        public void onTick(long millisUntilFinished) { //计时过程显示

        }
    }

    public enum WINDOW_TYPE {
        JOYSTICK,
        MAP,
//        HISTORY
    }
}