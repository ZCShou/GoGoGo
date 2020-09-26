package com.zcshou.gogogo;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.preference.PreferenceManager;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BaiduMapOptions;
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
import com.baidu.mapapi.search.sug.SuggestionResult;
import com.baidu.mapapi.search.sug.SuggestionSearch;
import com.baidu.mapapi.search.sug.SuggestionSearchOption;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
// import com.google.android.material.snackbar.Snackbar;

import org.apache.log4j.Logger;
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

import com.zcshou.log4j.LogUtil;
import com.zcshou.service.GoService;
import com.zcshou.database.HistoryLocationDataBaseHelper;
import com.zcshou.database.HistorySearchDataBaseHelper;
import com.zcshou.service.GoSntpClient;
import com.zcshou.utils.MapUtils;

import static android.view.View.GONE;
import static com.zcshou.gogogo.R.drawable;
import static com.zcshou.gogogo.R.id;
import static com.zcshou.gogogo.R.layout;
import static com.zcshou.gogogo.R.string;
import static com.zcshou.service.GoService.RunCode;
import static com.zcshou.service.GoService.StopCode;

public class MainActivity extends BaseActivity
        implements NavigationView.OnNavigationItemSelectedListener, SensorEventListener {

    private boolean isServiceRun = false;
    private boolean isMockServStart = false;
    private boolean isGPSOpen = false;
    private boolean isFirstLoc = true; // 是否首次定位

    private static final long mTS = 1607472000;

    //位置历史
    private SQLiteDatabase locHistoryDB;
    //搜索历史
    private HistorySearchDataBaseHelper mHistorySearchHelper;
    private SQLiteDatabase searchHistoryDB;

    // http
    private RequestQueue mRequestQueue;

    // 定位相关
    LocationClient mLocClient = null;
    public LocationGoListener myListener = new LocationGoListener();
    BitmapDescriptor mCurrentMarker;
    private Double lastX = 0.0;
    private int mCurrentDirection = 0;
    private double mCurrentLat = 0.0;
    private double mCurrentLon = 0.0;
    private float mCurrentAccracy;
    private String mCurrentCity = "济南市";

    private SensorManager mSensorManager;
    private Sensor mSensor;

    // 当前经度&纬度
    private static String curLatLng = "117.027707&36.667662";
    // 当前地点击的点
    public static LatLng curMapLatLng = new LatLng(36.547743718042415, 117.07018449827267);
    public static BitmapDescriptor bdA = BitmapDescriptorFactory
            .fromResource(drawable.icon_gcoding);

    public MapView mMapView;
    public static BaiduMap mBaiduMap = null;
    private boolean isMapLoc;

    // UI相关
    NavigationView mNavigationView;
    RadioGroup.OnCheckedChangeListener mMapTypeListener;
    RadioGroup.OnCheckedChangeListener mGroupMapTrackListener;
    private MyLocationData locData;

    private FloatingActionButton faBtnStart;
    private FloatingActionButton faBtnStop;

    //位置搜索相关
    private SearchView searchView;
    private ListView mSearchList;
    private ListView mSearchHistoryList;
    private SimpleAdapter simAdapt;
    private LinearLayout mSearchlinearLayout;
    private LinearLayout mHistorylinearLayout;
    private MenuItem searchItem;
    private boolean isSubmit;
    private SuggestionSearch mSuggestionSearch;

    private boolean isLimit = true;

    CheckBox mPtlCheck;
    SharedPreferences sharedPreferences;

    //log debug
    private static final Logger log = Logger.getLogger(MainActivity.class);

    private InterstitialAd mInterstitialAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(layout.activity_main);

        Toolbar toolbar = findViewById(id.toolbar);

        setSupportActionBar(toolbar);

        try {
            LogUtil.configLog();
        } catch (Exception e) {
            Log.e("Log", "LogUtil config error");
            e.printStackTrace();
        }

        Log.d("MainActivity", "onCreate");
        log.debug("MainActivity: onCreate");

        //sqlite相关
        try {
            //定位历史
            HistoryLocationDataBaseHelper HistoryLocationDataBaseHelper = new HistoryLocationDataBaseHelper(getApplicationContext());
            locHistoryDB = HistoryLocationDataBaseHelper.getWritableDatabase();
            // 搜索历史
            mHistorySearchHelper = new HistorySearchDataBaseHelper(getApplicationContext());
            searchHistoryDB = mHistorySearchHelper.getWritableDatabase();
        } catch (Exception e) {
            Log.e("DATABASE", "sqlite init error");
            log.error("DATABASE: sqlite init error");
            e.printStackTrace();
        }

        DrawerLayout drawer = findViewById(id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, string.navigation_drawer_open, string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        mNavigationView = findViewById(id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(this);

        //http init
        mRequestQueue = Volley.newRequestQueue(this);

        //注册GoService广播接收器
        try {
            MainActivity.GoServiceReceiver GoServiceReceiver = new GoServiceReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction("com.zcshou.service.GoService");
            this.registerReceiver(GoServiceReceiver, filter);
        } catch (Exception e) {
            Log.e("UNKNOWN", "registerReceiver error");
            e.printStackTrace();
        }

        //
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);// 获取传感器管理服务
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        initBaiduMap();

        //网络是否可用
        if (!isNetworkAvailable()) {
            DisplayToast("网络连接不可用,请检查网络连接设置");
        }

        //gps是否开启
        if (!(isGPSOpen = isGpsOpened())) {
            DisplayToast("GPS定位未开启，请先打开GPS定位服务");
            isMapLoc = false;
        } else {
            // 如果GPS定位开启，则打开定位图层
            openMapLocateLayer();
            isMapLoc = true;
        }

        // 地图按键的监听
        setMapBtnGroupListener();

        // set 开始定位 listener
        setGoBtnListener();

        // 搜索相关
        searchView = findViewById(id.action_search);
        mSearchList = findViewById(id.search_list_view);
        mSearchlinearLayout = findViewById(id.search_linear);
        mSearchHistoryList = findViewById(id.search_history_list_view);
        mHistorylinearLayout = findViewById(id.search_history_linear);

        //搜索结果列表的点击监听
        setSearchResultClickListener();

        //搜索历史列表的点击监听
        setSearchHistoryClickListener();

        //设置搜索建议返回值监听
        setSearchSuggestListener();

        setUserLimitInfo();

        // mWelDT = getIntent().getLongExtra("DT", 0);

        TimeTask timeTask = new TimeTask();
        ExecutorService threadExecutor = Executors.newSingleThreadExecutor();
        threadExecutor.submit(timeTask);

        // 这里记录启动次数
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        long num = sharedPreferences.getLong("setting_startup_num", 0);
        sharedPreferences.edit()
                .putLong("setting_startup_num", ++num)
                .apply();

        initGoogleAD();
    }

    @Override
    protected void onPause() {
        Log.d("MainActivity", "onPause");
        log.debug("MainActivity: onPause");
        mMapView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.d("MainActivity", "onPause");
        log.debug("MainActivity: onPause");
        mMapView.onResume();
        super.onResume();
        //为系统的方向传感器注册监听器
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onStop() {
        Log.d("MainActivity", "onStop");
        log.debug("MainActivity: onStop");
        //取消注册传感器监听
        mSensorManager.unregisterListener(this);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d("MainActivity", "onDestroy");

        if (isMockServStart) {
            Intent mockLocServiceIntent = new Intent(MainActivity.this, GoService.class);
            stopService(mockLocServiceIntent);
        }

        // 退出时销毁定位
        if (isMapLoc) {
            mLocClient.stop();
        }

        // 关闭定位图层
        mBaiduMap.setMyLocationEnabled(false);
        mMapView.onDestroy();

        mMapView = null;

        //poi search destroy
        mSuggestionSearch.destroy();

        //close db
        locHistoryDB.close();
        searchHistoryDB.close();

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
                mSearchlinearLayout.setVisibility(View.INVISIBLE);
                mHistorylinearLayout.setVisibility(View.INVISIBLE);
                return true;  // Return true to collapse action view
            }
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                // Do something when expanded
                menu.setGroupVisible(0, false);
                menu.setGroupVisible(1, false);
                mSearchlinearLayout.setVisibility(View.INVISIBLE);
                //展示搜索历史
                List<Map<String, Object>> data = getSearchHistory();

                if (data.size() > 0) {
                    simAdapt = new SimpleAdapter(
                            MainActivity.this,
                            data,
                            layout.search_record_item,
                            new String[] {"search_key", "search_description", "search_timestamp", "search_isLoc", "search_longitude", "search_latitude"}, // 与下面数组元素要一一对应
                            new int[] {id.search_key, id.search_description, id.search_timestamp, id.search_isLoc, id.search_longitude, id.search_latitude});
                    mSearchHistoryList.setAdapter(simAdapt);
                    mHistorylinearLayout.setVisibility(View.VISIBLE);
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
                    contentValues.put("SearchKey", query);
                    contentValues.put("Description", "搜索...");
                    contentValues.put("IsLocate", 0);
                    contentValues.put("TimeStamp", System.currentTimeMillis() / 1000);

                    if (!saveSelectSearchItem(searchHistoryDB, contentValues)) {
                        Log.e("DATABASE", "saveSelectSearchItem[SearchHistory] error");
                        log.error("DATABASE: saveSelectSearchItem[SearchHistory] error");
                    } else {
                        Log.d("DATABASE", "saveSelectSearchItem[SearchHistory] success");
                        log.debug("DATABASE: saveSelectSearchItem[SearchHistory] success");
                    }

                    mBaiduMap.clear();
                    mSearchlinearLayout.setVisibility(View.INVISIBLE);
                } catch (Exception e) {
                    DisplayToast("搜索失败，请检查网络连接");
                    Log.d("HTTP", "搜索失败，请检查网络连接");
                    log.debug("HTTP: 搜索失败，请检查网络连接");
                    e.printStackTrace();
                }

                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                //当输入框内容改变的时候回调
                //搜索历史置为不可见
                mHistorylinearLayout.setVisibility(View.INVISIBLE);

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
                        Log.d("HTTP", "搜索失败，请检查网络连接");
                        log.debug("HTTP: 搜索失败，请检查网络连接");
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
    public boolean onNavigationItemSelected(MenuItem item) {
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
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        double x = sensorEvent.values[0];

        if (Math.abs(x - lastX) > 1.0) {
            mCurrentDirection = (int) x;
            locData = new MyLocationData.Builder()
                    .accuracy(mCurrentAccracy)
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(mCurrentDirection).latitude(mCurrentLat)
                    .longitude(mCurrentLon).build();
            mBaiduMap.setMyLocationData(locData);
        }

        lastX = x;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    //判断GPS是否打开
    private boolean isGpsOpened() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    //模拟位置权限是否开启
    public boolean isAllowMockLocation() {
        boolean canMockPosition;

        if (Build.VERSION.SDK_INT <= 22) {//6.0以下
            canMockPosition = Settings.Secure.getInt(this.getContentResolver(), Settings.Secure.ALLOW_MOCK_LOCATION, 0) != 0;
        } else {
            try {
                LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);//获得LocationManager引用
                String providerStr = LocationManager.GPS_PROVIDER;
                LocationProvider provider = locationManager.getProvider(providerStr);

                // 为防止在已有testProvider的情况下导致addTestProvider抛出异常，先移除testProvider
                try {
                    locationManager.removeTestProvider(providerStr);
                    Log.d("PERMISSION", "try to move test provider");
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("PERMISSION", "try to move test provider");
                }

                if (provider != null) {
                    try {
                        locationManager.addTestProvider(
                                provider.getName()
                                , provider.requiresNetwork()
                                , provider.requiresSatellite()
                                , provider.requiresCell()
                                , provider.hasMonetaryCost()
                                , provider.supportsAltitude()
                                , provider.supportsSpeed()
                                , provider.supportsBearing()
                                , provider.getPowerRequirement()
                                , provider.getAccuracy());
                        canMockPosition = true;
                    } catch (Exception e) {
                        Log.e("FUCK", "add origin gps test provider error");
                        canMockPosition = false;
                        e.printStackTrace();
                    }
                } else {
                    try {
                        locationManager.addTestProvider(
                                providerStr
                                , true, true, false, false, true, true, true
                                , Criteria.POWER_HIGH, Criteria.ACCURACY_FINE);
                        canMockPosition = true;
                    } catch (Exception e) {
                        Log.e("FUCK", "add gps test provider error");
                        canMockPosition = false;
                        e.printStackTrace();
                    }
                }

                // 模拟位置可用
                if (canMockPosition) {
                    locationManager.setTestProviderEnabled(providerStr, true);
                    locationManager.setTestProviderStatus(providerStr, LocationProvider.AVAILABLE, null, System.currentTimeMillis());
                    //remove test provider
                    locationManager.setTestProviderEnabled(providerStr, false);
                    locationManager.removeTestProvider(providerStr);
                }
            } catch (SecurityException e) {
                canMockPosition = false;
                e.printStackTrace();
            }
        }

        return canMockPosition;
    }

    //WIFI是否可用
    private boolean isWifiConnected() {
        ConnectivityManager mConnectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWiFiNetworkInfo = mConnectivityManager
                .getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (mWiFiNetworkInfo != null) {
            return mWiFiNetworkInfo.isAvailable();
        }

        return false;
    }

    //MOBILE网络是否可用
    private boolean isMobileConnected() {
        ConnectivityManager mConnectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mMobileNetworkInfo = mConnectivityManager
                .getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        if (mMobileNetworkInfo != null) {
            return mMobileNetworkInfo.isAvailable();
        }

        return false;
    }

    // 断是否有网络连接，但是如果该连接的网络无法上网，也会返回true
    public boolean isNetworkConnected() {
        ConnectivityManager mConnectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();

        if (mNetworkInfo != null) {
            return mNetworkInfo.isAvailable();
        }

        return false;
    }

    //网络是否可用
    private boolean isNetworkAvailable() {
        return ((isWifiConnected() || isMobileConnected()) && isNetworkConnected());
    }

    //提醒开启位置模拟的弹框
    private void showEnableMockLocationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("启用位置模拟")//这里是表头的内容
                .setMessage("请在\"开发者选项→选择模拟位置信息应用\"中进行设置")//这里是中间显示的具体信息
                .setPositiveButton("设置",//这个string是设置左边按钮的文字
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
                                    startActivity(intent);
                                } catch (Exception e) {
                                    DisplayToast("无法跳转到开发者选项,请先确保您的设备已处于开发者模式");
                                    e.printStackTrace();
                                }
                            }
                        })//setPositiveButton里面的onClick执行的是左边按钮
                .setNegativeButton("取消",//这个string是设置右边按钮的文字
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })//setNegativeButton里面的onClick执行的是右边的按钮的操作
                .show();
    }

    //提醒开启悬浮窗的弹框
    private void showEnableFloatWindowDialog() {
        new AlertDialog.Builder(this)
                .setTitle("启用悬浮窗")//这里是表头的内容
                .setMessage("为了模拟定位的稳定性，建议开启\"显示悬浮窗\"选项")//这里是中间显示的具体信息
                .setPositiveButton("设置",//这个string是设置左边按钮的文字
                        new DialogInterface.OnClickListener() {
                            @RequiresApi(api = Build.VERSION_CODES.M)
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                                    startActivity(intent);
                                } catch (Exception e) {
                                    DisplayToast("无法跳转到设置界面，请在权限管理中开启该应用的悬浮窗");
                                    e.printStackTrace();
                                }
                            }
                        })//setPositiveButton里面的onClick执行的是左边按钮
                .setNegativeButton("取消",//这个string是设置右边按钮的文字
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })//setNegativeButton里面的onClick执行的是右边的按钮的操作
                .show();
    }

    //显示开启GPS的提示
    private void showEnableGpsDialog() {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Tips")//这里是表头的内容
                .setMessage("是否开启GPS定位服务?")//这里是中间显示的具体信息
                .setPositiveButton("确定",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivityForResult(intent, 0);
                            }
                        })
                .setNegativeButton("取消",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
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
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
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
                        curMapLatLng = new LatLng(dialog_lat_double, dialog_lng_double);
                        MapStatusUpdate mapstatusupdate = MapStatusUpdateFactory.newLatLng(curMapLatLng);
                        //对地图的中心点进行更新
                        mBaiduMap.setMapStatus(mapstatusupdate);
                        markSelectedPosition();
                        transformCoordinate(dialog_lng_str, dialog_lat_str);
                    }
                } catch (Exception e) {
                    DisplayToast("获取经纬度出错,请检查输入是否正确");
                    e.printStackTrace();
                }
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
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

    private void initGoogleAD() {
        // 横幅广告
        AdView mAdView = findViewById(R.id.ad_view);
        // Create an ad request.
        AdRequest adRequest = new AdRequest.Builder().build();
        // Start loading the ad in the background.
        mAdView.loadAd(adRequest);
        mAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                // Code to be executed when an ad finishes loading.
            }

            @Override
            public void onAdFailedToLoad(LoadAdError adError) {
                String error = String.format(Locale.getDefault(),
                        "domain: %s, code: %d, message: %s",
                        adError.getDomain(), adError.getCode(), adError.getMessage());

                Log.d("DEBUG", "markSelectedPosition");
                log.debug("Main Banner onAdFailedToLoad() with error: " + error);
            }

            @Override
            public void onAdOpened() {
                // Code to be executed when an ad opens an overlay that
                // covers the screen.
            }

            @Override
            public void onAdClicked() {
                // Code to be executed when the user clicks on an ad.
            }

            @Override
            public void onAdLeftApplication() {
                // Code to be executed when the user has left the app.
            }

            @Override
            public void onAdClosed() {
                // Code to be executed when the user is about to return
                // to the app after tapping on an ad.
            }
        });

        // 插页广告
        // Create the InterstitialAd and set the adUnitId.
        mInterstitialAd = new InterstitialAd(this);
        // Defined in res/values/strings.xml
        mInterstitialAd.setAdUnitId(getString(string.ad_unit_id_go_start));
        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                // Code to be executed when an ad finishes loading.
            }

            @Override
            public void onAdFailedToLoad(LoadAdError loadAdError) {
                // Code to be executed when an ad request fails.
                String error = String.format(Locale.getDefault(),
                        "domain: %s, code: %d, message: %s",
                        loadAdError.getDomain(), loadAdError.getCode(), loadAdError.getMessage());

                Log.d("DEBUG", "markSelectedPosition");
                log.debug("GoStart onAdFailedToLoad() with error: " + error);
            }

            @Override
            public void onAdOpened() {
                // Code to be executed when the ad is displayed.
            }

            @Override
            public void onAdClicked() {
                // Code to be executed when the user clicks on an ad.
            }

            @Override
            public void onAdLeftApplication() {
                // Code to be executed when the user has left the app.
            }

            @Override
            public void onAdClosed() {
                // Code to be executed when the interstitial ad is closed.
                requestGoogleAD();

                startGoLocation();
            }
        });

        requestGoogleAD();
    }

    private void showGoogleAD() {
        if (mInterstitialAd != null && mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
        } else {
            startGoLocation();
        }
    }

    private void requestGoogleAD() {
        if (mInterstitialAd != null && !mInterstitialAd.isLoading() && !mInterstitialAd.isLoaded()) {
            AdRequest adRequest = new AdRequest.Builder().build();
            mInterstitialAd.loadAd(adRequest);
        }
    }

    private void setUserLimitInfo() {
        // 从上到下逐级获取
        View navHeaderView = mNavigationView.getHeaderView(0);
        TextView mUserLimitInfo = navHeaderView.findViewById(R.id.user_limit);
        TextView mUserName = navHeaderView.findViewById(R.id.user_name);

        mUserName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DrawerLayout drawer = findViewById(id.drawer_layout);

                if (drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.closeDrawer(GravityCompat.START);
                }
                showRegisterDialog();
            }
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
            mPtlCheck = window.findViewById(id.reg_check);
            final TextView regResp = window.findViewById(id.reg_response);
            final TextView regUserName = window.findViewById(id.reg_user_name);

            mPtlCheck.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mPtlCheck.isChecked()) {
                        showProtocolDialog();
                    }
                }
            });

            regCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    alertDialog.cancel();
                }
            });

            regAgree.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!mPtlCheck.isChecked()) {
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
                }
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
            String str = "1. 本软件专为学习 Android 开发使用，不会收集任何用户数据。"
                    + "严禁利用本软件侵犯他人隐私权或者用于游戏牟利，如软件使用者不能遵守此规定， 请立即删除。"
                    + "对于因用户使用本软件而造成自身或他人隐私泄露等任何不良后果，均由用户自行承担，软件作者不负任何责任。\n"
                    + "2. 用户不得对本软件产品进行反向工程（reverse engineer）、反向编译（decompile）或反汇编（disassemble）， 违者属于侵权行为，并自行承担由此产生的不利后果。\n"
                    + "3. 软件保证不含任何病毒，木马，等破坏用户数据的恶意代码，但是由于本软件产品可以通过网络等途径下载、传播，对于从非软件作者指定站点下载的本软件产品软件作者无法保证该软件是否感染计算机病毒、是否隐藏有伪装的特洛伊木马程序或者黑客软件，不承担由此引起的直接和间接损害责任。\n"
                    + "4. 软件会不断更新，以便及时为用户提供新功能和修正软件中的BUG。 同时软件作者保证本软件在升级过程中也不含有任何旨在破坏用户计算机数据的恶意代码。\n"
                    + "5. 由于用户计算机软硬件环境的差异性和复杂性，本软件所提供的各项功能并不能保证在任何情况下都能正常执行或达到用户所期望的结果。 用户使用本软件所产生的一切后果，软件作者不承担任何责任。\n"
                    + "6. 如果用户自行安装本软件，即表明用户信任软件作者，自愿选择安装本软件，并接受本协议所有条款。 如果用户不接受本协议，请立即删除。\n";

            SpannableStringBuilder ssb = new SpannableStringBuilder();
            ssb.append(str);

            tvContent.setMovementMethod(LinkMovementMethod.getInstance());
            tvContent.setText(ssb, TextView.BufferType.SPANNABLE);

            tvCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mPtlCheck.setChecked(false);
                    alertDialog.cancel();
                }
            });

            tvAgree.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mPtlCheck.setChecked(true);
                    alertDialog.cancel();
                }
            });
        }
    }

    private void initBaiduMap() {
        // 地图初始化
        mMapView = findViewById(id.bmapView);
        mMapView.showZoomControls(false);
        mBaiduMap = mMapView.getMap();
        mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
        mBaiduMap.setMyLocationEnabled(true);
        initMapListener();
    }

    //开启地图的定位图层
    private void openMapLocateLayer() {
        // 定位初始化
        mLocClient = new LocationClient(this);
        mLocClient.registerLocationListener(myListener);
        LocationClientOption option = new LocationClientOption();
        option.setIsNeedAddress(true);
        option.setOpenGps(true); // 打开gps
        option.setCoorType("bd09ll"); // 设置坐标类型
        option.setScanSpan(1000);
        mLocClient.setLocOption(option);
        mLocClient.start();
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

    //对地图事件的消息响应
    private void initMapListener() {
        mBaiduMap.setOnMapTouchListener(new BaiduMap.OnMapTouchListener() {
            @Override
            public void onTouch(MotionEvent event) {
            }
        });
        mBaiduMap.setOnMapClickListener(new BaiduMap.OnMapClickListener() {
            /**
             * 单击地图
             */
            public void onMapClick(LatLng point) {
                curMapLatLng = point;
                //百度坐标系转wgs坐标系
                transformCoordinate(String.valueOf(point.longitude), String.valueOf(point.latitude));
                markSelectedPosition();
            }
            /**
             * 单击地图中的POI点
             */
            public void onMapPoiClick(MapPoi poi) {
                curMapLatLng = poi.getPosition();
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
                curMapLatLng = point;
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
                curMapLatLng = point;
                //百度坐标系转wgs坐标系
                transformCoordinate(String.valueOf(point.longitude), String.valueOf(point.latitude));
                markSelectedPosition();
            }
        });

        mBaiduMap.setOnMapStatusChangeListener(new BaiduMap.OnMapStatusChangeListener() {
            public void onMapStatusChangeStart(MapStatus status) {
                // markSelectedPosition();
            }
            @Override
            public void onMapStatusChangeStart(MapStatus status, int reason) {
            }
            public void onMapStatusChangeFinish(MapStatus status) {
                // markSelectedPosition();
            }
            public void onMapStatusChange(MapStatus status) {
                // markSelectedPosition();
            }
        });
    }

    //地图上各按键的监听
    private void setMapBtnGroupListener() {
        RadioGroup mGroupMapTrack = this.findViewById(id.RadioGroupMapTrack);

        mGroupMapTrackListener = new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == id.normalloc) {
                    mBaiduMap.setMyLocationConfiguration(new MyLocationConfiguration(
                            MyLocationConfiguration.LocationMode.NORMAL, true, mCurrentMarker));
                    MapStatus.Builder builder1 = new MapStatus.Builder();
                    builder1.overlook(0);
                    mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder1.build()));
                }

                if (checkedId == id.trackloc) {
                    mBaiduMap.setMyLocationConfiguration(new MyLocationConfiguration(
                            MyLocationConfiguration.LocationMode.FOLLOWING, true, mCurrentMarker));
                    MapStatus.Builder builder = new MapStatus.Builder();
                    builder.overlook(0);
                    mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
                }

                if (checkedId == id.compassloc) {
                    mBaiduMap.setMyLocationConfiguration(new MyLocationConfiguration(
                            MyLocationConfiguration.LocationMode.COMPASS, true, mCurrentMarker));
                }
            }
        };
        mGroupMapTrack.setOnCheckedChangeListener(mGroupMapTrackListener);

        RadioGroup mGroupMapType = this.findViewById(id.RadioGroupMapType);

        mMapTypeListener = new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == id.normal) {
                    mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
                }

                if (checkedId == id.statellite) {
                    mBaiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
                }
            }
        };
        mGroupMapType.setOnCheckedChangeListener(mMapTypeListener);
    }

    //标定选择的位置
    private void markSelectedPosition() {
        Log.d("DEBUG", "markSelectedPosition");
        log.debug("markSelectedPosition");
        
        if (curMapLatLng != null) {
            MarkerOptions ooA = new MarkerOptions().position(curMapLatLng).icon(bdA);
            mBaiduMap.clear();
            mBaiduMap.addOverlay(ooA);
        }
    }

    //重置地图
    private void resetMap() {
        if (isMapLoc) {
            mBaiduMap.clear();

            mLocClient.requestLocation();   /* 请求位置 */

            MapStatusUpdate mapstatusupdate = MapStatusUpdateFactory.newLatLng(new LatLng(mCurrentLat, mCurrentLon));
            //对地图的中心点进行更新
            mBaiduMap.setMapStatus(mapstatusupdate);
            //更新当前位置
            curMapLatLng = new LatLng(mCurrentLat, mCurrentLon);
            transformCoordinate(Double.toString(curMapLatLng.longitude), Double.toString(curMapLatLng.latitude));
        }
    }

    //坐标转换
    private void transformCoordinate(final String longitude, final String latitude) {
        //参数坐标系：bd09
        // boolean isInCHN=false;
        final double error = 0.00000001;
        final String mcode = getResources().getString(string.safecode);
        final String ak = getResources().getString(string.ak);
        //判断bd09坐标是否在国内
        String mapApiUrl = "https://api.map.baidu.com/geoconv/v1/?coords=" + longitude + "," + latitude +
                "&from=5&to=3&ak=" + ak + "&mcode=" + mcode;
        Log.d("transformCoordinate", mapApiUrl);
        log.debug("transformCoordinate: " + mapApiUrl);
        //bd09坐标转gcj02
        StringRequest stringRequest = new StringRequest(mapApiUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject getRetJson = new JSONObject(response);
                            log.debug("transformCoordinate:" + getRetJson.toString());

                            //如果api接口转换成功
                            if (Integer.parseInt(getRetJson.getString("status")) == 0) {
                                Log.d("HTTP", "call api[bd09_to_gcj02] success");
                                log.debug("HTTP: call api[bd09_to_gcj02] success");
                                JSONArray coordinateArr = getRetJson.getJSONArray("result");
                                JSONObject coordinate = coordinateArr.getJSONObject(0);
                                String gcj02Longitude = coordinate.getString("x");
                                String gcj02Latitude = coordinate.getString("y");
                                Log.d("DEBUG", "bd09Longitude is " + longitude);
                                Log.d("DEBUG", "bd09Latitude is " + latitude);
                                Log.d("DEBUG", "gcj02Longitude is " + gcj02Longitude);
                                Log.d("DEBUG", "gcj02Latitude is " + gcj02Latitude);
                                log.debug("bd09Longitude is " + longitude + ", " + "bd09Latitude is " + latitude);
                                log.debug("gcj02Longitude is " + gcj02Longitude + ", " + "gcj02Latitude is " + gcj02Latitude);
                                BigDecimal bigDecimalGcj02Longitude = BigDecimal.valueOf(Double.parseDouble(gcj02Longitude));
                                BigDecimal bigDecimalGcj02Latitude = BigDecimal.valueOf(Double.parseDouble(gcj02Latitude));
                                BigDecimal bigDecimalBd09Longitude = BigDecimal.valueOf(Double.parseDouble(longitude));
                                BigDecimal bigDecimalBd09Latitude = BigDecimal.valueOf(Double.parseDouble(latitude));
                                double gcj02LongitudeDouble = bigDecimalGcj02Longitude.setScale(9, BigDecimal.ROUND_HALF_UP).doubleValue();
                                double gcj02LatitudeDouble = bigDecimalGcj02Latitude.setScale(9, BigDecimal.ROUND_HALF_UP).doubleValue();
                                double bd09LongitudeDouble = bigDecimalBd09Longitude.setScale(9, BigDecimal.ROUND_HALF_UP).doubleValue();
                                double bd09LatitudeDouble = bigDecimalBd09Latitude.setScale(9, BigDecimal.ROUND_HALF_UP).doubleValue();
                                Log.d("DEBUG", "gcj02LongitudeDouble is " + gcj02LongitudeDouble);
                                Log.d("DEBUG", "gcj02LatitudeDouble is " + gcj02LatitudeDouble);
                                Log.d("DEBUG", "bd09LongitudeDouble is " + bd09LongitudeDouble);
                                Log.d("DEBUG", "bd09LatitudeDouble is " + bd09LatitudeDouble);
                                log.debug("gcj02LongitudeDouble is " + gcj02LongitudeDouble + ", " + "gcj02LatitudeDouble is " + gcj02LatitudeDouble);
                                log.debug("bd09LongitudeDouble is " + bd09LongitudeDouble + ", " + "bd09LatitudeDouble is " + bd09LatitudeDouble);

                                //如果bd09转gcj02 结果误差很小  认为该坐标在国外
                                if ((Math.abs(gcj02LongitudeDouble - bd09LongitudeDouble)) <= error && (Math.abs(gcj02LatitudeDouble - bd09LatitudeDouble)) <= error) {
                                    //不进行坐标转换
                                    curLatLng = longitude + "&" + latitude;
                                    Log.d("DEBUG", "OUT OF CHN, NO NEED TO TRANSFORM COORDINATE");
                                    log.debug("OUT OF CHN, NO NEED TO TRANSFORM COORDINATE");
                                    // DisplayToast("OUT OF CHN, NO NEED TO TRANSFORM COORDINATE");
                                } else {
                                    //离线转换坐标系
                                    // double latLng[] = MapUtils.bd2wgs(Double.parseDouble(longitude), Double.parseDouble(latitude));
                                    double[] latLng = MapUtils.gcj02towgs84(Double.parseDouble(gcj02Longitude), Double.parseDouble(gcj02Latitude));
                                    curLatLng = latLng[0] + "&" + latLng[1];
                                    Log.d("DEBUG", "IN CHN, NEED TO TRANSFORM COORDINATE");
                                    log.debug("IN CHN, NEED TO TRANSFORM COORDINATE");
                                    // DisplayToast("IN CHN, NEED TO TRANSFORM COORDINATE");
                                }
                            }
                            //api接口转换失败 认为在国内
                            else {
                                //离线转换坐标系
                                double[] latLng = MapUtils.bd2wgs(Double.parseDouble(longitude), Double.parseDouble(latitude));
                                curLatLng = latLng[0] + "&" + latLng[1];
                                Log.d("DEBUG", "IN CHN, NEED TO TRANSFORM COORDINATE");
                                log.debug("IN CHN, NEED TO TRANSFORM COORDINATE");
                                // DisplayToast("BD Map Api Return not Zero, ASSUME IN CHN, NEED TO TRANSFORM COORDINATE");
                            }
                        } catch (JSONException e) {
                            Log.e("JSON", "resolve json error");
                            log.error("JSON: resolve json error");
                            e.printStackTrace();
                            //离线转换坐标系
                            double[] latLng = MapUtils.bd2wgs(Double.parseDouble(longitude), Double.parseDouble(latitude));
                            curLatLng = latLng[0] + "&" + latLng[1];
                            Log.d("DEBUG", "IN CHN, NEED TO TRANSFORM COORDINATE");
                            log.debug("IN CHN, NEED TO TRANSFORM COORDINATE");
                            // DisplayToast("Resolve JSON Error, ASSUME IN CHN, NEED TO TRANSFORM COORDINATE");
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //http 请求失败
                Log.e("HTTP", "HTTP GET FAILED");
                log.error("HTTP: HTTP GET FAILED");
                //离线转换坐标系
                double[] latLng = MapUtils.bd2wgs(Double.parseDouble(longitude), Double.parseDouble(latitude));
                curLatLng = latLng[0] + "&" + latLng[1];
                Log.d("DEBUG", "IN CHN, NEED TO TRANSFORM COORDINATE");
                log.debug("IN CHN, NEED TO TRANSFORM COORDINATE");
                // DisplayToast("HTTP Get Failed, ASSUME IN CHN, NEED TO TRANSFORM COORDINATE");
            }
        });
        // 给请求设置tag
        stringRequest.setTag("MapAPI");
        // 添加tag到请求队列
        mRequestQueue.add(stringRequest);
    }

    // 记录请求的位置信息
    private void recordGetPositionInfo() {
        //参数坐标系：bd09
        final String mcode = getResources().getString(string.safecode);
        final String ak = getResources().getString(string.ak);
        final String mapType = "bd09ll";
        //bd09坐标的位置信息
        String mapApiUrl = "https://api.map.baidu.com/reverse_geocoding/v3/?ak=" + ak + "&output=json&coordtype=" + mapType + "&location=" + curMapLatLng.latitude + "," + curMapLatLng.longitude + "&mcode=" + mcode;
        Log.d("recordGetPositionInfo", mapApiUrl);
        log.debug("recordGetPositionInfo:" + mapApiUrl);
        StringRequest stringRequest = new StringRequest(mapApiUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject getRetJson = new JSONObject(response);
                            Log.d("recordGetPositionInfo",  getRetJson.toString());
                            log.debug("recordGetPositionInfo:" + getRetJson.toString());

                            //位置获取成功
                            if (Integer.parseInt(getRetJson.getString("status")) == 0) {
                                JSONObject posInfoJson = getRetJson.getJSONObject("result");
                                String formatted_address = posInfoJson.getString("formatted_address");
                                // DisplayToast(tmp);
                                Log.d("ADDR", formatted_address);
                                log.debug(formatted_address);
                                //插表参数
                                ContentValues contentValues = new ContentValues();
                                contentValues.put("Location", formatted_address);
                                String[] latLngStr = curLatLng.split("&");
                                contentValues.put("WGS84Longitude", latLngStr[0]);
                                contentValues.put("WGS84Latitude", latLngStr[1]);
                                contentValues.put("TimeStamp", System.currentTimeMillis() / 1000);
                                contentValues.put("BD09Longitude", "" + curMapLatLng.longitude);
                                contentValues.put("BD09Latitude", "" + curMapLatLng.latitude);

                                if (saveSelectedLocation(locHistoryDB, contentValues)) {
                                    Log.d("DATABASE", "saveSelectedLocation[HistoryLocation] success");
                                    log.debug("DATABASE: saveSelectedLocation[HistoryLocation] success");
                                } else {
                                    Log.e("DATABASE", "saveSelectedLocation[HistoryLocation] error");
                                    log.error("DATABASE: saveSelectedLocation[HistoryLocation] error");
                                }
                            } else { //位置获取失败
                                //插表参数
                                ContentValues contentValues = new ContentValues();
                                contentValues.put("Location", "NULL");
                                String[] latLngStr = curLatLng.split("&");
                                contentValues.put("WGS84Longitude", latLngStr[0]);
                                contentValues.put("WGS84Latitude", latLngStr[1]);
                                contentValues.put("TimeStamp", System.currentTimeMillis() / 1000);
                                contentValues.put("BD09Longitude", "" + curMapLatLng.longitude);
                                contentValues.put("BD09Latitude", "" + curMapLatLng.latitude);

                                if (saveSelectedLocation(locHistoryDB, contentValues)) {
                                    Log.d("DATABASE", "saveSelectedLocation[HistoryLocation] success");
                                    log.debug("DATABASE: saveSelectedLocation[HistoryLocation] success");
                                } else {
                                    Log.e("DATABASE", "saveSelectedLocation[HistoryLocation] error");
                                    log.error("DATABASE: saveSelectedLocation[HistoryLocation] error");
                                }
                            }
                        } catch (JSONException e) {
                            Log.e("JSON", "resolve json error");
                            log.error("JSON: resolve json error");
                            //插表参数
                            ContentValues contentValues = new ContentValues();
                            contentValues.put("Location", "NULL");
                            String[] latLngStr = curLatLng.split("&");
                            contentValues.put("WGS84Longitude", latLngStr[0]);
                            contentValues.put("WGS84Latitude", latLngStr[1]);
                            contentValues.put("TimeStamp", System.currentTimeMillis() / 1000);
                            contentValues.put("BD09Longitude", "" + curMapLatLng.longitude);
                            contentValues.put("BD09Latitude", "" + curMapLatLng.latitude);

                            if (saveSelectedLocation(locHistoryDB, contentValues)) {
                                Log.d("DATABASE", "saveSelectedLocation[HistoryLocation] success");
                                log.debug("DATABASE: saveSelectedLocation[HistoryLocation] success");
                            } else {
                                Log.e("DATABASE", "saveSelectedLocation[HistoryLocation] error");
                                log.error("DATABASE: saveSelectedLocation[HistoryLocation] error");
                            }

                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //http 请求失败
                Log.e("HTTP", "HTTP GET FAILED");
                log.error("HTTP: HTTP GET FAILED");
                //插表参数
                ContentValues contentValues = new ContentValues();
                contentValues.put("Location", "NULL");
                String[] latLngStr = curLatLng.split("&");
                contentValues.put("WGS84Longitude", latLngStr[0]);
                contentValues.put("WGS84Latitude", latLngStr[1]);
                contentValues.put("TimeStamp", System.currentTimeMillis() / 1000);
                contentValues.put("BD09Longitude", "" + curMapLatLng.longitude);
                contentValues.put("BD09Latitude", "" + curMapLatLng.latitude);

                if (saveSelectedLocation(locHistoryDB, contentValues)) {
                    Log.d("DATABASE", "saveSelectedLocation[HistoryLocation] success");
                    log.debug("DATABASE: saveSelectedLocation[HistoryLocation] success");
                } else {
                    Log.e("DATABASE", "saveSelectedLocation[HistoryLocation] error");
                    log.error("DATABASE: saveSelectedLocation[HistoryLocation] error");
                }
            }
        });
        // 给请求设置tag
        stringRequest.setTag("MapAPI");
        // 添加tag到请求队列
        mRequestQueue.add(stringRequest);
    }

    // 在地图上显示历史位置
    public static boolean showHistoryLocation(String bd09Longitude, String bd09Latitude, String wgs84Longitude, String wgs84Latitude) {
        boolean ret = true;

        try {
            if (!bd09Longitude.isEmpty() && !bd09Latitude.isEmpty()) {
                curMapLatLng = new LatLng(Double.parseDouble(bd09Latitude), Double.parseDouble(bd09Longitude));
                MarkerOptions ooA = new MarkerOptions().position(curMapLatLng).icon(bdA);
                mBaiduMap.clear();
                mBaiduMap.addOverlay(ooA);
                MapStatusUpdate mapstatusupdate = MapStatusUpdateFactory.newLatLng(curMapLatLng);
                mBaiduMap.setMapStatus(mapstatusupdate);
                curLatLng = wgs84Longitude + "&" + wgs84Latitude;
            }
        } catch (Exception e) {
            ret = false;
            Log.e("UNKNOWN", "showHistoryLocation error");
            log.error("UNKNOWN: showHistoryLocation error");
            e.printStackTrace();
        }

        return ret;
    }

    // 保存选择的位置
    private boolean saveSelectedLocation(SQLiteDatabase sqLiteDatabase, ContentValues contentValues) {
        boolean insertRet = true;

        try {
            // 先删除原来的记录，再插入新记录
            String location = contentValues.get("Location").toString();
            sqLiteDatabase.delete(HistoryLocationDataBaseHelper.TABLE_NAME, "Location = ?", new String[] {location});
            sqLiteDatabase.insert(HistoryLocationDataBaseHelper.TABLE_NAME, null, contentValues);
        } catch (Exception e) {
            Log.e("DATABASE", "insert error");
            log.error("DATABASE: insert error");
            insertRet = false;
            e.printStackTrace();
        }

        return insertRet;
    }

    //保存搜索选项
    private boolean saveSelectSearchItem(SQLiteDatabase sqLiteDatabase, ContentValues contentValues) {
        boolean insertRet = true;

        try {
            // 先删除原来的记录，再插入新记录
            String searchKey = contentValues.get("SearchKey").toString();
            sqLiteDatabase.delete(HistorySearchDataBaseHelper.TABLE_NAME, "SearchKey = ?", new String[] {searchKey});
            sqLiteDatabase.insert(HistorySearchDataBaseHelper.TABLE_NAME, null, contentValues);
        } catch (Exception e) {
            Log.e("DATABASE", "insert error");
            log.error("DATABASE: insert error");
            insertRet = false;
            e.printStackTrace();
        }

        return insertRet;
    }

    //获取查询历史
    private List<Map<String, Object>> getSearchHistory() {
        List<Map<String, Object>> data = new ArrayList<>();

        try {
            Cursor cursor = mHistorySearchHelper.getWritableDatabase().query(HistorySearchDataBaseHelper.TABLE_NAME, null,
                    "ID > ?", new String[] {"0"},
                    null, null, "TimeStamp DESC", null);

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
            Log.e("DATABASE", "query error");
            log.error("DATABASE: query error");
            e.printStackTrace();
        }

        return data;
    }

    private  void startGoLocation() {
        if (!isLimit && isNetworkAvailable()) {    // 时间限制
            //悬浮窗权限判断
            if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(getApplicationContext())) {
                showEnableFloatWindowDialog();
            } else {
                isGPSOpen = isGpsOpened();
                if (!isGPSOpen) {
                    showEnableGpsDialog();
                } else {
                    //gps是否开启
                    if (!isMapLoc) {
                        // 如果GPS定位开启，则打开定位图层
                        openMapLocateLayer();
                        isMapLoc = true;
                    }

                    if (!isAllowMockLocation()) {
                        showEnableMockLocationDialog();
                    } else {
                        if (!isMockServStart && !isServiceRun) {
                            Log.d("DEBUG", "Current Baidu LatLng: " + curMapLatLng.longitude + "  " + curMapLatLng.latitude);
                            log.debug("Current Baidu LatLng: " + curMapLatLng.longitude + "  " + curMapLatLng.latitude);

                            markSelectedPosition();

                            //start mock location service
                            Intent mockLocServiceIntent = new Intent(MainActivity.this, GoService.class);
                            mockLocServiceIntent.putExtra("CurLatLng", curLatLng);

                            //save record
                            recordGetPositionInfo();

                            //insert end
                            if (Build.VERSION.SDK_INT >= 26) {
                                startForegroundService(mockLocServiceIntent);
                                Log.d("DEBUG", "startForegroundService: GoService");
                                log.debug("startForegroundService: GoService");
                            } else {
                                startService(mockLocServiceIntent);
                                Log.d("DEBUG", "startService: GoService");
                                log.debug("startService: GoService");
                            }

                            isMockServStart = true;
//                            Snackbar.make(view, "位置模拟已开启", Snackbar.LENGTH_LONG)
//                                    .setAction("Action", null).show();
                            faBtnStart.hide();
                            faBtnStop.show();
                            //track
                        } else {
//                            Snackbar.make(view, "位置模拟已在运行", Snackbar.LENGTH_LONG)
//                                    .setAction("Action", null).show();
                            faBtnStart.hide();
                            faBtnStop.show();
                            isMockServStart = true;
                        }
                    }
                }
            }
        }
    }

    private void stopGoLocation() {
        if (isMockServStart) {
            //end mock location
            Intent mockLocServiceIntent = new Intent(MainActivity.this, GoService.class);
            stopService(mockLocServiceIntent);
//            Snackbar.make(v, "位置模拟服务终止", Snackbar.LENGTH_LONG)
//                    .setAction("Action", null).show();
            //service finish
            isMockServStart = false;
            //faBtnStart.setVisibility(View.VISIBLE);
            faBtnStart.show();
            //faBtnStop.setVisibility(View.INVISIBLE);
            faBtnStop.hide();
        }
    }

    //set float action button listener
    private void setGoBtnListener() {
        //应用内悬浮按钮
        faBtnStart = findViewById(id.faBtnStart);
        faBtnStop = findViewById(id.faBtnStop);
        faBtnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showGoogleAD();
            }
        });
        faBtnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopGoLocation();
            }
        });
    }

    //设置 search list 点击监听
    private void setSearchResultClickListener() {
        mSearchList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String lng = ((TextView) view.findViewById(R.id.poi_longitude)).getText().toString();
                String lat = ((TextView) view.findViewById(R.id.poi_latitude)).getText().toString();
                // DisplayToast("lng is "+lng+"lat is "+lat);
                curMapLatLng = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));
                MapStatusUpdate mapstatusupdate = MapStatusUpdateFactory.newLatLng(curMapLatLng);
                //对地图的中心点进行更新，
                mBaiduMap.setMapStatus(mapstatusupdate);

                markSelectedPosition();

                transformCoordinate(lng, lat);
                
                // mSearchList.setVisibility(View.GONE);
                //搜索历史 插表参数
                ContentValues contentValues = new ContentValues();
                contentValues.put("SearchKey", ((TextView) view.findViewById(R.id.poi_name)).getText().toString());
                contentValues.put("Description", ((TextView) view.findViewById(R.id.poi_addr)).getText().toString());
                contentValues.put("IsLocate", 1);
                contentValues.put("BD09Longitude", lng);
                contentValues.put("BD09Latitude", lat);
                String[] wgsLatLngStr = curLatLng.split("&");
                contentValues.put("WGS84Longitude", wgsLatLngStr[0]);
                contentValues.put("WGS84Latitude", wgsLatLngStr[1]);
                contentValues.put("TimeStamp", System.currentTimeMillis() / 1000);

                if (saveSelectSearchItem(searchHistoryDB, contentValues)) {
                    Log.d("DATABASE", "saveSelectSearchItem[SearchHistory] success");
                    log.debug("DATABASE: saveSelectSearchItem[SearchHistory] success");
                } else {
                    Log.e("DATABASE", "saveSelectSearchItem[SearchHistory] error");
                    log.error("DATABASE: saveSelectSearchItem[SearchHistory] error");
                }

                mSearchlinearLayout.setVisibility(View.INVISIBLE);
                searchItem.collapseActionView();
            }
        });
    }

    //设置 search history list 点击监听
    private void setSearchHistoryClickListener() {
        mSearchHistoryList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String searchDescription = ((TextView) view.findViewById(R.id.search_description)).getText().toString();
                String searchKey = ((TextView) view.findViewById(R.id.search_key)).getText().toString();
                String searchIsLoc = ((TextView) view.findViewById(R.id.search_isLoc)).getText().toString();

                //如果是定位搜索
                if (searchIsLoc.equals("1")) {
                    String lng = ((TextView) view.findViewById(R.id.search_longitude)).getText().toString();
                    String lat = ((TextView) view.findViewById(R.id.search_latitude)).getText().toString();
                    // DisplayToast("lng is " + lng + "lat is " + lat);
                    curMapLatLng = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));
                    MapStatusUpdate mapstatusupdate = MapStatusUpdateFactory.newLatLng(curMapLatLng);
                    //对地图的中心点进行更新
                    mBaiduMap.setMapStatus(mapstatusupdate);

                    markSelectedPosition();

                    transformCoordinate(lng, lat);

                    //设置列表不可见
                    mHistorylinearLayout.setVisibility(View.INVISIBLE);
                    searchItem.collapseActionView();
                    //更新表
                    ContentValues contentValues = new ContentValues();
                    contentValues.put("SearchKey", searchKey);
                    contentValues.put("Description", searchDescription);
                    contentValues.put("IsLocate", 1);
                    contentValues.put("BD09Longitude", lng);
                    contentValues.put("BD09Latitude", lat);
                    String[] wgsLatLngStr = curLatLng.split("&");
                    contentValues.put("WGS84Longitude", wgsLatLngStr[0]);
                    contentValues.put("WGS84Latitude", wgsLatLngStr[1]);
                    contentValues.put("TimeStamp", System.currentTimeMillis() / 1000);

                    if (saveSelectSearchItem(searchHistoryDB, contentValues)) {
                        Log.d("DATABASE", "saveSelectSearchItem[SearchHistory] success");
                        log.debug("DATABASE: saveSelectSearchItem[SearchHistory] success");
                    } else {
                        Log.e("DATABASE", "saveSelectSearchItem[SearchHistory] error");
                        log.error("DATABASE: saveSelectSearchItem[SearchHistory] error");
                    }
                }
                //如果仅仅是搜索
                else if (searchIsLoc.equals("0")) {
                    try {
                        // resetMap();
                        isSubmit = true;
                        mSuggestionSearch.requestSuggestion((new SuggestionSearchOption())
                                .keyword(searchKey)
                                .city(mCurrentCity)
                        );
                        mBaiduMap.clear();
                        mHistorylinearLayout.setVisibility(View.INVISIBLE);
                        searchItem.collapseActionView();
                        //更新表
                        //搜索历史 插表参数
                        ContentValues contentValues = new ContentValues();
                        contentValues.put("SearchKey", searchKey);
                        contentValues.put("Description", "搜索...");
                        contentValues.put("IsLocate", 0);
                        contentValues.put("TimeStamp", System.currentTimeMillis() / 1000);

                        if (saveSelectSearchItem(searchHistoryDB, contentValues)) {
                            Log.d("DATABASE", "saveSelectSearchItem[SearchHistory] success");
                            log.debug("DATABASE: saveSelectSearchItem[SearchHistory] success");
                        } else {
                            Log.e("DATABASE", "saveSelectSearchItem[SearchHistory] error");
                            log.error("DATABASE: saveSelectSearchItem[SearchHistory] error");
                        }
                    } catch (Exception e) {
                        DisplayToast("搜索失败，请检查网络连接");
                        Log.d("HTTP", "搜索失败，请检查网络连接");
                        log.debug("搜索失败，请检查网络连接");
                        e.printStackTrace();
                    }
                } else {    //其他情况
                    Log.d("HTTP", "illegal parameter");
                    log.debug("搜索失败，参数非法");
                }
            }
        });
        mSearchHistoryList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, final View view, int position, long id) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Warning")//这里是表头的内容
                        .setMessage("确定要删除该项搜索记录吗?")//这里是中间显示的具体信息
                        .setPositiveButton("确定",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        String searchKey = ((TextView) view.findViewById(R.id.search_key)).getText().toString();

                                        try {
                                            searchHistoryDB.delete(HistorySearchDataBaseHelper.TABLE_NAME, "SearchKey = ?", new String[] {searchKey});
                                            //删除成功
                                            //展示搜索历史
                                            List<Map<String, Object>> data = getSearchHistory();

                                            if (data.size() > 0) {
                                                simAdapt = new SimpleAdapter(
                                                        MainActivity.this,
                                                        data,
                                                        layout.search_record_item,
                                                        new String[] {"search_key", "search_description", "search_timestamp", "search_isLoc", "search_longitude", "search_latitude"}, // 与下面数组元素要一一对应
                                                        new int[] {R.id.search_key, R.id.search_description, R.id.search_timestamp, R.id.search_isLoc, R.id.search_longitude, R.id.search_latitude});
                                                mSearchHistoryList.setAdapter(simAdapt);
                                                mHistorylinearLayout.setVisibility(View.VISIBLE);
                                            }
                                        } catch (Exception e) {
                                            Log.e("DATABASE", "delete error");
                                            log.error("DATABASE: delete error");
                                            DisplayToast("DELETE ERROR[UNKNOWN]");
                                            e.printStackTrace();
                                        }
                                    }
                                })
                        .setNegativeButton("取消",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                })
                        .show();
                return true;
            }
        });
    }

    //检索建议
    private void setSearchSuggestListener() {
        mSuggestionSearch = SuggestionSearch.newInstance();
        OnGetSuggestionResultListener listener = new OnGetSuggestionResultListener() {
            public void onGetSuggestionResult(SuggestionResult res) {
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
                        mSearchlinearLayout.setVisibility(View.INVISIBLE);
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

                        simAdapt = new SimpleAdapter(
                                MainActivity.this,
                                data,
                                layout.poi_search_item,
                                new String[] {"key_name", "key_addr", "key_lng", "key_lat"}, // 与下面数组元素要一一对应
                                new int[] {id.poi_name, id.poi_addr, id.poi_longitude, id.poi_latitude});
                        mSearchList.setAdapter(simAdapt);
                        // mSearchList.setVisibility(View.VISIBLE);
                        mSearchlinearLayout.setVisibility(View.VISIBLE);
                    }
                }
            }
        };
        mSuggestionSearch.setOnGetSuggestionResultListener(listener);
    }

    //定位SDK监听函数
    public class LocationGoListener extends BDAbstractLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {
            // map view 销毁后不在处理新接收的位置
            if (location == null || mMapView == null) {
                return;
            }

            mCurrentCity = location.getCity();
            mCurrentLat = location.getLatitude();
            mCurrentLon = location.getLongitude();
            mCurrentAccracy = location.getRadius();
            locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(mCurrentDirection).latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();
            mBaiduMap.setMyLocationData(locData);

            if (isFirstLoc) {
                isFirstLoc = false;
                // 这里记录百度地图返回的位置
                curMapLatLng = new LatLng(location.getLatitude(),
                        location.getLongitude());
                MapStatus.Builder builder = new MapStatus.Builder();
                builder.target(curMapLatLng).zoom(18.0f);
                mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));

                Log.d("DEBUG", "First Baidu LatLng: " + curMapLatLng);
                log.debug("First Baidu LatLng: " + curMapLatLng);

                // 这里将百度地图位置转换为 GPS 坐标。实际使用GPS 返回的坐标会更好点
                double[] latLng = MapUtils.bd2wgs(curMapLatLng.longitude, curMapLatLng.latitude);
                curLatLng = latLng[0] + "&" + latLng[1];
                Log.d("DEBUG", "First LatLng: " + curLatLng);
                log.debug("First LatLng: " + curLatLng);
            }
        }
    }

    public class GoServiceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int StatusRun;

            Bundle bundle = intent.getExtras();
            assert bundle != null;

            StatusRun = bundle.getInt("StatusRun");

            if (StatusRun == RunCode) {
                isServiceRun = true;
            } else if (StatusRun == StopCode) {
                isServiceRun = false;
            }
        }
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
