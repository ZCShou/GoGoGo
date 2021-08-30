package com.zcshou.gogogo;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.preference.PreferenceManager;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.sug.SuggestionSearch;
import com.baidu.mapapi.search.sug.SuggestionSearchOption;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.zcshou.service.ServiceGo;
import com.zcshou.database.DataBaseHistoryLocation;
import com.zcshou.database.DataBaseHistorySearch;
import com.zcshou.service.GoSntpClient;
import com.zcshou.utils.ShareUtils;
import com.zcshou.utils.GoUtils;
import com.zcshou.utils.MapUtils;

import static android.view.View.GONE;

import com.elvishew.xlog.XLog;

public class MainActivity extends BaseActivity
        implements SensorEventListener {
    /* 对外 */
    public static final String LAT_MSG_ID = "LAT_VALUE";
    public static final String LNG_MSG_ID = "LNG_VALUE";

    public static final String POI_NAME = "POI_NAME";
    public static final String POI_ADDRESS = "POI_ADDRESS";
    public static final String POI_LONGITUDE = "POI_LONGITUDE";
    public static final String POI_LATITUDE = "POI_LATITUDE";


    // 百度地图相关
    private MapView mMapView;
    private static BaiduMap mBaiduMap = null;
    private LocationClient mLocClient = null;
    private String mCurrentCity = null;
    private double mCurrentLat = 0.0;
    private double mCurrentLon = 0.0;
    private float mCurrentDirection = 0.0f;
    public static LatLng mCurLatLngMap = new LatLng(36.547743718042415, 117.07018449827267);
    public static BitmapDescriptor mMapIndicator = BitmapDescriptorFactory.fromResource(R.drawable.icon_gcoding);
    private boolean isFirstLoc = true; // 是否首次定位
    private static double mCurLat = ServiceGo.DEFAULT_LAT;  /* WGS84 坐标系的纬度 */
    private static double mCurLng = ServiceGo.DEFAULT_LNG;  /* WGS84 坐标系的经度 */
    private SensorManager mSensorManager;
    private Sensor mSensorAccelerometer;
    private Sensor mSensorMagnetic;
    //加速度传感器数据
    float[] mAccValues = new float[3];
    //地磁传感器数据
    float[] mMagValues = new float[3];
    //旋转矩阵，用来保存磁场和加速度的数据
    float[] mR = new float[9];
    //模拟方向传感器的数据（原始数据为弧度）
    float[] mDirectionValues = new float[3];
    // http
    private RequestQueue mRequestQueue;

    // 历史记录数据库
    private SQLiteDatabase mLocationHistoryDB;
    private SQLiteDatabase mSearchHistoryDB;

    // UI相关
    NavigationView mNavigationView;
    CheckBox mPtlCheckBox;
    FloatingActionButton mButtonStart;
    //位置搜索相关
    private SearchView searchView;
    private ListView mSearchList;
    private ListView mSearchHistoryList;
    private LinearLayout mSearchLayout;
    private LinearLayout mHistoryLayout;
    private MenuItem searchItem;
    private SuggestionSearch mSuggestionSearch;

    private boolean isLimit = true;
    private static final long mTS = 1636588801;
    private boolean isMockServStart = false;
    private boolean isMove = false;
    private ServiceGo.ServiceGoBinder mServiceBinder;
    private ServiceConnection mConnection;
    private SharedPreferences sharedPreferences;
    private final JSONObject mReg = new JSONObject();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        XLog.i("MainActivity: onCreate");

        //sqlite相关
        initStoreHistory();

        //http init
        mRequestQueue = Volley.newRequestQueue(this);

        initBaiduMap();

        initBaiduLocation();

        // 地图上按键的监听
        initListenerMapBtn();

        initNavigationView();

        initSearchView();

        setGoBtnListener();

        TimeTask timeTask = new TimeTask();
        ExecutorService threadExecutor = Executors.newSingleThreadExecutor();
        threadExecutor.submit(timeTask);

        mConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mServiceBinder = (ServiceGo.ServiceGoBinder)service;
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };
    }

    @Override
    protected void onPause() {
        XLog.i("MainActivity: onPause");
        mMapView.onPause();
        mSensorManager.unregisterListener(this);
        super.onPause();
    }

    @Override
    protected void onResume() {
        XLog.i("MainActivity: onResume");
        mMapView.onResume();
        mSensorManager.registerListener(this, mSensorAccelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mSensorMagnetic, SensorManager.SENSOR_DELAY_UI);
        super.onResume();
    }

    @Override
    protected void onStop() {
        XLog.i("MainActivity: onStop");
        //取消注册传感器监听
        mSensorManager.unregisterListener(this);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        XLog.i("MainActivity: onDestroy");

        if (isMockServStart) {
            unbindService(mConnection); // 解绑服务，服务要记得解绑，不要造成内存泄漏
            Intent serviceGoIntent = new Intent(MainActivity.this, ServiceGo.class);
            stopService(serviceGoIntent);
        }

        mSensorManager.unregisterListener(this);

        // 退出时销毁定位
        mLocClient.stop();
        // 关闭定位图层
        mBaiduMap.setMyLocationEnabled(false);
        mMapView.onDestroy();

        //poi search destroy
        mSuggestionSearch.destroy();

        //close db
        mLocationHistoryDB.close();
        mSearchHistoryDB.close();

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(false);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        //找到searchView
        searchItem = menu.findItem(R.id.action_search);
        searchView = (SearchView) searchItem.getActionView();
        searchView.setIconified(false);// 设置searchView处于展开状态
        searchView.onActionViewExpanded();// 当展开无输入内容的时候，没有关闭的图标
        searchView.setIconifiedByDefault(true);//默认为true在框内，设置false则在框外
        searchView.setSubmitButtonEnabled(false);//显示提交按钮
        searchItem.setOnActionExpandListener(new  MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                mSearchLayout.setVisibility(View.INVISIBLE);
                mHistoryLayout.setVisibility(View.INVISIBLE);
                return true;  // Return true to collapse action view
            }
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                mSearchLayout.setVisibility(View.INVISIBLE);
                //展示搜索历史
                List<Map<String, Object>> data = getSearchHistory();

                if (data.size() > 0) {
                    SimpleAdapter simAdapt = new SimpleAdapter(
                            MainActivity.this,
                            data,
                            R.layout.search_record_item,
                            new String[] {DataBaseHistorySearch.DB_COLUMN_KEY,
                                    DataBaseHistorySearch.DB_COLUMN_DESCRIPTION,
                                    DataBaseHistorySearch.DB_COLUMN_TIMESTAMP,
                                    DataBaseHistorySearch.DB_COLUMN_IS_LOCATION,
                                    DataBaseHistorySearch.DB_COLUMN_LONGITUDE_CUSTOM,
                                    DataBaseHistorySearch.DB_COLUMN_LATITUDE_CUSTOM},
                            new int[] {R.id.search_key,
                                    R.id.search_description,
                                    R.id.search_timestamp,
                                    R.id.search_isLoc,
                                    R.id.search_longitude,
                                    R.id.search_latitude});
                    mSearchHistoryList.setAdapter(simAdapt);
                    mHistoryLayout.setVisibility(View.VISIBLE);
                }

                return true;  // Return true to expand action view
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                try {
                    mSuggestionSearch.requestSuggestion((new SuggestionSearchOption())
                            .keyword(query)
                            .city(mCurrentCity)
                    );
                    //搜索历史 插表参数
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(DataBaseHistorySearch.DB_COLUMN_KEY, query);
                    contentValues.put(DataBaseHistorySearch.DB_COLUMN_DESCRIPTION, "搜索关键字");
                    contentValues.put(DataBaseHistorySearch.DB_COLUMN_IS_LOCATION, DataBaseHistorySearch.DB_SEARCH_TYPE_KEY);
                    contentValues.put(DataBaseHistorySearch.DB_COLUMN_TIMESTAMP, System.currentTimeMillis() / 1000);

                    DataBaseHistorySearch.saveHistorySearch(mSearchHistoryDB, contentValues);
                    mBaiduMap.clear();
                    mSearchLayout.setVisibility(View.INVISIBLE);
                } catch (Exception e) {
                    DisplayToast("搜索失败，请检查网络连接");
                    XLog.d("HTTP: 搜索失败，请检查网络连接");
                    e.printStackTrace();
                }

                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                //当输入框内容改变的时候回调
                //搜索历史置为不可见
                mHistoryLayout.setVisibility(View.INVISIBLE);

                if (!newText.equals("")) {
                    try {
                        mSuggestionSearch.requestSuggestion((new SuggestionSearchOption())
                                .keyword(newText)
                                .city(mCurrentCity)
                        );
                    } catch (Exception e) {
                        DisplayToast("搜索失败，请检查网络连接");
                        XLog.d("HTTP: 搜索失败，请检查网络连接");
                        e.printStackTrace();
                    }
                }

                return true;
            }
        });
        return true;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            mAccValues = sensorEvent.values;
        }
        else if(sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
            mMagValues = sensorEvent.values;
        }

        SensorManager.getRotationMatrix(mR, null, mAccValues, mMagValues);
        SensorManager.getOrientation(mR, mDirectionValues);
        mCurrentDirection = (float) Math.toDegrees(mDirectionValues[0]);    // 弧度转角度
        if (mCurrentDirection < 0) {    // 由 -180 ~ + 180 转为 0 ~ 360
            mCurrentDirection += 360;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void initBaiduMap() {
        // 地图初始化
        mMapView = findViewById(R.id.bmapView);
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
                mCurLatLngMap = point;
                //百度坐标系转wgs坐标系
                transformCoordinate(String.valueOf(point.longitude), String.valueOf(point.latitude));
                markSelectedPosition();
            }
            /**
             * 单击地图中的POI点
             */
            public void onMapPoiClick(MapPoi poi) {
                mCurLatLngMap = poi.getPosition();
                //百度坐标系转wgs坐标系
                transformCoordinate(String.valueOf(poi.getPosition().longitude), String.valueOf(poi.getPosition().latitude));
                markSelectedPosition();
            }
        });

        mBaiduMap.setOnMapLongClickListener(new BaiduMap.OnMapLongClickListener() {
            /**
             * 长按地图
             */
            public void onMapLongClick(LatLng point) {
                mCurLatLngMap = point;
                //百度坐标系转wgs坐标系
                transformCoordinate(String.valueOf(point.longitude), String.valueOf(point.latitude));
                markSelectedPosition();
            }
        });

        mBaiduMap.setOnMapDoubleClickListener(new BaiduMap.OnMapDoubleClickListener() {
            /**
             * 双击地图
             */
            public void onMapDoubleClick(LatLng point) {
                mCurLatLngMap = point;
                //百度坐标系转wgs坐标系
                transformCoordinate(String.valueOf(point.longitude), String.valueOf(point.latitude));
                markSelectedPosition();
            }
        });

        mBaiduMap.setOnMapStatusChangeListener(new BaiduMap.OnMapStatusChangeListener() {
            public void onMapStatusChangeStart(MapStatus status) {

            }
            @Override
            public void onMapStatusChangeStart(MapStatus status, int reason) {

            }
            public void onMapStatusChangeFinish(MapStatus status) {

            }
            public void onMapStatusChange(MapStatus status) {

            }
        });

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);// 获取传感器管理服务
        if (mSensorManager != null) {
            mSensorAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (mSensorAccelerometer != null) {
                mSensorManager.registerListener(this, mSensorAccelerometer, SensorManager.SENSOR_DELAY_UI);
            }
            mSensorMagnetic = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            if (mSensorMagnetic != null) {
                mSensorManager.registerListener(this, mSensorMagnetic, SensorManager.SENSOR_DELAY_UI);
            }
        }
    }

    //开启地图的定位图层
    private void initBaiduLocation() {
        // 定位初始化
        mLocClient = new LocationClient(this);
        mLocClient.registerLocationListener(new BDAbstractLocationListener() {
            @Override
            public void onReceiveLocation(BDLocation bdLocation) {
                if (bdLocation == null || mMapView == null) {// mapview 销毁后不在处理新接收的位置
                    return;
                }

                mCurrentCity = bdLocation.getCity();
                mCurrentLat = bdLocation.getLatitude();
                mCurrentLon = bdLocation.getLongitude();
//                mCurrentAccuracy = bdLocation.getRadius();
                MyLocationData locData = new MyLocationData.Builder()
                        .accuracy(bdLocation.getRadius())
                        .direction(mCurrentDirection)// 此处设置开发者获取到的方向信息，顺时针0-360
                        .latitude(bdLocation.getLatitude())
                        .longitude(bdLocation.getLongitude()).build();
                mBaiduMap.setMyLocationData(locData);
                MyLocationConfiguration configuration = new MyLocationConfiguration(MyLocationConfiguration.LocationMode.NORMAL, true, null);
                mBaiduMap.setMyLocationConfiguration(configuration);

                if (isMove) {
                    isMove = false;
                    mBaiduMap.clear();
                    mCurLatLngMap = null;

                    if (GoUtils.isWifiEnabled(MainActivity.this)) {
                        showDisableWifiDialog();
                    }
                }

                /* 如果出现错误，则需要重新请求位置 */
                int err = bdLocation.getLocType();
                if (err == BDLocation.TypeCriteriaException || err == BDLocation.TypeNetWorkException) {
                    mLocClient.requestLocation();   /* 请求位置 */
                } else {
                    if (isFirstLoc) {
                        isFirstLoc = false;
                        // 这里记录百度地图返回的位置
                        mCurLatLngMap = new LatLng(bdLocation.getLatitude(), bdLocation.getLongitude());
                        MapStatus.Builder builder = new MapStatus.Builder();
                        builder.target(mCurLatLngMap).zoom(18.0f);
                        mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));

                        XLog.d("First Baidu LatLng: " + mCurLatLngMap);

                        // 这里将百度地图位置转换为 GPS 坐标
                        double[] latLng = MapUtils.bd2wgs(mCurLatLngMap.longitude, mCurLatLngMap.latitude);
                        mCurLng = latLng[0];
                        mCurLat = latLng[1];
                        XLog.d("First LatLng: " + mCurLng + "   " + mCurLat);
                    }
                }
            }
            /**
             * 错误的状态码
             * <a>http://lbsyun.baidu.com/index.php?title=android-locsdk/guide/addition-func/error-code</a>
             * <p>
             * 回调定位诊断信息，开发者可以根据相关信息解决定位遇到的一些问题
             *
             * @param locType      当前定位类型
             * @param diagnosticType  诊断类型（1~9）
             * @param diagnosticMessage 具体的诊断信息释义
             */
            public void onLocDiagnosticMessage(int locType, int diagnosticType, String diagnosticMessage) {
                XLog.d("Baidu ERROR: " + locType + "-" + diagnosticType + "-" + diagnosticMessage);
            }
        });
        LocationClientOption locationOption = new LocationClientOption();
        //可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
        locationOption.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        //可选，默认gcj02，设置返回的定位结果坐标系，如果配合百度地图使用，建议设置为bd09ll;
        locationOption.setCoorType("bd09ll");
        //可选，默认0，即仅定位一次，设置发起连续定位请求的间隔需要大于等于1000ms才是有效的
        locationOption.setScanSpan(1000);
        //可选，设置是否需要地址信息，默认不需要
        locationOption.setIsNeedAddress(true);
        //可选，设置是否需要设备方向结果
        locationOption.setNeedDeviceDirect(false);
        //可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
        locationOption.setLocationNotify(true);
        //可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
        locationOption.setIgnoreKillProcess(true);
        //可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
        locationOption.setIsNeedLocationDescribe(false);
        //可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
        locationOption.setIsNeedLocationPoiList(false);
        //可选，默认false，设置是否收集CRASH信息，默认收集
        locationOption.SetIgnoreCacheException(true);
        //可选，默认false，设置是否开启Gps定位
        locationOption.setOpenGps(true);
        //可选，默认false，设置定位时是否需要海拔信息，默认不需要，除基础定位版本都可用
        locationOption.setIsNeedAltitude(false);
//        //设置打开自动回调位置模式，该开关打开后，期间只要定位SDK检测到位置变化就会主动回调给开发者，该模式下开发者无需再关心定位间隔是多少，定位SDK本身发现位置变化就会及时回调给开发者
//        locationOption.setOpenAutoNotifyMode();
//        //设置打开自动回调位置模式，该开关打开后，期间只要定位SDK检测到位置变化就会主动回调给开发者
//        locationOption.setOpenAutoNotifyMode(3000,1, LocationClientOption.LOC_SENSITIVITY_HIGHT);
        //需将配置好的LocationClientOption对象，通过setLocOption方法传递给LocationClient对象使用
        mLocClient.setLocOption(locationOption);
        //开始定位
        mLocClient.start();
    }

    //地图上各按键的监听
    private void initListenerMapBtn() {
        RadioGroup mGroupMapType = this.findViewById(R.id.RadioGroupMapType);
        mGroupMapType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.normal) {
                mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
            }

            if (checkedId == R.id.statellite) {
                mBaiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
            }
        });
    }

    public void goCurrentPosition(View view) {
        resetMap();
    }

    //放大地图
    public void zoomInMap(View view) {
        mBaiduMap.animateMapStatus(MapStatusUpdateFactory.zoomIn());
    }

    //缩小地图
    public void zoomOutMap(View view) {
        mBaiduMap.animateMapStatus(MapStatusUpdateFactory.zoomOut());
    }

    public void goInputPosition(View view1) {
        AlertDialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("请输入经度和纬度");
        View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.input_latlng, null);
        builder.setView(view);
        dialog = builder.show();

        EditText dialog_lng = view.findViewById(R.id.joystick_longitude);
        EditText dialog_lat = view.findViewById(R.id.joystick_latitude);
        RadioButton rbBD = view.findViewById(R.id.pos_type_bd);

        Button btnGo = view.findViewById(R.id.joystick_latlng_ok);
        btnGo.setOnClickListener(v -> {
            String dialog_lng_str = dialog_lng.getText().toString();
            String dialog_lat_str = dialog_lat.getText().toString();

            if (TextUtils.isEmpty(dialog_lng_str) || TextUtils.isEmpty(dialog_lat_str)) {
                DisplayToast("输入不能为空");
            } else {
                double dialog_lng_double = Double.parseDouble(dialog_lng_str);
                double dialog_lat_double = Double.parseDouble(dialog_lat_str);

                if (dialog_lng_double > 180.0 || dialog_lng_double < -180.0 || dialog_lat_double > 90.0 || dialog_lat_double < -90.0) {
                    DisplayToast("经纬度超出限制!\n-180.0<经度<180.0\n-90.0<纬度<90.0");
                } else {
                    if (rbBD.isChecked()) {
                        mCurLatLngMap = new LatLng(dialog_lat_double, dialog_lng_double);
                    } else {
                        double[] latLon = MapUtils.wgs2bd09(dialog_lat_double, dialog_lng_double);
                        mCurLatLngMap = new LatLng(latLon[0], latLon[0]);
                    }

                    MapStatusUpdate mapstatusupdate = MapStatusUpdateFactory.newLatLng(mCurLatLngMap);
                    mBaiduMap.setMapStatus(mapstatusupdate);
                    markSelectedPosition();
                    dialog.dismiss();
                }
            }
        });

        Button btnCancel = view.findViewById(R.id.joystick_latlng_cancel);
        btnCancel.setOnClickListener(v -> dialog.dismiss());
    }

    // 在地图上显示历史位置
    public static boolean showHistoryLocation(String bd09Longitude, String bd09Latitude, String wgs84Longitude, String wgs84Latitude) {
        boolean ret = true;

        try {
            if (!bd09Longitude.isEmpty() && !bd09Latitude.isEmpty()) {
                mCurLatLngMap = new LatLng(Double.parseDouble(bd09Latitude), Double.parseDouble(bd09Longitude));
                MarkerOptions ooA = new MarkerOptions().position(mCurLatLngMap).icon(mMapIndicator);
                mBaiduMap.clear();
                mBaiduMap.addOverlay(ooA);
                MapStatusUpdate mapstatusupdate = MapStatusUpdateFactory.newLatLng(mCurLatLngMap);
                mBaiduMap.setMapStatus(mapstatusupdate);
                mCurLng = Double.parseDouble(wgs84Longitude);
                mCurLat = Double.parseDouble(wgs84Latitude);
            }
        } catch (Exception e) {
            ret = false;
            XLog.e("UNKNOWN: showHistoryLocation error");
            e.printStackTrace();
        }

        return ret;
    }

    //标定选择的位置
    private void markSelectedPosition() {
        if (mCurLatLngMap != null) {
            MarkerOptions ooA = new MarkerOptions().position(mCurLatLngMap).icon(mMapIndicator);
            mBaiduMap.clear();
            mBaiduMap.addOverlay(ooA);
        }
    }

    //重置地图
    private void resetMap() {
        mBaiduMap.clear();
        mCurLatLngMap = null;

        mLocClient.requestLocation();   /* 请求位置 */

        //对地图的中心点进行更新
        MapStatusUpdate mapstatusupdate = MapStatusUpdateFactory.newLatLng(new LatLng(mCurrentLat, mCurrentLon));
        mBaiduMap.setMapStatus(mapstatusupdate);
    }

    //坐标转换
    private void transformCoordinate(final String longitude, final String latitude) {
        //参数坐标系：bd09
        final double error = 0.00000001;
        final String safeCode = getResources().getString(R.string.safecode);
        final String ak = getResources().getString(R.string.ak);
        //判断bd09坐标是否在国内
        String mapApiUrl = "https://api.map.baidu.com/geoconv/v1/?coords=" + longitude + "," + latitude +
                "&from=5&to=3&ak=" + ak + "&mcode=" + safeCode;
        XLog.d("transformCoordinate: " + mapApiUrl);
        //bd09坐标转gcj02
        StringRequest stringRequest = new StringRequest(mapApiUrl, response -> {
            try {
                JSONObject getRetJson = new JSONObject(response);
                XLog.d("transformCoordinate:" + getRetJson.toString());

                //如果api接口转换成功
                if (Integer.parseInt(getRetJson.getString("status")) == 0) {
                    XLog.d("HTTP: call api[bd09_to_gcj02] success");
                    JSONArray coordinateArr = getRetJson.getJSONArray("result");
                    JSONObject coordinate = coordinateArr.getJSONObject(0);
                    String gcj02Longitude = coordinate.getString("x");
                    String gcj02Latitude = coordinate.getString("y");
                    XLog.d("bd09Longitude is " + longitude + ", " + "bd09Latitude is " + latitude);
                    XLog.d("gcj02Longitude is " + gcj02Longitude + ", " + "gcj02Latitude is " + gcj02Latitude);
                    BigDecimal bigDecimalGcj02Longitude = BigDecimal.valueOf(Double.parseDouble(gcj02Longitude));
                    BigDecimal bigDecimalGcj02Latitude = BigDecimal.valueOf(Double.parseDouble(gcj02Latitude));
                    BigDecimal bigDecimalBd09Longitude = BigDecimal.valueOf(Double.parseDouble(longitude));
                    BigDecimal bigDecimalBd09Latitude = BigDecimal.valueOf(Double.parseDouble(latitude));
                    double gcj02LongitudeDouble = bigDecimalGcj02Longitude.setScale(9, BigDecimal.ROUND_HALF_UP).doubleValue();
                    double gcj02LatitudeDouble = bigDecimalGcj02Latitude.setScale(9, BigDecimal.ROUND_HALF_UP).doubleValue();
                    double bd09LongitudeDouble = bigDecimalBd09Longitude.setScale(9, BigDecimal.ROUND_HALF_UP).doubleValue();
                    double bd09LatitudeDouble = bigDecimalBd09Latitude.setScale(9, BigDecimal.ROUND_HALF_UP).doubleValue();
                    XLog.d("gcj02LongitudeDouble is " + gcj02LongitudeDouble + ", " + "gcj02LatitudeDouble is " + gcj02LatitudeDouble);
                    XLog.d("bd09LongitudeDouble is " + bd09LongitudeDouble + ", " + "bd09LatitudeDouble is " + bd09LatitudeDouble);

                    //如果bd09转gcj02 结果误差很小  认为该坐标在国外
                    if ((Math.abs(gcj02LongitudeDouble - bd09LongitudeDouble)) <= error && (Math.abs(gcj02LatitudeDouble - bd09LatitudeDouble)) <= error) {
                        //不进行坐标转换
                        mCurLat = Double.parseDouble(latitude);
                        mCurLng = Double.parseDouble(longitude);
                        XLog.d("OUT OF CHN, NO NEED TO TRANSFORM COORDINATE");
                    } else {
                        //离线转换坐标系
                        // double latLng[] = MapUtils.bd2wgs(Double.parseDouble(longitude), Double.parseDouble(latitude));
                        double[] latLng = MapUtils.gcj02towgs84(Double.parseDouble(gcj02Longitude), Double.parseDouble(gcj02Latitude));
                        mCurLng = latLng[0];
                        mCurLat = latLng[1];
                        XLog.d("IN CHN, NEED TO TRANSFORM COORDINATE");
                    }
                }
                //api接口转换失败 认为在国内
                else {
                    //离线转换坐标系
                    double[] latLng = MapUtils.bd2wgs(Double.parseDouble(longitude), Double.parseDouble(latitude));
                    mCurLng = latLng[0];
                    mCurLat = latLng[1];
                    XLog.d("IN CHN, NEED TO TRANSFORM COORDINATE");
                }
            } catch (JSONException e) {
                XLog.e("JSON: resolve json error");
                e.printStackTrace();
                //离线转换坐标系
                double[] latLng = MapUtils.bd2wgs(Double.parseDouble(longitude), Double.parseDouble(latitude));
                mCurLng = latLng[0];
                mCurLat = latLng[1];
                XLog.d("IN CHN, NEED TO TRANSFORM COORDINATE");
            }
        }, error1 -> {
            XLog.e("HTTP: HTTP GET FAILED");
            //http 请求失败 离线转换坐标系
            double[] latLng = MapUtils.bd2wgs(Double.parseDouble(longitude), Double.parseDouble(latitude));
            mCurLng = latLng[0];
            mCurLat = latLng[1];
            XLog.d("IN CHN, NEED TO TRANSFORM COORDINATE");
        });
        // 给请求设置tag
        stringRequest.setTag("MapAPI");
        // 添加tag到请求队列
        mRequestQueue.add(stringRequest);
    }



    private void initNavigationView() {
        mNavigationView = findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_history) {
                Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
                startActivity(intent);
            } else if (id == R.id.nav_settings) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            } else if (id == R.id.nav_contact) {

                Intent intent = new Intent();
                intent.setAction("android.intent.action.VIEW");
                Uri content_url = Uri.parse("https://gitee.com/zcshou/gogogo/issues");
                intent.setData(content_url);
                startActivity(intent);
            } else if (id == R.id.nav_dev) {
                try {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
                    startActivity(intent);
                } catch (Exception e) {
                    DisplayToast("无法跳转到开发者选项,请先确保您的设备已处于开发者模式");
                    e.printStackTrace();
                }
            } else if (id == R.id.nav_feedback) {
                File file = new File(getExternalFilesDir(null).toPath() + "/" + GoApplication.APP_NAME  + "/" + GoApplication.LOG_FILE_NAME);
                ShareUtils.shareFile(this, file, item.getTitle().toString());
            }

            DrawerLayout drawer = findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);

            return true;
        });
        initUserInfo();
    }

    private void initUserInfo() {
        View navHeaderView = mNavigationView.getHeaderView(0);

        TextView mUserName = navHeaderView.findViewById(R.id.user_name);
        TextView mUserLimitInfo = navHeaderView.findViewById(R.id.user_limit);
        ImageView mUserIcon = navHeaderView.findViewById(R.id.user_icon);

        if (sharedPreferences.getString("setting_reg_code", null) != null) {

            // mUserName.setText("ZCShou");

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            mUserLimitInfo.setText(String.format(Locale.getDefault(), "有效期: %s", simpleDateFormat.format(new Date(mTS*1000))));
        } else {
            mUserIcon.setOnClickListener(v -> {
                DrawerLayout drawer = findViewById(R.id.drawer_layout);

                if (drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.closeDrawer(GravityCompat.START);
                }
                showRegisterDialog();
            });

            mUserName.setOnClickListener(v -> {
                DrawerLayout drawer = findViewById(R.id.drawer_layout);

                if (drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.closeDrawer(GravityCompat.START);
                }
                showRegisterDialog();
            });
        }
    }

    public void showRegisterDialog() {
        final android.app.AlertDialog alertDialog = new android.app.AlertDialog.Builder(this).create();
        alertDialog.show();
        alertDialog.setCancelable(false);
        Window window = alertDialog.getWindow();
        if (window != null) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            window.setContentView(R.layout.register_dialog);
            window.setGravity(Gravity.CENTER);
            window.setWindowAnimations(R.style.DialogAnimFadeInFadeOut);

            final TextView mRegReq = window.findViewById(R.id.reg_request);
            final TextView regResp = window.findViewById(R.id.reg_response);

            final TextView regUserName = window.findViewById(R.id.reg_user_name);
            regUserName.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() >= 3) {
                        try {
                            mReg.put("UserName", s.toString());
                            mRegReq.setText(mReg.toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });

            DatePicker mDatePicker = window.findViewById(R.id.date_picker);
            mDatePicker.setOnDateChangedListener((view, year, monthOfYear, dayOfMonth) -> {
                try {
                    mReg.put("DateTime", 1111);
                    mRegReq.setText(mReg.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            });

            mPtlCheckBox = window.findViewById(R.id.reg_check);
            mPtlCheckBox.setOnClickListener(v -> {
                if (mPtlCheckBox.isChecked()) {
                    showProtocolDialog();
                }
            });

            TextView regCancel = window.findViewById(R.id.reg_cancel);
            regCancel.setOnClickListener(v -> alertDialog.cancel());

            TextView regAgree = window.findViewById(R.id.reg_agree);
            regAgree.setOnClickListener(v -> {
                if (!mPtlCheckBox.isChecked()) {
                    DisplayToast("您必须先阅读并同意免责声明");
                    return;
                }
                if (TextUtils.isEmpty(regUserName.getText())) {
                    DisplayToast("用户名不能为空");
                    return;
                }
                if (TextUtils.isEmpty(regResp.getText())) {
                    DisplayToast("注册码不能为空");
                    return;
                }
                try {
                    mReg.put("RegReq", mReg.toString());
                    mReg.put("ReqResp", regResp.toString());

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                alertDialog.cancel();
            });
        }
    }

    private void showProtocolDialog() {
        final android.app.AlertDialog alertDialog = new android.app.AlertDialog.Builder(this).create();
        alertDialog.show();
        alertDialog.setCancelable(false);
        Window window = alertDialog.getWindow();
        if (window != null) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);      // 防止出现闪屏
            window.setContentView(R.layout.user_protocol);
            window.setGravity(Gravity.CENTER);
            window.setWindowAnimations(R.style.DialogAnimFadeInFadeOut);

            TextView tvContent = window.findViewById(R.id.tv_content);
            Button tvCancel = window.findViewById(R.id.tv_cancel);
            Button tvAgree = window.findViewById(R.id.tv_agree);
            CheckBox tvCheck = window.findViewById(R.id.tv_check);
            tvCheck.setVisibility(GONE);
            SpannableStringBuilder ssb = new SpannableStringBuilder();
            ssb.append(getResources().getString(R.string.protocol));

            tvContent.setMovementMethod(LinkMovementMethod.getInstance());
            tvContent.setText(ssb, TextView.BufferType.SPANNABLE);

            tvCancel.setOnClickListener(v -> {
                mPtlCheckBox.setChecked(false);
                alertDialog.cancel();
            });

            tvAgree.setOnClickListener(v -> {
                mPtlCheckBox.setChecked(true);
                alertDialog.cancel();
            });
        }
    }



    private void initSearchView() {
        searchView = findViewById(R.id.action_search);
        mSearchList = findViewById(R.id.search_list_view);
        mSearchLayout = findViewById(R.id.search_linear);
        mSearchHistoryList = findViewById(R.id.search_history_list_view);
        mHistoryLayout = findViewById(R.id.search_history_linear);

        //搜索结果列表的点击监听
        setSearchResultClickListener();

        //搜索历史列表的点击监听
        setSearchHistoryClickListener();

        //设置搜索建议返回值监听
        setSearchSuggestListener();
    }

    //设置 search list 点击监听
    private void setSearchResultClickListener() {
        mSearchList.setOnItemClickListener((parent, view, position, id) -> {
            String lng = ((TextView) view.findViewById(R.id.poi_longitude)).getText().toString();
            String lat = ((TextView) view.findViewById(R.id.poi_latitude)).getText().toString();
            // DisplayToast("lng is "+lng+"lat is "+lat);
            mCurLatLngMap = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));
            MapStatusUpdate mapstatusupdate = MapStatusUpdateFactory.newLatLng(mCurLatLngMap);
            //对地图的中心点进行更新，
            mBaiduMap.setMapStatus(mapstatusupdate);

            markSelectedPosition();

            transformCoordinate(lng, lat);

            // mSearchList.setVisibility(View.GONE);
            //搜索历史 插表参数
            ContentValues contentValues = new ContentValues();
            contentValues.put(DataBaseHistorySearch.DB_COLUMN_KEY, ((TextView) view.findViewById(R.id.poi_name)).getText().toString());
            contentValues.put(DataBaseHistorySearch.DB_COLUMN_DESCRIPTION, ((TextView) view.findViewById(R.id.poi_address)).getText().toString());
            contentValues.put(DataBaseHistorySearch.DB_COLUMN_IS_LOCATION, DataBaseHistorySearch.DB_SEARCH_TYPE_RESULT);
            contentValues.put(DataBaseHistorySearch.DB_COLUMN_LONGITUDE_CUSTOM, lng);
            contentValues.put(DataBaseHistorySearch.DB_COLUMN_LATITUDE_CUSTOM, lat);
            contentValues.put(DataBaseHistorySearch.DB_COLUMN_LONGITUDE_WGS84, String.valueOf(mCurLng));
            contentValues.put(DataBaseHistorySearch.DB_COLUMN_LATITUDE_WGS84, String.valueOf(mCurLat));
            contentValues.put(DataBaseHistorySearch.DB_COLUMN_TIMESTAMP, System.currentTimeMillis() / 1000);

            DataBaseHistorySearch.saveHistorySearch(mSearchHistoryDB, contentValues);
            mSearchLayout.setVisibility(View.INVISIBLE);
            searchItem.collapseActionView();
        });
    }

    //设置 search history list 点击监听
    private void setSearchHistoryClickListener() {
        mSearchHistoryList.setOnItemClickListener((parent, view, position, id) -> {
            String searchDescription = ((TextView) view.findViewById(R.id.search_description)).getText().toString();
            String searchKey = ((TextView) view.findViewById(R.id.search_key)).getText().toString();
            String searchIsLoc = ((TextView) view.findViewById(R.id.search_isLoc)).getText().toString();

            //如果是定位搜索
            if (searchIsLoc.equals("1")) {
                String lng = ((TextView) view.findViewById(R.id.search_longitude)).getText().toString();
                String lat = ((TextView) view.findViewById(R.id.search_latitude)).getText().toString();
                //对地图的中心点进行更新
                mCurLatLngMap = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));
                MapStatusUpdate mapstatusupdate = MapStatusUpdateFactory.newLatLng(mCurLatLngMap);
                mBaiduMap.setMapStatus(mapstatusupdate);

                markSelectedPosition();

                transformCoordinate(lng, lat);

                //设置列表不可见
                mHistoryLayout.setVisibility(View.INVISIBLE);
                searchItem.collapseActionView();
                //更新表
                ContentValues contentValues = new ContentValues();
                contentValues.put(DataBaseHistorySearch.DB_COLUMN_KEY, searchKey);
                contentValues.put(DataBaseHistorySearch.DB_COLUMN_DESCRIPTION, searchDescription);
                contentValues.put(DataBaseHistorySearch.DB_COLUMN_IS_LOCATION, DataBaseHistorySearch.DB_SEARCH_TYPE_RESULT);
                contentValues.put(DataBaseHistorySearch.DB_COLUMN_LONGITUDE_CUSTOM, lng);
                contentValues.put(DataBaseHistorySearch.DB_COLUMN_LATITUDE_CUSTOM, lat);
                contentValues.put(DataBaseHistorySearch.DB_COLUMN_LONGITUDE_WGS84, String.valueOf(mCurLng));
                contentValues.put(DataBaseHistorySearch.DB_COLUMN_LATITUDE_WGS84, String.valueOf(mCurLat));
                contentValues.put(DataBaseHistorySearch.DB_COLUMN_TIMESTAMP, System.currentTimeMillis() / 1000);

                DataBaseHistorySearch.saveHistorySearch(mSearchHistoryDB, contentValues);
            } else if (searchIsLoc.equals("0")) { //如果仅仅是搜索
                try {
                    // 重新搜索之前的关键字
                    searchView.setQuery(searchKey, true);
                } catch (Exception e) {
                    DisplayToast("搜索失败，请检查网络连接");
                    XLog.d("搜索失败，请检查网络连接");
                    e.printStackTrace();
                }
            } else {    //其他情况
                XLog.d("搜索失败，参数非法");
            }
        });
        mSearchHistoryList.setOnItemLongClickListener((parent, view, position, id) -> {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("警告")//这里是表头的内容
                    .setMessage("确定要删除该项搜索记录吗?")//这里是中间显示的具体信息
                    .setPositiveButton("确定",(dialog, which) -> {
                        String searchKey = ((TextView) view.findViewById(R.id.search_key)).getText().toString();

                        try {
                            mSearchHistoryDB.delete(DataBaseHistorySearch.TABLE_NAME, DataBaseHistorySearch.DB_COLUMN_KEY + " = ?", new String[] {searchKey});
                            //删除成功
                            //展示搜索历史
                            List<Map<String, Object>> data = getSearchHistory();

                            if (data.size() > 0) {
                                SimpleAdapter simAdapt = new SimpleAdapter(
                                        MainActivity.this,
                                        data,
                                        R.layout.search_record_item,
                                        new String[] {DataBaseHistorySearch.DB_COLUMN_KEY,
                                                DataBaseHistorySearch.DB_COLUMN_DESCRIPTION,
                                                DataBaseHistorySearch.DB_COLUMN_TIMESTAMP,
                                                DataBaseHistorySearch.DB_COLUMN_IS_LOCATION,
                                                DataBaseHistorySearch.DB_COLUMN_LONGITUDE_CUSTOM,
                                                DataBaseHistorySearch.DB_COLUMN_LATITUDE_CUSTOM}, // 与下面数组元素要一一对应
                                        new int[] {R.id.search_key, R.id.search_description, R.id.search_timestamp, R.id.search_isLoc, R.id.search_longitude, R.id.search_latitude});
                                mSearchHistoryList.setAdapter(simAdapt);
                                mHistoryLayout.setVisibility(View.VISIBLE);
                            }
                        } catch (Exception e) {
                            XLog.e("DATABASE: delete error");
                            DisplayToast("DELETE ERROR[UNKNOWN]");
                            e.printStackTrace();
                        }
                    })
                    .setNegativeButton("取消",
                            (dialog, which) -> {
                            })
                    .show();
            return true;
        });
    }

    //检索建议
    private void setSearchSuggestListener() {
        mSuggestionSearch = SuggestionSearch.newInstance();
        mSuggestionSearch.setOnGetSuggestionResultListener(suggestionResult -> {
            if (suggestionResult == null || suggestionResult.getAllSuggestions() == null) {
                DisplayToast("没有找到检索结果");
            } else { //获取在线建议检索结果
                    List<Map<String, Object>> data = new ArrayList<>();
                    int retCnt = suggestionResult.getAllSuggestions().size();

                    for (int i = 0; i < retCnt; i++) {
                        if (suggestionResult.getAllSuggestions().get(i).pt == null) {
                            continue;
                        }

                        Map<String, Object> poiItem = new HashMap<>();
                        poiItem.put(POI_NAME, suggestionResult.getAllSuggestions().get(i).key);
                        poiItem.put(POI_ADDRESS, suggestionResult.getAllSuggestions().get(i).city + " " + suggestionResult.getAllSuggestions().get(i).district);
                        poiItem.put(POI_LONGITUDE, "" + suggestionResult.getAllSuggestions().get(i).pt.longitude);
                        poiItem.put(POI_LATITUDE, "" + suggestionResult.getAllSuggestions().get(i).pt.latitude);
                        data.add(poiItem);
                    }

                    SimpleAdapter simAdapt = new SimpleAdapter(
                            MainActivity.this,
                            data,
                            R.layout.poi_search_item,
                            new String[] {POI_NAME, POI_ADDRESS, POI_LONGITUDE, POI_LATITUDE}, // 与下面数组元素要一一对应
                            new int[] {R.id.poi_name, R.id.poi_address, R.id.poi_longitude, R.id.poi_latitude});
                    mSearchList.setAdapter(simAdapt);
                    // mSearchList.setVisibility(View.VISIBLE);
                    mSearchLayout.setVisibility(View.VISIBLE);
                }
        });
    }



    //提醒开启位置模拟的弹框
    private void showEnableMockLocationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("启用位置模拟")//这里是表头的内容
                .setMessage("请在\"开发者选项→选择模拟位置信息应用\"中进行设置")//这里是中间显示的具体信息
                .setPositiveButton("设置",(dialog, which) -> {
                    try {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
                        startActivity(intent);
                    } catch (Exception e) {
                        DisplayToast("无法跳转到开发者选项,请先确保您的设备已处于开发者模式");
                        e.printStackTrace();
                    }
                })
                .setNegativeButton("取消",(dialog, which) -> {
                })
                .show();
    }

    //提醒开启悬浮窗的弹框
    private void showEnableFloatWindowDialog() {
        new AlertDialog.Builder(this)
                .setTitle("启用悬浮窗")//这里是表头的内容
                .setMessage("为了模拟定位的稳定性，建议开启\"显示悬浮窗\"选项")//这里是中间显示的具体信息
                .setPositiveButton("设置",(dialog, which) -> {
                    try {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    } catch (Exception e) {
                        DisplayToast("无法跳转到设置界面，请在权限管理中开启该应用的悬浮窗");
                        e.printStackTrace();
                    }
                })
                .setNegativeButton("取消", (dialog, which) -> DisplayToast("悬浮窗权限未开启，无法启动模拟位置"))
                .show();
    }

    //显示开启GPS的提示
    private void showEnableGpsDialog() {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("启用定位服务")//这里是表头的内容
                .setMessage("是否开启 GPS 定位服务?")//这里是中间显示的具体信息
                .setPositiveButton("确定",(dialog, which) -> {
                    try {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);
                    } catch (Exception e) {
                        DisplayToast("无法跳转到设置界面，请在手动前往设置界面开启定位服务");
                        e.printStackTrace();
                    }
                })
                .setNegativeButton("取消",(dialog, which) -> DisplayToast("定位服务未开启，无法启动模拟位置"))
                .show();
    }

    // 提醒开启位置模拟的弹框
    private void showDisableWifiDialog() {
        new AlertDialog.Builder(this)
                .setTitle("警告")
                .setMessage("开启 WIFI 后（即使没有连接热点）将导致定位闪回真实位置。建议关闭 WIFI，使用移动流量进行游戏！")
                .setPositiveButton("去关闭",(dialog, which) -> {
                    try {
                        Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                        startActivity(intent);
                    } catch (Exception e) {
                        DisplayToast("无法跳转到 WIFI 设置界面，请手动关闭 WIFI");
                        e.printStackTrace();
                    }
                })
                .setNegativeButton("忽略",(dialog, which) -> {
                })
                .show();
    }

    private void initStoreHistory() {
        try {
            //定位历史
            DataBaseHistoryLocation dbLocation = new DataBaseHistoryLocation(getApplicationContext());
            mLocationHistoryDB = dbLocation.getWritableDatabase();
            // 搜索历史
            DataBaseHistorySearch dbHistory = new DataBaseHistorySearch(getApplicationContext());
            mSearchHistoryDB = dbHistory.getWritableDatabase();
        } catch (Exception e) {
            XLog.e("DATABASE: sqlite init error");
            e.printStackTrace();
        }
    }

    //获取查询历史
    private List<Map<String, Object>> getSearchHistory() {
        List<Map<String, Object>> data = new ArrayList<>();

        try {
            Cursor cursor = mSearchHistoryDB.query(DataBaseHistorySearch.TABLE_NAME, null,
                    DataBaseHistorySearch.DB_COLUMN_ID + " > ?", new String[] {"0"},
                    null, null, DataBaseHistorySearch.DB_COLUMN_TIMESTAMP + " DESC", null);

            while (cursor.moveToNext()) {
                Map<String, Object> searchHistoryItem = new HashMap<>();
                searchHistoryItem.put(DataBaseHistorySearch.DB_COLUMN_KEY, cursor.getString(1));
                searchHistoryItem.put(DataBaseHistorySearch.DB_COLUMN_DESCRIPTION, cursor.getString(2));
                searchHistoryItem.put(DataBaseHistorySearch.DB_COLUMN_TIMESTAMP, "" + cursor.getInt(3));
                searchHistoryItem.put(DataBaseHistorySearch.DB_COLUMN_IS_LOCATION, "" + cursor.getInt(4));
                searchHistoryItem.put(DataBaseHistorySearch.DB_COLUMN_LONGITUDE_CUSTOM, "" + cursor.getString(7));
                searchHistoryItem.put(DataBaseHistorySearch.DB_COLUMN_LATITUDE_CUSTOM, "" + cursor.getString(8));
                data.add(searchHistoryItem);
            }
            cursor.close();
        } catch (Exception e) {
            XLog.e("DATABASE: query error");
            e.printStackTrace();
        }

        return data;
    }

    // 记录请求的位置信息
    private void recordGetPositionInfo() {
        //参数坐标系：bd09
        final String safeCode = getResources().getString(R.string.safecode);
        final String ak = getResources().getString(R.string.ak);
        final String mapType = "bd09ll";
        final double latitude = mCurLatLngMap.latitude;
        final double longitude = mCurLatLngMap.longitude;
        //bd09坐标的位置信息
        String mapApiUrl = "https://api.map.baidu.com/reverse_geocoding/v3/?ak=" + ak + "&output=json&coordtype=" + mapType + "&location=" + latitude + "," + longitude + "&mcode=" + safeCode;
        XLog.d("recordGetPositionInfo:" + mapApiUrl);
        StringRequest stringRequest = new StringRequest(mapApiUrl, response -> {
            try {
                JSONObject getRetJson = new JSONObject(response);
                XLog.d("recordGetPositionInfo:" + getRetJson.toString());

                //位置获取成功
                if (Integer.parseInt(getRetJson.getString("status")) == 0) {
                    JSONObject posInfoJson = getRetJson.getJSONObject("result");
                    String formatted_address = posInfoJson.getString("formatted_address");
                    XLog.d(formatted_address);
                    //插表参数
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(DataBaseHistoryLocation.DB_COLUMN_LOCATION, formatted_address);
                    contentValues.put(DataBaseHistoryLocation.DB_COLUMN_LONGITUDE_WGS84, String.valueOf(mCurLng));
                    contentValues.put(DataBaseHistoryLocation.DB_COLUMN_LATITUDE_WGS84, String.valueOf(mCurLat));
                    contentValues.put(DataBaseHistoryLocation.DB_COLUMN_TIMESTAMP, System.currentTimeMillis() / 1000);
                    contentValues.put(DataBaseHistoryLocation.DB_COLUMN_LONGITUDE_CUSTOM, Double.toString(longitude));
                    contentValues.put(DataBaseHistoryLocation.DB_COLUMN_LATITUDE_CUSTOM, Double.toString(latitude));

                    DataBaseHistoryLocation.saveHistoryLocation(mLocationHistoryDB, contentValues);
                } else { //位置获取失败
                    //插表参数
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(DataBaseHistoryLocation.DB_COLUMN_LOCATION, "NULL");
                    contentValues.put(DataBaseHistoryLocation.DB_COLUMN_LONGITUDE_WGS84, String.valueOf(mCurLng));
                    contentValues.put(DataBaseHistoryLocation.DB_COLUMN_LATITUDE_WGS84, String.valueOf(mCurLat));
                    contentValues.put(DataBaseHistoryLocation.DB_COLUMN_TIMESTAMP, System.currentTimeMillis() / 1000);
                    contentValues.put(DataBaseHistoryLocation.DB_COLUMN_LONGITUDE_CUSTOM, Double.toString(longitude));
                    contentValues.put(DataBaseHistoryLocation.DB_COLUMN_LATITUDE_CUSTOM, Double.toString(latitude));

                    DataBaseHistoryLocation.saveHistoryLocation(mLocationHistoryDB, contentValues);
                }
            } catch (JSONException e) {
                XLog.e("JSON: resolve json error");
                //插表参数
                ContentValues contentValues = new ContentValues();
                contentValues.put(DataBaseHistoryLocation.DB_COLUMN_LOCATION, "NULL");
                contentValues.put(DataBaseHistoryLocation.DB_COLUMN_LONGITUDE_WGS84, String.valueOf(mCurLng));
                contentValues.put(DataBaseHistoryLocation.DB_COLUMN_LATITUDE_WGS84, String.valueOf(mCurLat));
                contentValues.put(DataBaseHistoryLocation.DB_COLUMN_TIMESTAMP, System.currentTimeMillis() / 1000);
                contentValues.put(DataBaseHistoryLocation.DB_COLUMN_LONGITUDE_CUSTOM, Double.toString(longitude));
                contentValues.put(DataBaseHistoryLocation.DB_COLUMN_LATITUDE_CUSTOM, Double.toString(latitude));

                DataBaseHistoryLocation.saveHistoryLocation(mLocationHistoryDB, contentValues);
                e.printStackTrace();
            }
        }, error -> {
            //http 请求失败
            XLog.e("HTTP: HTTP GET FAILED");
            //插表参数
            ContentValues contentValues = new ContentValues();
            contentValues.put(DataBaseHistoryLocation.DB_COLUMN_LOCATION, "NULL");
            contentValues.put(DataBaseHistoryLocation.DB_COLUMN_LONGITUDE_WGS84, String.valueOf(mCurLng));
            contentValues.put(DataBaseHistoryLocation.DB_COLUMN_LATITUDE_WGS84, String.valueOf(mCurLat));
            contentValues.put(DataBaseHistoryLocation.DB_COLUMN_TIMESTAMP, System.currentTimeMillis() / 1000);
            contentValues.put(DataBaseHistoryLocation.DB_COLUMN_LONGITUDE_CUSTOM, Double.toString(longitude));
            contentValues.put(DataBaseHistoryLocation.DB_COLUMN_LATITUDE_CUSTOM, Double.toString(latitude));

            DataBaseHistoryLocation.saveHistoryLocation(mLocationHistoryDB, contentValues);
        });
        // 给请求设置tag
        stringRequest.setTag("MapAPI");
        // 添加tag到请求队列
        mRequestQueue.add(stringRequest);
    }

    private void doGoLocation() {
        if (!isMockServStart) {
            XLog.d("Current Baidu LatLng: " + mCurLatLngMap.longitude + "  " + mCurLatLngMap.latitude);

            //start mock location service
            Intent serviceGoIntent = new Intent(MainActivity.this, ServiceGo.class);
            bindService(serviceGoIntent, mConnection, BIND_AUTO_CREATE);    // 绑定服务和活动，之后活动就可以去调服务的方法了
            serviceGoIntent.putExtra(LNG_MSG_ID, mCurLng);
            serviceGoIntent.putExtra(LAT_MSG_ID, mCurLat);

            isMove = true;

            //save record
            recordGetPositionInfo();

            startForegroundService(serviceGoIntent);
            XLog.d("startForegroundService: ServiceGo");

            isMockServStart = true;
        }
    }

    private void startGoLocation(View v) {
        if (!isLimit && GoUtils.isNetworkAvailable(this)) {    // 时间限制
            //悬浮窗权限判断
            if (!Settings.canDrawOverlays(getApplicationContext())) {
                showEnableFloatWindowDialog();
                XLog.e("无悬浮窗权限!");
            } else {
                if (!GoUtils.isGpsOpened(this)) {
                    showEnableGpsDialog();
                } else {
                    if (isMockServStart) {
                        if (mCurLatLngMap == null) {
                            stopGoLocation();
                            Snackbar.make(v, "模拟位置已终止", Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();
                            mButtonStart.setImageResource(R.drawable.ic_position);
                        } else {
                            mServiceBinder.setPosition(mCurLng, mCurLat);
                            isMove = true;
                            Snackbar.make(v, "已传送到新位置", Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();
                        }
                    } else {
                        if (!GoUtils.isAllowMockLocation(this)) {
                            showEnableMockLocationDialog();
                            XLog.e("无模拟位置权限!");
                        } else {
                            if (mCurLatLngMap == null) {
                                Snackbar.make(v, "请先点击地图位置或者搜索位置", Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();
                            } else {
                                doGoLocation();
                                mButtonStart.setImageResource(R.drawable.ic_fly);
                                Snackbar.make(v, "模拟位置已启动", Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();
                            }
                        }
                    }
                }
            }
        } else {
            XLog.e("内部出现错误，无法继续!");
        }
    }

    private void stopGoLocation() {
        if (isMockServStart) {
            unbindService(mConnection); // 解绑服务，服务要记得解绑，不要造成内存泄漏
            Intent serviceGoIntent = new Intent(MainActivity.this, ServiceGo.class);
            stopService(serviceGoIntent);
            isMockServStart = false;
        }
    }

    private void setGoBtnListener() {
        mButtonStart = findViewById(R.id.faBtnStart);
        mButtonStart.setOnClickListener(this::startGoLocation);
    }

    private class TimeTask implements Runnable {
        private final String[] ntpServerPool = {"ntp1.aliyun.com", "ntp2.aliyun.com", "ntp3.aliyun.com", "ntp4.aliyun.com", "ntp5.aliyun.com", "ntp6.aliyun.com", "ntp7.aliyun.com",
                "cn.pool.ntp.org", "cn.ntp.org.cn", "sg.pool.ntp.org", "tw.pool.ntp.org", "jp.pool.ntp.org", "hk.pool.ntp.org", "th.pool.ntp.org",
                "time.windows.com", "time.nist.gov", "time.apple.com", "time.asia.apple.com",
                "dns1.synet.edu.cn", "news.neu.edu.cn", "dns.sjtu.edu.cn", "dns2.synet.edu.cn", "ntp.glnet.edu.cn", "s2g.time.edu.cn",
                "ntp-sz.chl.la", "ntp.gwadar.cn", "3.asia.pool.ntp.org"};

        @Override
        public void run() {
            GoSntpClient GoSntpClient = new GoSntpClient();
            int i;
            for (i = 0; i < ntpServerPool.length; i++) {
                if (GoSntpClient.requestTime(ntpServerPool[i], 30000)) {
                    long now = GoSntpClient.getNtpTime() + SystemClock.elapsedRealtime() - GoSntpClient.getNtpTimeReference();
                    if (now / 1000 < mTS) {
                        isLimit = false;
                    }
                    break;
                }
            }
            if (i >= ntpServerPool.length) {
                isLimit = true;
            }
        }
    }

    public void DisplayToast(String str) {
        Toast toast = Toast.makeText(MainActivity.this, str, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP, 0, 220);
        toast.show();
    }
}
