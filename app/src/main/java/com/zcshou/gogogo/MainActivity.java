package com.zcshou.gogogo;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
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
import com.baidu.mapapi.search.sug.OnGetSuggestionResultListener;
import com.baidu.mapapi.search.sug.SuggestionSearch;
import com.baidu.mapapi.search.sug.SuggestionSearchOption;
import com.elvishew.xlog.XLog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
// import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
import com.zcshou.utils.GoUtils;
import com.zcshou.utils.MapUtils;

import static android.view.View.GONE;
import static com.zcshou.gogogo.R.drawable;
import static com.zcshou.gogogo.R.id;
import static com.zcshou.gogogo.R.layout;
import static com.zcshou.gogogo.R.string;

public class MainActivity extends BaseActivity
        implements SensorEventListener {
    /* 对外 */
    public static final String LAT_MSG_ID = "LAT_VALUE";
    public static final String LNG_MSG_ID = "LNG_VALUE";

    // 百度地图相关
    private MapView mMapView;
    private static BaiduMap mBaiduMap = null;
    private LocationClient mLocClient = null;
    private String mCurrentCity = null;
    private double mCurrentLat = 0.0;
    private double mCurrentLon = 0.0;
    private float mCurrentDirection = 0.0f;
    private float mCurrentAccuracy;
    public static LatLng mCurLatLngMap = new LatLng(36.547743718042415, 117.07018449827267);
    public static BitmapDescriptor mMapIndicator = BitmapDescriptorFactory.fromResource(drawable.icon_gcoding);
    private boolean isFirstLoc = true; // 是否首次定位
    private static double mCurLat = ServiceGo.DEFAULT_LAT;  /* WGS84 坐标系的纬度 */
    private static double mCurLng = ServiceGo.DEFAULT_LNG;  /* WGS84 坐标系的经度 */
    private SensorManager mSensorManager;
    private float mLastDirection = 0.0f;
    // http
    private RequestQueue mRequestQueue;

    // 历史记录数据库
    private SQLiteDatabase mLocationHistoryDB;
    private SQLiteDatabase mSearchHistoryDB;

    // UI相关
    NavigationView mNavigationView;
    private FloatingActionButton mButtonStart;
    private FloatingActionButton mButtonStop;
    CheckBox mPtlCheckBox;
    //位置搜索相关
    private SearchView searchView;
    private ListView mSearchList;
    private ListView mSearchHistoryList;
    private LinearLayout mSearchLayout;
    private LinearLayout mHistoryLayout;
    private MenuItem searchItem;
    private SuggestionSearch mSuggestionSearch;
    private boolean isSubmit;

    private boolean isLimit = true;
    private static final long mTS = 1636588801;
    private boolean isMockServStart = false;

    SharedPreferences sharedPreferences;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(layout.activity_main);

        Toolbar toolbar = findViewById(id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, string.navigation_drawer_open, string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        //sqlite相关
        initStoreHistory();

        //http init
        mRequestQueue = Volley.newRequestQueue(this);

        initBaiduMap();

        initLocateBaiduMap();

        // 地图上按键的监听
        initListenerMapBtn();

        //网络是否可用
        if (!GoUtils.isNetworkAvailable(this)) {
            DisplayToast("网络连接不可用,请检查网络连接设置");
        }

        //gps是否开启
        if (!GoUtils.isGpsOpened(this)) {
            DisplayToast("GPS定位未开启，请先打开GPS定位服务");
        }

        initNavigationView();

        initSearchView();

        // set 开始定位 listener
        setGoBtnListener();

        TimeTask timeTask = new TimeTask();
        ExecutorService threadExecutor = Executors.newSingleThreadExecutor();
        threadExecutor.submit(timeTask);

        // 这里记录启动次数
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        long num = sharedPreferences.getLong("setting_startup_num", 0);
        sharedPreferences.edit()
                .putLong("setting_startup_num", ++num)
                .apply();
    }

    @Override
    protected void onPause() {
        XLog.d("MainActivity: onPause");
        mMapView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        XLog.d("MainActivity: onPause");
        mMapView.onResume();
        super.onResume();
    }

    @Override
    protected void onStop() {
        XLog.d("MainActivity: onStop");
        //取消注册传感器监听
        mSensorManager.unregisterListener(this);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (isMockServStart) {
            Intent serviceGoIntent = new Intent(MainActivity.this, ServiceGo.class);
            stopService(serviceGoIntent);
        }

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
//        DrawerLayout drawer = findViewById(id.drawer_layout);
//
//        if (drawer.isDrawerOpen(GravityCompat.START)) {
//            drawer.closeDrawer(GravityCompat.START);
//        } else {
//            super.onBackPressed();
//        }
        moveTaskToBack(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.main_menu_action_setting) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        } else if (id == R.id.main_menu_action_latlng) {
            showInputLatLngDialog();
        }  else if (id == R.id.action_faq) {
            showFaqDialog();
        } else if (id == R.id.main_menu_action_history) {
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        //找到searchView
        searchItem = menu.findItem(id.action_search);
        searchView = (SearchView) searchItem.getActionView();
        //searchView.setIconified(false);// 设置searchView处于展开状态
        searchView.onActionViewExpanded();// 当展开无输入内容的时候，没有关闭的图标
        // searchView.setIconifiedByDefault(true);//默认为true在框内，设置false则在框外
        //searchView.setSubmitButtonEnabled(false);//显示提交按钮
        searchItem.setOnActionExpandListener(new  MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                // Do something when collapsed
                menu.setGroupVisible(0, true);
                menu.setGroupVisible(1, true);
                // searchView.setIconified(false);// 设置searchView处于展开状态
                // mSearchList.setVisibility(View.GONE);
                mSearchLayout.setVisibility(View.INVISIBLE);
                mHistoryLayout.setVisibility(View.INVISIBLE);
                return true;  // Return true to collapse action view
            }
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                // Do something when expanded
                menu.setGroupVisible(0, false);
                menu.setGroupVisible(1, false);
                mSearchLayout.setVisibility(View.INVISIBLE);
                //展示搜索历史
                List<Map<String, Object>> data = getSearchHistory();

                if (data.size() > 0) {
                    SimpleAdapter simAdapt = new SimpleAdapter(
                            MainActivity.this,
                            data,
                            layout.search_record_item,
                            new String[] {"search_key", "search_description", "search_timestamp", "search_isLoc", "search_longitude", "search_latitude"}, // 与下面数组元素要一一对应
                            new int[] {id.search_key, id.search_description, id.search_timestamp, id.search_isLoc, id.search_longitude, id.search_latitude});
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
                    isSubmit = true;
                    mSuggestionSearch.requestSuggestion((new SuggestionSearchOption())
                            .keyword(query)
                            .city(mCurrentCity)
                    );
                    //搜索历史 插表参数
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(DataBaseHistorySearch.DB_COLUMN_KEY, query);
                    contentValues.put(DataBaseHistorySearch.DB_COLUMN_DESCRIPTION, "搜索...");
                    contentValues.put(DataBaseHistorySearch.DB_COLUMN_IS_LOCATION, 0);
                    contentValues.put(DataBaseHistorySearch.DB_COLUMN_TIMESTAMP, System.currentTimeMillis() / 1000);

                    if (!saveSelectedSearchItem(mSearchHistoryDB, contentValues)) {
                        XLog.e("DATABASE: saveSelectedSearchItem[SearchHistory] error");
                    } else {
                        XLog.d("DATABASE: saveSelectedSearchItem[SearchHistory] success");
                    }

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
                    //do search
                    //WATCH ME
                    try {
                        mSuggestionSearch.requestSuggestion((new SuggestionSearchOption())
                                .keyword(newText)
                                .city(mCurrentCity)
                        );
                        // poiSearch.searchInCity((new PoiCitySearchOption())
                        //         .city(mCurrentCity)
                        //         .keyword(newText)
                        //         .pageCapacity(30)
                        //         .pageNum(0));
                    } catch (Exception e) {
                        DisplayToast("搜索失败，请检查网络连接");
                        XLog.d("HTTP: 搜索失败，请检查网络连接");
                        e.printStackTrace();
                    }

                    //
                }

                return true;
            }
        });
        return true;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        float x = sensorEvent.values[0];

        if (Math.abs(x - mLastDirection) > 1.0) {
            mCurrentDirection = x;
            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(mCurrentAccuracy)
                    .direction(mCurrentDirection)   // 此处设置开发者获取到的方向信息，顺时针0-360
                    .latitude(mCurrentLat)
                    .longitude(mCurrentLon)
                    .build();
            mBaiduMap.setMyLocationData(locData);
        }

        mLastDirection = x;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void initBaiduMap() {
        // 地图初始化
        mMapView = findViewById(id.bmapView);
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
            Sensor mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (mSensor != null) {
                mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
    }

    //开启地图的定位图层
    private void initLocateBaiduMap() {
        // 定位初始化
        mLocClient = new LocationClient(this);
        mLocClient.registerLocationListener(new BDAbstractLocationListener() {
            @Override
            public void onReceiveLocation(BDLocation bdLocation) {
                // mapview 销毁后不在处理新接收的位置
                if (bdLocation == null || mMapView == null) {
                    return;
                }

                mCurrentCity = bdLocation.getCity();
                mCurrentLat = bdLocation.getLatitude();
                mCurrentLon = bdLocation.getLongitude();
                mCurrentAccuracy = bdLocation.getRadius();
                MyLocationData locData = new MyLocationData.Builder()
                        .accuracy(bdLocation.getRadius())
                        .direction(mCurrentDirection)// 此处设置开发者获取到的方向信息，顺时针0-360
                        .latitude(bdLocation.getLatitude())
                        .longitude(bdLocation.getLongitude()).build();
                mBaiduMap.setMyLocationData(locData);

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

                        // 这里将百度地图位置转换为 GPS 坐标。实际使用GPS 返回的坐标会更好点
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
        LocationClientOption option = new LocationClientOption();
        option.setIsNeedAddress(true);
        //可选，设置是否使用gps，默认false
        //使用高精度和仅用设备两种定位模式的，参数必须设置为true
        option.setOpenGps(true);
        //可选，设置返回经纬度坐标类型，默认GCJ02
        //GCJ02：国测局坐标；
        //BD09ll：百度经纬度坐标；
        //BD09：百度墨卡托坐标；
        //海外地区定位，无需设置坐标类型，统一返回WGS84类型坐标
        option.setCoorType("bd09ll");
        //可选，设置发起定位请求的间隔，int类型，单位ms
        //如果设置为0，则代表单次定位，即仅定位一次，默认为0
        //如果设置非0，需设置1000ms以上才有效
        option.setScanSpan(1200);
        //可选，设置是否收集Crash信息，默认收集，即参数为false
        option.SetIgnoreCacheException(true);
        //可选，设置定位模式，默认高精度
        //LocationMode.Hight_Accuracy：高精度；
        //LocationMode. Battery_Saving：低功耗；
        //LocationMode. Device_Sensors：仅使用设备；
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy); //高精度模式
        //可选，设置是否当GPS有效时按照1S/1次频率输出GPS结果，默认false
//    option.setLocationNotify(true);
        mLocClient.setLocOption(option);
        mLocClient.start();
    }

    //地图上各按键的监听
    private void initListenerMapBtn() {
        RadioGroup mGroupMapTrack = this.findViewById(id.RadioGroupMapTrack);
        mGroupMapTrack.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == id.normalloc) {
                mBaiduMap.setMyLocationConfiguration(new MyLocationConfiguration(
                        MyLocationConfiguration.LocationMode.NORMAL, true, null));
                MapStatus.Builder builder1 = new MapStatus.Builder();
                builder1.overlook(0);
                mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder1.build()));
            }

            if (checkedId == id.trackloc) {
                mBaiduMap.setMyLocationConfiguration(new MyLocationConfiguration(
                        MyLocationConfiguration.LocationMode.FOLLOWING, true, null));
                MapStatus.Builder builder = new MapStatus.Builder();
                builder.overlook(0);
                mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
            }

            if (checkedId == id.compassloc) {
                mBaiduMap.setMyLocationConfiguration(new MyLocationConfiguration(
                        MyLocationConfiguration.LocationMode.COMPASS, true, null));
            }
        });

        RadioGroup mGroupMapType = this.findViewById(id.RadioGroupMapType);
        mGroupMapType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == id.normal) {
                mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
            }

            if (checkedId == id.statellite) {
                mBaiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
            }
        });
    }

    //设置是否显示交通图
    public void setMapTraffic(View view) {
        mBaiduMap.setTrafficEnabled(((CheckBox) view).isChecked());
    }

    //设置是否显示百度热力图
    public void setBaiduHeatMap(View view) {
        mBaiduMap.setBaiduHeatMapEnabled(((CheckBox) view).isChecked());
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
        XLog.d("markSelectedPosition");

        if (mCurLatLngMap != null) {
            MarkerOptions ooA = new MarkerOptions().position(mCurLatLngMap).icon(mMapIndicator);
            mBaiduMap.clear();
            mBaiduMap.addOverlay(ooA);
        }
    }

    //重置地图
    private void resetMap() {
        mBaiduMap.clear();

        mLocClient.requestLocation();   /* 请求位置 */

        MapStatusUpdate mapstatusupdate = MapStatusUpdateFactory.newLatLng(new LatLng(mCurrentLat, mCurrentLon));
        //对地图的中心点进行更新
        mBaiduMap.setMapStatus(mapstatusupdate);
        //更新当前位置
        mCurLatLngMap = new LatLng(mCurrentLat, mCurrentLon);
        transformCoordinate(Double.toString(mCurLatLngMap.longitude), Double.toString(mCurLatLngMap.latitude));
    }

    //坐标转换
    private void transformCoordinate(final String longitude, final String latitude) {
        //参数坐标系：bd09
        final double error = 0.00000001;
        final String mcode = getResources().getString(string.safecode);
        final String ak = getResources().getString(string.ak);
        //判断bd09坐标是否在国内
        String mapApiUrl = "https://api.map.baidu.com/geoconv/v1/?coords=" + longitude + "," + latitude +
                "&from=5&to=3&ak=" + ak + "&mcode=" + mcode;
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
                        // DisplayToast("OUT OF CHN, NO NEED TO TRANSFORM COORDINATE");
                    } else {
                        //离线转换坐标系
                        // double latLng[] = MapUtils.bd2wgs(Double.parseDouble(longitude), Double.parseDouble(latitude));
                        double[] latLng = MapUtils.gcj02towgs84(Double.parseDouble(gcj02Longitude), Double.parseDouble(gcj02Latitude));
                        mCurLng = latLng[0];
                        mCurLat = latLng[1];
                        XLog.d("IN CHN, NEED TO TRANSFORM COORDINATE");
                        // DisplayToast("IN CHN, NEED TO TRANSFORM COORDINATE");
                    }
                }
                //api接口转换失败 认为在国内
                else {
                    //离线转换坐标系
                    double[] latLng = MapUtils.bd2wgs(Double.parseDouble(longitude), Double.parseDouble(latitude));
                    mCurLng = latLng[0];
                    mCurLat = latLng[1];
                    XLog.d("IN CHN, NEED TO TRANSFORM COORDINATE");
                    // DisplayToast("BD Map Api Return not Zero, ASSUME IN CHN, NEED TO TRANSFORM COORDINATE");
                }
            } catch (JSONException e) {
                XLog.e("JSON: resolve json error");
                e.printStackTrace();
                //离线转换坐标系
                double[] latLng = MapUtils.bd2wgs(Double.parseDouble(longitude), Double.parseDouble(latitude));
                mCurLng = latLng[0];
                mCurLat = latLng[1];
                XLog.d("IN CHN, NEED TO TRANSFORM COORDINATE");
                // DisplayToast("Resolve JSON Error, ASSUME IN CHN, NEED TO TRANSFORM COORDINATE");
            }
        }, error1 -> {
            //http 请求失败
            XLog.e("HTTP: HTTP GET FAILED");
            //离线转换坐标系
            double[] latLng = MapUtils.bd2wgs(Double.parseDouble(longitude), Double.parseDouble(latitude));
            mCurLng = latLng[0];
            mCurLat = latLng[1];
            XLog.d("IN CHN, NEED TO TRANSFORM COORDINATE");
            // DisplayToast("HTTP Get Failed, ASSUME IN CHN, NEED TO TRANSFORM COORDINATE");
        });
        // 给请求设置tag
        stringRequest.setTag("MapAPI");
        // 添加tag到请求队列
        mRequestQueue.add(stringRequest);
    }



    private void initNavigationView() {
        mNavigationView = findViewById(id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_history) {
                Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
                startActivity(intent);
            } else if (id == R.id.nav_settings) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            } else if (id == R.id.nav_contact) {
                Intent i = new Intent(Intent.ACTION_SEND);
                // i.setType("text/plain"); //模拟器请使用这行
                i.setType("message/rfc822"); // 真机上使用这行
                i.putExtra(Intent.EXTRA_EMAIL,
                        new String[] {"zcsexp@gmail.com"});
                i.putExtra(Intent.EXTRA_SUBJECT, "SUGGESTION");
                startActivity(Intent.createChooser(i,
                        "Select email application."));
            } else if (id == R.id.nav_dev) {
                try {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
                    startActivity(intent);
                } catch (Exception e) {
                    DisplayToast("无法跳转到开发者选项,请先确保您的设备已处于开发者模式");
                    e.printStackTrace();
                }
            }

            DrawerLayout drawer = findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);

            return true;
        });
        setUserLimitInfo();
    }

    private void setUserLimitInfo() {
        // 从上到下逐级获取
        View navHeaderView = mNavigationView.getHeaderView(0);
        TextView mUserLimitInfo = navHeaderView.findViewById(R.id.user_limit);
        TextView mUserName = navHeaderView.findViewById(R.id.user_name);

        mUserName.setOnClickListener(v -> {
            DrawerLayout drawer = findViewById(id.drawer_layout);

            if (drawer.isDrawerOpen(GravityCompat.START)) {
                drawer.closeDrawer(GravityCompat.START);
            }
            showRegisterDialog();
        });

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        mUserLimitInfo.setText(String.format(Locale.getDefault(), "有效期: %s", simpleDateFormat.format(new Date(mTS*1000))));
    }

    public void showRegisterDialog() {
        final android.app.AlertDialog alertDialog = new android.app.AlertDialog.Builder(this).create();
        alertDialog.show();
        alertDialog.setCancelable(false);
        Window window = alertDialog.getWindow();
        if (window != null) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            window.setContentView(layout.register_dialog);
            window.setGravity(Gravity.CENTER);
            window.setWindowAnimations(R.style.DialogAnimFadeInFadeOut);

            TextView regCancel = window.findViewById(R.id.reg_cancel);
            TextView regAgree = window.findViewById(R.id.reg_agree);
            mPtlCheckBox = window.findViewById(id.reg_check);
            final TextView regResp = window.findViewById(id.reg_response);
            final TextView regUserName = window.findViewById(id.reg_user_name);

            mPtlCheckBox.setOnClickListener(v -> {
                if (mPtlCheckBox.isChecked()) {
                    showProtocolDialog();
                }
            });

            regCancel.setOnClickListener(v -> alertDialog.cancel());

            regAgree.setOnClickListener(v -> {
                if (!mPtlCheckBox.isChecked()) {
                    DisplayToast("您必须先阅读并同意免责声明");
                    return;
                }
                if (TextUtils.isEmpty(regResp.getText())) {
                    DisplayToast("注册码不能为空");
                    return;
                }
                if (TextUtils.isEmpty(regUserName.getText())) {
                    DisplayToast("用户名不能为空");
                    return;
                }
                sharedPreferences.edit()
                        .putString("setting_reg_code", regResp.getText().toString())
                        .apply();

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
            window.setContentView(R.layout.welcom_protocol);
            window.setGravity(Gravity.CENTER);
            window.setWindowAnimations(R.style.DialogAnimFadeInFadeOut);

            TextView tvContent = window.findViewById(R.id.tv_content);
            TextView tvCancel = window.findViewById(R.id.tv_cancel);
            TextView tvAgree = window.findViewById(R.id.tv_agree);
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
        // 搜索相关
        searchView = findViewById(id.action_search);
        mSearchList = findViewById(id.search_list_view);
        mSearchLayout = findViewById(id.search_linear);
        mSearchHistoryList = findViewById(id.search_history_list_view);
        mHistoryLayout = findViewById(id.search_history_linear);

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
            contentValues.put(DataBaseHistorySearch.DB_COLUMN_DESCRIPTION, ((TextView) view.findViewById(R.id.poi_addr)).getText().toString());
            contentValues.put(DataBaseHistorySearch.DB_COLUMN_IS_LOCATION, 1);
            contentValues.put(DataBaseHistorySearch.DB_COLUMN_LONGITUDE_CUSTOM, lng);
            contentValues.put(DataBaseHistorySearch.DB_COLUMN_LATITUDE_CUSTOM, lat);
            contentValues.put(DataBaseHistorySearch.DB_COLUMN_LONGITUDE_WGS84, String.valueOf(mCurLng));
            contentValues.put(DataBaseHistorySearch.DB_COLUMN_LATITUDE_WGS84, String.valueOf(mCurLat));
            contentValues.put(DataBaseHistorySearch.DB_COLUMN_TIMESTAMP, System.currentTimeMillis() / 1000);

            if (saveSelectedSearchItem(mSearchHistoryDB, contentValues)) {
                XLog.d("DATABASE: saveSelectedSearchItem[SearchHistory] success");
            } else {
                XLog.e("DATABASE: saveSelectedSearchItem[SearchHistory] error");
            }

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
                // DisplayToast("lng is " + lng + "lat is " + lat);
                mCurLatLngMap = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));
                MapStatusUpdate mapstatusupdate = MapStatusUpdateFactory.newLatLng(mCurLatLngMap);
                //对地图的中心点进行更新
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
                contentValues.put(DataBaseHistorySearch.DB_COLUMN_IS_LOCATION, 1);
                contentValues.put(DataBaseHistorySearch.DB_COLUMN_LONGITUDE_CUSTOM, lng);
                contentValues.put(DataBaseHistorySearch.DB_COLUMN_LATITUDE_CUSTOM, lat);
                contentValues.put(DataBaseHistorySearch.DB_COLUMN_LONGITUDE_WGS84, String.valueOf(mCurLng));
                contentValues.put(DataBaseHistorySearch.DB_COLUMN_LATITUDE_WGS84, String.valueOf(mCurLat));
                contentValues.put(DataBaseHistorySearch.DB_COLUMN_TIMESTAMP, System.currentTimeMillis() / 1000);

                if (saveSelectedSearchItem(mSearchHistoryDB, contentValues)) {
                    XLog.d("DATABASE: saveSelectedSearchItem[SearchHistory] success");
                } else {
                    XLog.e("DATABASE: saveSelectedSearchItem[SearchHistory] error");
                }
            } else if (searchIsLoc.equals("0")) { //如果仅仅是搜索
                try {
                    // resetMap();
                    isSubmit = true;
                    mSuggestionSearch.requestSuggestion((new SuggestionSearchOption())
                            .keyword(searchKey)
                            .city(mCurrentCity)
                    );
                    mBaiduMap.clear();
                    mHistoryLayout.setVisibility(View.INVISIBLE);
                    searchItem.collapseActionView();
                    //更新表
                    //搜索历史 插表参数
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(DataBaseHistorySearch.DB_COLUMN_KEY, searchKey);
                    contentValues.put(DataBaseHistorySearch.DB_COLUMN_DESCRIPTION, "搜索...");
                    contentValues.put(DataBaseHistorySearch.DB_COLUMN_IS_LOCATION, 0);
                    contentValues.put(DataBaseHistorySearch.DB_COLUMN_TIMESTAMP, System.currentTimeMillis() / 1000);

                    if (saveSelectedSearchItem(mSearchHistoryDB, contentValues)) {
                        XLog.d("DATABASE: saveSelectedSearchItem[SearchHistory] success");
                    } else {
                        XLog.e("DATABASE: saveSelectedSearchItem[SearchHistory] error");
                    }
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
                                        layout.search_record_item,
                                        new String[] {"search_key", "search_description", "search_timestamp", "search_isLoc", "search_longitude", "search_latitude"}, // 与下面数组元素要一一对应
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
        OnGetSuggestionResultListener listener = res -> {
            if (res == null || res.getAllSuggestions() == null) {
                DisplayToast("没有找到检索结果");
            } else { //获取在线建议检索结果
                if (isSubmit) {
                    // mBaiduMap.clear();
                    //normal
                    //PoiGoOverlay poiOverlay = new PoiGoOverlay(mBaiduMap);
                    //poiOverlay.setSugData(res);// 设置POI数据
                    //mBaiduMap.setOnMarkerClickListener(poiOverlay);
                    //poiOverlay.addToMap();// 将所有的overlay添加到地图上
                    //poiOverlay.zoomToSpan();
                    mSearchLayout.setVisibility(View.INVISIBLE);
                    //标注搜索点 关闭搜索列表
                    // searchView.clearFocus();  //可以收起键盘
                    searchItem.collapseActionView(); //关闭搜索视图
                    isSubmit = false;
                } else {
                    List<Map<String, Object>> data = new ArrayList<>();
                    int retCnt = res.getAllSuggestions().size();

                    for (int i = 0; i < retCnt; i++) {
                        if (res.getAllSuggestions().get(i).pt == null) {
                            continue;
                        }

                        Map<String, Object> poiItem = new HashMap<>();
                        poiItem.put("key_name", res.getAllSuggestions().get(i).key);
                        poiItem.put("key_addr", res.getAllSuggestions().get(i).city + " " + res.getAllSuggestions().get(i).district);
                        poiItem.put("key_lng", "" + res.getAllSuggestions().get(i).pt.longitude);
                        poiItem.put("key_lat", "" + res.getAllSuggestions().get(i).pt.latitude);
                        data.add(poiItem);
                    }

                    SimpleAdapter simAdapt = new SimpleAdapter(
                            MainActivity.this,
                            data,
                            layout.poi_search_item,
                            new String[] {"key_name", "key_addr", "key_lng", "key_lat"}, // 与下面数组元素要一一对应
                            new int[] {id.poi_name, id.poi_addr, id.poi_longitude, id.poi_latitude});
                    mSearchList.setAdapter(simAdapt);
                    // mSearchList.setVisibility(View.VISIBLE);
                    mSearchLayout.setVisibility(View.VISIBLE);
                }
            }
        };
        mSuggestionSearch.setOnGetSuggestionResultListener(listener);
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
                })//setPositiveButton里面的onClick执行的是左边按钮
                .setNegativeButton("取消",(dialog, which) -> {
                })//setNegativeButton里面的onClick执行的是右边的按钮的操作
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
                })//setPositiveButton里面的onClick执行的是左边按钮
                .setNegativeButton("取消", (dialog, which) -> {
                })//setNegativeButton里面的onClick执行的是右边的按钮的操作
                .show();
    }

    //显示开启GPS的提示
    private void showEnableGpsDialog() {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Tips")//这里是表头的内容
                .setMessage("是否开启GPS定位服务?")//这里是中间显示的具体信息
                .setPositiveButton("确定",(dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivityForResult(intent, 0);
                })
                .setNegativeButton("取消",(dialog, which) -> {
                })
                .show();
    }

    //显示输入经纬度的对话框
    public void showInputLatLngDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("输入经度和纬度(BD09坐标系)");
        //    通过LayoutInflater来加载一个xml的布局文件作为一个View对象
        View view = LayoutInflater.from(MainActivity.this).inflate(layout.latlng_dialog, null);
        //    设置我们自己定义的布局文件作为弹出框的Content
        builder.setView(view);
        final EditText dialog_lng = view.findViewById(id.dialog_longitude);
        final EditText dialog_lat = view.findViewById(id.dialog_latitude);
        builder.setPositiveButton("确定", (dialog, which) -> {
            String dialog_lng_str, dialog_lat_str;

            try {
                dialog_lng_str = dialog_lng.getText().toString().trim();
                dialog_lat_str = dialog_lat.getText().toString().trim();
                double dialog_lng_double = Double.parseDouble(dialog_lng_str);
                double dialog_lat_double = Double.parseDouble(dialog_lat_str);

                // DisplayToast("经度: " + dialog_lng_str + ", 纬度: " + dialog_lat_str);
                if (dialog_lng_double > 180.0 || dialog_lng_double < -180.0 || dialog_lat_double > 90.0 || dialog_lat_double < -90.0) {
                    DisplayToast("经纬度超出限制!\n-180.0<经度<180.0\n-90.0<纬度<90.0");
                } else {
                    mCurLatLngMap = new LatLng(dialog_lat_double, dialog_lng_double);
                    MapStatusUpdate mapstatusupdate = MapStatusUpdateFactory.newLatLng(mCurLatLngMap);
                    //对地图的中心点进行更新
                    mBaiduMap.setMapStatus(mapstatusupdate);
                    markSelectedPosition();
                    transformCoordinate(dialog_lng_str, dialog_lat_str);
                }
            } catch (Exception e) {
                DisplayToast("获取经纬度出错,请检查输入是否正确");
                e.printStackTrace();
            }
        });
        builder.setNegativeButton("取消", (dialog, which) -> {
        });
        builder.show();
    }

    private void showFaqDialog() {
        final android.app.AlertDialog alertDialog = new android.app.AlertDialog.Builder(this).create();
        alertDialog.show();
        // alertDialog.setCancelable(false);
        Window window = alertDialog.getWindow();
        if (window != null) {
            window.setContentView(layout.faq);
            window.setGravity(Gravity.CENTER);
            window.setWindowAnimations(R.style.DialogAnimFadeInFadeOut);

            TextView tvContent = window.findViewById(R.id.faq_content);
            String str = "Q：Android 虚拟定位的实现原理是什么？\n"
                    + "A：具有 ROOT 权限的，一般直接拦截和位置相关的接口更改位置数据。没有 ROOT 权限的，一种是使用 Android 提供的模拟位置 API，一种基于 VirtualApp。\n"
                    + "\nQ：为何定位总是闪回真实位置？\n"
                    + "A：这和虚拟定位的实现方式有关系。Android 提供的模拟位置 API 只能模拟 GPS。而安卓的定位数据会同时使用 GPS、网络/WIFI等来实现更精确的定位\n"
                    + "\nQ：如何防止虚拟定位闪回真实位置？\n"
                    + "A：对于多数手机，是可以设置定位数据来源的。可以直接关闭从网络/WIFI定位，只允许 GPS 定位；同时关闭 WIFI，仅使用数据流量来上网可有效防止闪回\n"
                    + "\nQ：为啥在某些软件上没有效果？\n"
                    + "A：目前仅适用于百度地图和高德地图的SDK定位. 腾讯系列无法使用\n"
                    + "\nQ：使用位置的 APP 如何检测有没有虚拟定位？\n"
                    + "A：对于使用 ROOT 权限的虚拟定位，是无法被检测的（但是会检测到 ROOT 权限）；使用 Android 提供的模拟位置 API，在 Android 6.0 之后，系统也没有挺检测方式。基于 VirtualApp 的检测方式要多一些。但是通常，如果位置变化较大、较快，APP 会认为定位异常\n";

            SpannableStringBuilder ssb = new SpannableStringBuilder();
            ssb.append(str);

            tvContent.setMovementMethod(LinkMovementMethod.getInstance());
            tvContent.setText(ssb, TextView.BufferType.SPANNABLE);

        }
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
    // 保存选择的位置
    private boolean saveSelectedLocation(SQLiteDatabase sqLiteDatabase, ContentValues contentValues) {
        boolean insertRet = true;

        try {
            // 先删除原来的记录，再插入新记录
            String location = contentValues.get(DataBaseHistoryLocation.DB_COLUMN_LOCATION).toString();
            sqLiteDatabase.delete(DataBaseHistoryLocation.TABLE_NAME, DataBaseHistoryLocation.DB_COLUMN_LOCATION + " = ?", new String[] {location});
            sqLiteDatabase.insert(DataBaseHistoryLocation.TABLE_NAME, null, contentValues);
        } catch (Exception e) {
            XLog.e("DATABASE: insert error");
            insertRet = false;
            e.printStackTrace();
        }

        return insertRet;
    }

    //保存搜索选项
    private boolean saveSelectedSearchItem(SQLiteDatabase sqLiteDatabase, ContentValues contentValues) {
        boolean insertRet = true;

        try {
            // 先删除原来的记录，再插入新记录
            String searchKey = contentValues.get(DataBaseHistorySearch.DB_COLUMN_KEY).toString();
            sqLiteDatabase.delete(DataBaseHistorySearch.TABLE_NAME, DataBaseHistorySearch.DB_COLUMN_KEY + " = ?", new String[] {searchKey});
            sqLiteDatabase.insert(DataBaseHistorySearch.TABLE_NAME, null, contentValues);
        } catch (Exception e) {
            XLog.e("DATABASE: insert error");
            insertRet = false;
            e.printStackTrace();
        }

        return insertRet;
    }

    //获取查询历史
    private List<Map<String, Object>> getSearchHistory() {
        List<Map<String, Object>> data = new ArrayList<>();

        try {
            Cursor cursor = mSearchHistoryDB.query(DataBaseHistorySearch.TABLE_NAME, null,
                    DataBaseHistorySearch.DB_COLUMN_ID + " > ?", new String[] {"0"},
                    null, null, DataBaseHistorySearch.DB_COLUMN_TIMESTAMP + " DESC", null);

            while (cursor.moveToNext()) {
                // int ID = cursor.getInt(0);
                Map<String, Object> searchHistoryItem = new HashMap<>();
                searchHistoryItem.put("search_key", cursor.getString(1));
                searchHistoryItem.put("search_description", cursor.getString(2));
                searchHistoryItem.put("search_timestamp", "" + cursor.getInt(3));
                searchHistoryItem.put("search_isLoc", "" + cursor.getInt(4));
                searchHistoryItem.put("search_longitude", "" + cursor.getString(7));
                searchHistoryItem.put("search_latitude", "" + cursor.getString(8));
                data.add(searchHistoryItem);
            }
            // 关闭光标
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
        final String mcode = getResources().getString(string.safecode);
        final String ak = getResources().getString(string.ak);
        final String mapType = "bd09ll";
        //bd09坐标的位置信息
        String mapApiUrl = "https://api.map.baidu.com/reverse_geocoding/v3/?ak=" + ak + "&output=json&coordtype=" + mapType + "&location=" + mCurLatLngMap.latitude + "," + mCurLatLngMap.longitude + "&mcode=" + mcode;
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
                    contentValues.put(DataBaseHistoryLocation.DB_COLUMN_LONGITUDE_CUSTOM, Double.toString(mCurLatLngMap.longitude));
                    contentValues.put(DataBaseHistoryLocation.DB_COLUMN_LATITUDE_CUSTOM, Double.toString(mCurLatLngMap.latitude));

                    if (saveSelectedLocation(mLocationHistoryDB, contentValues)) {
                        XLog.d("DATABASE: saveSelectedLocation[HistoryLocation] success");
                    } else {
                        XLog.e("DATABASE: saveSelectedLocation[HistoryLocation] error");
                    }
                } else { //位置获取失败
                    //插表参数
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(DataBaseHistoryLocation.DB_COLUMN_LOCATION, "NULL");
                    contentValues.put(DataBaseHistoryLocation.DB_COLUMN_LONGITUDE_WGS84, String.valueOf(mCurLng));
                    contentValues.put(DataBaseHistoryLocation.DB_COLUMN_LATITUDE_WGS84, String.valueOf(mCurLat));
                    contentValues.put(DataBaseHistoryLocation.DB_COLUMN_TIMESTAMP, System.currentTimeMillis() / 1000);
                    contentValues.put(DataBaseHistoryLocation.DB_COLUMN_LONGITUDE_CUSTOM, Double.toString(mCurLatLngMap.longitude));
                    contentValues.put(DataBaseHistoryLocation.DB_COLUMN_LATITUDE_CUSTOM, Double.toString(mCurLatLngMap.latitude));

                    if (saveSelectedLocation(mLocationHistoryDB, contentValues)) {
                        XLog.d("DATABASE: saveSelectedLocation[HistoryLocation] success");
                    } else {
                        XLog.e("DATABASE: saveSelectedLocation[HistoryLocation] error");
                    }
                }
            } catch (JSONException e) {
                XLog.e("JSON: resolve json error");
                //插表参数
                ContentValues contentValues = new ContentValues();
                contentValues.put(DataBaseHistoryLocation.DB_COLUMN_LOCATION, "NULL");
                contentValues.put(DataBaseHistoryLocation.DB_COLUMN_LONGITUDE_WGS84, String.valueOf(mCurLng));
                contentValues.put(DataBaseHistoryLocation.DB_COLUMN_LATITUDE_WGS84, String.valueOf(mCurLat));
                contentValues.put(DataBaseHistoryLocation.DB_COLUMN_TIMESTAMP, System.currentTimeMillis() / 1000);
                contentValues.put(DataBaseHistoryLocation.DB_COLUMN_LONGITUDE_CUSTOM, Double.toString(mCurLatLngMap.longitude));
                contentValues.put(DataBaseHistoryLocation.DB_COLUMN_LATITUDE_CUSTOM, Double.toString(mCurLatLngMap.latitude));

                if (saveSelectedLocation(mLocationHistoryDB, contentValues)) {
                    XLog.d("DATABASE: saveSelectedLocation[HistoryLocation] success");
                } else {
                    XLog.e("DATABASE: saveSelectedLocation[HistoryLocation] error");
                }

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
            contentValues.put(DataBaseHistoryLocation.DB_COLUMN_LONGITUDE_CUSTOM, Double.toString(mCurLatLngMap.longitude));
            contentValues.put(DataBaseHistoryLocation.DB_COLUMN_LATITUDE_CUSTOM, Double.toString(mCurLatLngMap.latitude));

            if (saveSelectedLocation(mLocationHistoryDB, contentValues)) {
                XLog.d("DATABASE: saveSelectedLocation[HistoryLocation] success");
            } else {
                XLog.e("DATABASE: saveSelectedLocation[HistoryLocation] error");
            }
        });
        // 给请求设置tag
        stringRequest.setTag("MapAPI");
        // 添加tag到请求队列
        mRequestQueue.add(stringRequest);
    }

    private void doGoLocation() {
        if (!isMockServStart) {
            XLog.d("Current Baidu LatLng: " + mCurLatLngMap.longitude + "  " + mCurLatLngMap.latitude);

            markSelectedPosition();

            //start mock location service
            Intent serviceGoIntent = new Intent(MainActivity.this, ServiceGo.class);
            serviceGoIntent.putExtra(LNG_MSG_ID, mCurLng);
            serviceGoIntent.putExtra(LAT_MSG_ID, mCurLat);

            //save record
            recordGetPositionInfo();

            //insert end
            startForegroundService(serviceGoIntent);
            XLog.d("startForegroundService: ServiceGo");

            isMockServStart = true;
//                            Snackbar.make(view, "位置模拟已开启", Snackbar.LENGTH_LONG)
//                                    .setAction("Action", null).show();
            mButtonStart.hide();
            mButtonStop.show();
            //track
        } else {
//                            Snackbar.make(view, "位置模拟已在运行", Snackbar.LENGTH_LONG)
//                                    .setAction("Action", null).show();
            mButtonStart.hide();
            mButtonStop.show();
            isMockServStart = true;
        }
    }

    private void startGoLocation() {
        if (!isLimit && GoUtils.isNetworkAvailable(this)) {    // 时间限制
            //悬浮窗权限判断
            if (!Settings.canDrawOverlays(getApplicationContext())) {
                showEnableFloatWindowDialog();
            } else {
                if (!GoUtils.isGpsOpened(this)) {
                    showEnableGpsDialog();
                } else {
                    if (!GoUtils.isAllowMockLocation(this)) {
                        showEnableMockLocationDialog();
                    } else {
                        doGoLocation();
                    }
                }
            }
        }
    }

    private void stopGoLocation() {
        if (isMockServStart) {
            //end mock location
            Intent serviceGoIntent = new Intent(MainActivity.this, ServiceGo.class);
            stopService(serviceGoIntent);
//            Snackbar.make(v, "位置模拟服务终止", Snackbar.LENGTH_LONG)
//                    .setAction("Action", null).show();
            //service finish
            isMockServStart = false;
            //mButtonStart.setVisibility(View.VISIBLE);
            mButtonStart.show();
            //mButtonStop.setVisibility(View.INVISIBLE);
            mButtonStop.hide();
        }
    }

    private void setGoBtnListener() {
        //应用内悬浮按钮
        mButtonStart = findViewById(id.faBtnStart);
        mButtonStop = findViewById(id.faBtnStop);
        mButtonStart.setOnClickListener(view -> startGoLocation());
        mButtonStop.setOnClickListener(v -> stopGoLocation());
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
