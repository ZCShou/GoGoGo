package com.zcshou.gogogo;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.preference.PreferenceManager;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.baidu.mapapi.search.sug.SuggestionSearch;
import com.baidu.mapapi.search.sug.SuggestionSearchOption;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zcshou.service.ServiceGo;
import com.zcshou.database.DataBaseHistoryLocation;
import com.zcshou.database.DataBaseHistorySearch;
import com.zcshou.utils.ShareUtils;
import com.zcshou.utils.GoUtils;
import com.zcshou.utils.MapUtils;

import static android.view.View.GONE;

import com.elvishew.xlog.XLog;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MainActivity extends BaseActivity implements SensorEventListener {
    /* ?????? */
    public static final String LAT_MSG_ID = "LAT_VALUE";
    public static final String LNG_MSG_ID = "LNG_VALUE";

    public static final String POI_NAME = "POI_NAME";
    public static final String POI_ADDRESS = "POI_ADDRESS";
    public static final String POI_LONGITUDE = "POI_LONGITUDE";
    public static final String POI_LATITUDE = "POI_LATITUDE";

    // ??????????????????
    public final static BitmapDescriptor mMapIndicator = BitmapDescriptorFactory.fromResource(R.drawable.icon_gcoding);
    public static String mCurrentCity = null;
    private static LatLng mMarkLatLngMap = new LatLng(36.547743718042415, 117.07018449827267); // ????????????????????????
    private double mCurrentLat = 0.0;       // ???????????????????????????
    private double mCurrentLon = 0.0;       // ???????????????????????????
    private static double mCurLat = ServiceGo.DEFAULT_LAT;  /* WGS84 ?????????????????? */
    private static double mCurLng = ServiceGo.DEFAULT_LNG;  /* WGS84 ?????????????????? */
    private static BaiduMap mBaiduMap = null;
    private MapView mMapView;
    private LocationClient mLocClient = null;
    private float mCurrentDirection = 0.0f;
    private boolean isFirstLoc = true; // ??????????????????
    private SensorManager mSensorManager;
    private Sensor mSensorAccelerometer;
    private Sensor mSensorMagnetic;
    //????????????????????????
    private float[] mAccValues = new float[3];
    //?????????????????????
    private float[] mMagValues = new float[3];
    //??????????????????????????????????????????????????????
    private final float[] mR = new float[9];
    //?????????????????????????????????????????????????????????
    private final float[] mDirectionValues = new float[3];
    private GeoCoder mGeoCoder;

    // ?????????????????????
    private SQLiteDatabase mLocationHistoryDB;
    private SQLiteDatabase mSearchHistoryDB;

    private OkHttpClient mOkHttpClient;

    // UI??????
    private NavigationView mNavigationView;
    private CheckBox mPtlCheckBox;
    private final JSONObject mReg = new JSONObject();
    private FloatingActionButton mButtonStart;
    private SearchView searchView;
    private ListView mSearchList;
    private ListView mSearchHistoryList;
    private LinearLayout mSearchLayout;
    private LinearLayout mHistoryLayout;
    private MenuItem searchItem;
    private SuggestionSearch mSuggestionSearch;

    private boolean isMockServStart = false;
    private boolean isMove = false;
    private ServiceGo.ServiceGoBinder mServiceBinder;
    private ServiceConnection mConnection;
    private SharedPreferences sharedPreferences;


    private DownloadManager mDownloadManager = null;
    private long mDownloadId;
    private BroadcastReceiver mDownloadBdRcv;
    private String mUpdateFilename;

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

        XLog.i("MainActivity: onCreate");

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        mOkHttpClient = new OkHttpClient();

        //sqlite??????
        initStoreHistory();

        initBaiduMap();

        initBaiduLocation();

        // ????????????????????????
        initListenerMapBtn();

        initNavigationView();

        initSearchView();

        setGoBtnListener();

        mConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mServiceBinder = (ServiceGo.ServiceGoBinder)service;
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };

        initUpdateVersion();

        checkUpdateVersion(false);
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
        //???????????????????????????
        mSensorManager.unregisterListener(this);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        XLog.i("MainActivity: onDestroy");

        if (isMockServStart) {
            unbindService(mConnection); // ???????????????????????????????????????????????????????????????
            Intent serviceGoIntent = new Intent(MainActivity.this, ServiceGo.class);
            stopService(serviceGoIntent);
        }
        unregisterReceiver(mDownloadBdRcv);

        mSensorManager.unregisterListener(this);

        // ?????????????????????
        mLocClient.stop();
        // ??????????????????
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
        //??????searchView
        searchItem = menu.findItem(R.id.action_search);
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
                //??????????????????
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

        searchView = (SearchView) searchItem.getActionView();
        searchView.setIconified(false);// ??????searchView??????????????????
        searchView.onActionViewExpanded();// ?????????????????????????????????????????????????????????
        searchView.setIconifiedByDefault(true);//?????????true??????????????????false????????????
        searchView.setSubmitButtonEnabled(false);//??????????????????
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                try {
                    mSuggestionSearch.requestSuggestion((new SuggestionSearchOption())
                            .keyword(query)
                            .city(mCurrentCity)
                    );
                    //???????????? ????????????
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(DataBaseHistorySearch.DB_COLUMN_KEY, query);
                    contentValues.put(DataBaseHistorySearch.DB_COLUMN_DESCRIPTION, "???????????????");
                    contentValues.put(DataBaseHistorySearch.DB_COLUMN_IS_LOCATION, DataBaseHistorySearch.DB_SEARCH_TYPE_KEY);
                    contentValues.put(DataBaseHistorySearch.DB_COLUMN_TIMESTAMP, System.currentTimeMillis() / 1000);

                    DataBaseHistorySearch.saveHistorySearch(mSearchHistoryDB, contentValues);
                    mBaiduMap.clear();
                    mSearchLayout.setVisibility(View.INVISIBLE);
                } catch (Exception e) {
                    GoUtils.DisplayToast(MainActivity.this,"????????????????????????????????????");
                    XLog.d("HTTP: ????????????????????????????????????");
                    e.printStackTrace();
                }

                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                //???????????????????????????????????????
                //???????????????????????????
                mHistoryLayout.setVisibility(View.INVISIBLE);

                if (newText != null && newText.length() > 0) {
                    try {
                        mSuggestionSearch.requestSuggestion((new SuggestionSearchOption())
                                .keyword(newText)
                                .city(mCurrentCity)
                        );
                    } catch (Exception e) {
                        GoUtils.DisplayToast(MainActivity.this,"????????????????????????????????????");
                        XLog.d("HTTP: ????????????????????????????????????");
                        e.printStackTrace();
                    }
                }

                return true;
            }
        });

        // ????????????????????????(?????????????????????????????????)
        ImageView closeButton = searchView.findViewById(androidx.appcompat.R.id.search_close_btn);
        closeButton.setOnClickListener(v -> {
            EditText et = findViewById(androidx.appcompat.R.id.search_src_text);
            et.setText("");
            searchView.setQuery("", false);
            mSearchLayout.setVisibility(View.INVISIBLE);
            mHistoryLayout.setVisibility(View.VISIBLE);
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
        mCurrentDirection = (float) Math.toDegrees(mDirectionValues[0]);    // ???????????????
        if (mCurrentDirection < 0) {    // ??? -180 ~ + 180 ?????? 0 ~ 360
            mCurrentDirection += 360;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

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
            } else if (id == R.id.nav_dev) {
                try {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
                    startActivity(intent);
                } catch (Exception e) {
                    GoUtils.DisplayToast(this,"??????????????????????????????,????????????????????????????????????????????????");
                    e.printStackTrace();
                }
            } else if (id == R.id.nav_update) {
                checkUpdateVersion(true);
            } else if (id == R.id.nav_feedback) {
                File file = new File(getExternalFilesDir("Logs"), GoApplication.LOG_FILE_NAME);
                ShareUtils.shareFile(this, file, item.getTitle().toString());
            } else if (id == R.id.nav_contact) {
                Uri uri = Uri.parse("https://gitee.com/zcshou/gogogo/issues");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
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
//        TextView mUserLimitInfo = navHeaderView.findViewById(R.id.user_limit);
        ImageView mUserIcon = navHeaderView.findViewById(R.id.user_icon);

        if (sharedPreferences.getString("setting_reg_code", null) != null) {
            mUserName.setText(getResources().getString(R.string.app_author));
        } else {
            mUserIcon.setOnClickListener(v -> {
                DrawerLayout drawer = findViewById(R.id.drawer_layout);

                if (drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.closeDrawer(GravityCompat.START);
                }
                Uri uri = Uri.parse("https://gitee.com/zcshou/gogogo");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
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

    private void initSearchView() {
        mSearchList = findViewById(R.id.search_list_view);
        mSearchLayout = findViewById(R.id.search_linear);
        mSearchHistoryList = findViewById(R.id.search_history_list_view);
        mHistoryLayout = findViewById(R.id.search_history_linear);

        //?????????????????????????????????
        setSearchResultClickListener();

        //?????????????????????????????????
        setSearchHistoryClickListener();

        //?????????????????????????????????
        setSearchSuggestListener();
    }

    //?????? search list ????????????
    private void setSearchResultClickListener() {
        mSearchList.setOnItemClickListener((parent, view, position, id) -> {
            String lng = ((TextView) view.findViewById(R.id.poi_longitude)).getText().toString();
            String lat = ((TextView) view.findViewById(R.id.poi_latitude)).getText().toString();
            mMarkLatLngMap = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));
            MapStatusUpdate mapstatusupdate = MapStatusUpdateFactory.newLatLng(mMarkLatLngMap);
            //????????????????????????????????????
            mBaiduMap.setMapStatus(mapstatusupdate);

            markMap();

            transformCoordinate(lng, lat);

            // mSearchList.setVisibility(View.GONE);
            //???????????? ????????????
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

    //?????? search history list ????????????
    private void setSearchHistoryClickListener() {
        mSearchHistoryList.setOnItemClickListener((parent, view, position, id) -> {
            String searchDescription = ((TextView) view.findViewById(R.id.search_description)).getText().toString();
            String searchKey = ((TextView) view.findViewById(R.id.search_key)).getText().toString();
            String searchIsLoc = ((TextView) view.findViewById(R.id.search_isLoc)).getText().toString();

            //?????????????????????
            if (searchIsLoc.equals("1")) {
                String lng = ((TextView) view.findViewById(R.id.search_longitude)).getText().toString();
                String lat = ((TextView) view.findViewById(R.id.search_latitude)).getText().toString();
                //?????????????????????????????????
                mMarkLatLngMap = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));
                MapStatusUpdate mapstatusupdate = MapStatusUpdateFactory.newLatLng(mMarkLatLngMap);
                mBaiduMap.setMapStatus(mapstatusupdate);

                markMap();

                transformCoordinate(lng, lat);

                //?????????????????????
                mHistoryLayout.setVisibility(View.INVISIBLE);
                searchItem.collapseActionView();
                //?????????
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
            } else if (searchIsLoc.equals("0")) { //?????????????????????
                try {
                    searchView.setQuery(searchKey, true);
                } catch (Exception e) {
                    GoUtils.DisplayToast(this,"????????????????????????????????????");
                    XLog.e("ERROR: ????????????????????????????????????");
                    e.printStackTrace();
                }
            } else {
                XLog.e("ERROR:???????????????????????????");
            }
        });
        mSearchHistoryList.setOnItemLongClickListener((parent, view, position, id) -> {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("??????")//????????????????????????
                    .setMessage("?????????????????????????????????????")//????????????????????????????????????
                    .setPositiveButton("??????",(dialog, which) -> {
                        String searchKey = ((TextView) view.findViewById(R.id.search_key)).getText().toString();

                        try {
                            mSearchHistoryDB.delete(DataBaseHistorySearch.TABLE_NAME, DataBaseHistorySearch.DB_COLUMN_KEY + " = ?", new String[] {searchKey});
                            //????????????
                            //??????????????????
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
                                                DataBaseHistorySearch.DB_COLUMN_LATITUDE_CUSTOM}, // ????????????????????????????????????
                                        new int[] {R.id.search_key, R.id.search_description, R.id.search_timestamp, R.id.search_isLoc, R.id.search_longitude, R.id.search_latitude});
                                mSearchHistoryList.setAdapter(simAdapt);
                                mHistoryLayout.setVisibility(View.VISIBLE);
                            }
                        } catch (Exception e) {
                            XLog.e("ERROR: delete database error");
                            GoUtils.DisplayToast(MainActivity.this,"??????????????????");
                            e.printStackTrace();
                        }
                    })
                    .setNegativeButton("??????",
                            (dialog, which) -> {
                            })
                    .show();
            return true;
        });
    }

    //????????????
    private void setSearchSuggestListener() {
        mSuggestionSearch = SuggestionSearch.newInstance();
        mSuggestionSearch.setOnGetSuggestionResultListener(suggestionResult -> {
            if (suggestionResult == null || suggestionResult.getAllSuggestions() == null) {
                GoUtils.DisplayToast(this,"????????????????????????");
            } else { //??????????????????????????????
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
                        new String[] {POI_NAME, POI_ADDRESS, POI_LONGITUDE, POI_LATITUDE}, // ????????????????????????????????????
                        new int[] {R.id.poi_name, R.id.poi_address, R.id.poi_longitude, R.id.poi_latitude});
                mSearchList.setAdapter(simAdapt);
                // mSearchList.setVisibility(View.VISIBLE);
                mSearchLayout.setVisibility(View.VISIBLE);
            }
        });
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
                    GoUtils.DisplayToast(this,"???????????????????????????????????????");
                    return;
                }
                if (TextUtils.isEmpty(regUserName.getText())) {
                    GoUtils.DisplayToast(this, "?????????????????????");
                    return;
                }
                if (TextUtils.isEmpty(regResp.getText())) {
                    GoUtils.DisplayToast(this,"?????????????????????");
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
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);      // ??????????????????
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


    private void initBaiduMap() {
        // ???????????????
        mMapView = findViewById(R.id.bdMapView);
        mMapView.showZoomControls(false);
        mBaiduMap = mMapView.getMap();
        mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
        mBaiduMap.setMyLocationEnabled(true);

        View poiView = View.inflate(MainActivity.this, R.layout.poi_info, null);
        TextView poiAddress = poiView.findViewById(R.id.poi_address);
        TextView poiLongitude = poiView.findViewById(R.id.poi_longitude);
        TextView poiLatitude = poiView.findViewById(R.id.poi_latitude);
        ImageButton ibSave = poiView.findViewById(R.id.poi_save);
        ibSave.setOnClickListener(v -> {
            recordGetPositionInfo();
            GoUtils.DisplayToast(this, "???????????????");
        });
        ImageButton ibCopy = poiView.findViewById(R.id.poi_copy);
        ibCopy.setOnClickListener(v -> {
            //???????????????????????????
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            // ?????????????????????ClipData
            ClipData mClipData = ClipData.newPlainText("Label", mMarkLatLngMap.toString());
            // ??? ClipData?????????????????????????????????
            cm.setPrimaryClip(mClipData);

            GoUtils.DisplayToast(this, "?????????????????????");
        });
        ImageButton ibShare = poiView.findViewById(R.id.poi_share);
        ibShare.setOnClickListener(v -> ShareUtils.shareText(MainActivity.this, "????????????", poiLongitude.getText()+","+poiLatitude.getText()));
        ImageButton ibFly = poiView.findViewById(R.id.poi_fly);
        ibFly.setOnClickListener(this::startGoLocation);

        mGeoCoder = GeoCoder.newInstance();
        mGeoCoder.setOnGetGeoCodeResultListener(new OnGetGeoCoderResultListener() {
            @Override
            public void onGetGeoCodeResult(GeoCodeResult geoCodeResult) {
                XLog.i(geoCodeResult.getLocation());
            }

            @Override
            public void onGetReverseGeoCodeResult(ReverseGeoCodeResult reverseGeoCodeResult) {
                if (reverseGeoCodeResult == null || reverseGeoCodeResult.error != SearchResult.ERRORNO.NO_ERROR) {
                    XLog.i("?????????????????????!");
                } else {
                    poiLatitude.setText(String.valueOf(reverseGeoCodeResult.getLocation().latitude));
                    poiLongitude.setText(String.valueOf(reverseGeoCodeResult.getLocation().longitude));
                    poiAddress.setText(reverseGeoCodeResult.getAddress());
                    final InfoWindow mInfoWindow = new InfoWindow(poiView, reverseGeoCodeResult.getLocation(), -100);
                    mBaiduMap.showInfoWindow(mInfoWindow);
                }
            }
        });


        mBaiduMap.setOnMapTouchListener(event -> {

        });

        mBaiduMap.setOnMapClickListener(new BaiduMap.OnMapClickListener() {
            /**
             * ????????????
             */
            @Override
            public void onMapClick(LatLng point) {
                mMarkLatLngMap = point;
                markMap();

                //??????????????????wgs?????????
                transformCoordinate(String.valueOf(point.longitude), String.valueOf(point.latitude));
            }
            /**
             * ??????????????????POI???
             */
            @Override
            public void onMapPoiClick(MapPoi poi) {
                mMarkLatLngMap = poi.getPosition();
                markMap();
                //??????????????????wgs?????????
                transformCoordinate(String.valueOf(poi.getPosition().longitude), String.valueOf(poi.getPosition().latitude));
            }
        });

        mBaiduMap.setOnMapLongClickListener(new BaiduMap.OnMapLongClickListener() {
            /**
             * ????????????
             */
            @Override
            public void onMapLongClick(LatLng point) {
                mMarkLatLngMap = point;
                markMap();
                mGeoCoder.reverseGeoCode(new ReverseGeoCodeOption().location(point));
                //??????????????????wgs?????????
                transformCoordinate(String.valueOf(point.longitude), String.valueOf(point.latitude));
            }
        });

        mBaiduMap.setOnMapDoubleClickListener(new BaiduMap.OnMapDoubleClickListener() {
            /**
             * ????????????
             */
            @Override
            public void onMapDoubleClick(LatLng point) {
                mBaiduMap.animateMapStatus(MapStatusUpdateFactory.zoomIn());
            }
        });

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);// ???????????????????????????
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

    //???????????????????????????
    private void initBaiduLocation() {
        try {
            // ???????????????
            mLocClient = new LocationClient(this);
            mLocClient.registerLocationListener(new BDAbstractLocationListener() {
                @Override
                public void onReceiveLocation(BDLocation bdLocation) {
                    if (bdLocation == null || mMapView == null) {// mapview ???????????????????????????????????????
                        return;
                    }

                    mCurrentCity = bdLocation.getCity();
                    mCurrentLat = bdLocation.getLatitude();
                    mCurrentLon = bdLocation.getLongitude();
                    MyLocationData locData = new MyLocationData.Builder()
                            .accuracy(bdLocation.getRadius())
                            .direction(mCurrentDirection)// ?????????????????????????????????????????????????????????0-360
                            .latitude(bdLocation.getLatitude())
                            .longitude(bdLocation.getLongitude()).build();
                    mBaiduMap.setMyLocationData(locData);
                    MyLocationConfiguration configuration = new MyLocationConfiguration(MyLocationConfiguration.LocationMode.NORMAL, true, null);
                    mBaiduMap.setMyLocationConfiguration(configuration);

                    if (isMove) {
                        isMove = false;
                        mBaiduMap.clear();
                        mMarkLatLngMap = null;

                        if (GoUtils.isWifiEnabled(MainActivity.this)) {
                            GoUtils.showDisableWifiDialog(MainActivity.this);
                        }
                    }

                    /* ???????????????????????????????????????????????? */
                    int err = bdLocation.getLocType();
                    if (err == BDLocation.TypeCriteriaException || err == BDLocation.TypeNetWorkException) {
                        mLocClient.requestLocation();   /* ???????????? */
                    } else {
                        if (isFirstLoc) {
                            isFirstLoc = false;
                            // ???????????????????????????????????????
                            mMarkLatLngMap = new LatLng(bdLocation.getLatitude(), bdLocation.getLongitude());
                            MapStatus.Builder builder = new MapStatus.Builder();
                            builder.target(mMarkLatLngMap).zoom(18.0f);
                            mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));

                            XLog.i("First Baidu LatLng: " + mMarkLatLngMap);

                            // ???????????????????????????????????? GPS ??????
                            double[] latLng = MapUtils.bd2wgs(mMarkLatLngMap.longitude, mMarkLatLngMap.latitude);
                            mCurLng = latLng[0];
                            mCurLat = latLng[1];
                        }
                    }
                }
                /**
                 * ??????????????????
                 * <a>http://lbsyun.baidu.com/index.php?title=android-locsdk/guide/addition-func/error-code</a>
                 * <p>
                 * ?????????????????????????????????????????????????????????????????????????????????????????????
                 *
                 * @param locType      ??????????????????
                 * @param diagnosticType  ???????????????1~9???
                 * @param diagnosticMessage ???????????????????????????
                 */
                @Override
                public void onLocDiagnosticMessage(int locType, int diagnosticType, String diagnosticMessage) {
                    XLog.i("Baidu ERROR: " + locType + "-" + diagnosticType + "-" + diagnosticMessage);
                }
            });
            LocationClientOption locationOption = new LocationClientOption();
            //?????????????????????????????????????????????????????????????????????????????????
            locationOption.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
            //???????????????gcj02??????????????????????????????????????????????????????????????????????????????????????????bd09ll;
            locationOption.setCoorType("bd09ll");
            //???????????????0?????????????????????????????????????????????????????????????????????????????????1000ms???????????????
            locationOption.setScanSpan(1000);
            //?????????????????????????????????????????????????????????
            locationOption.setIsNeedAddress(true);
            //?????????????????????????????????????????????
            locationOption.setNeedDeviceDirect(false);
            //???????????????false??????????????????gps???????????????1S1???????????????GPS??????
            locationOption.setLocationNotify(true);
            //???????????????true?????????SDK???????????????SERVICE?????????????????????????????????????????????stop?????????????????????????????????????????????
            locationOption.setIgnoreKillProcess(true);
            //???????????????false??????????????????????????????????????????????????????BDLocation.getLocationDescribe?????????????????????????????????????????????????????????
            locationOption.setIsNeedLocationDescribe(false);
            //???????????????false?????????????????????POI??????????????????BDLocation.getPoiList?????????
            locationOption.setIsNeedLocationPoiList(false);
            //???????????????false?????????????????????CRASH?????????????????????
            locationOption.SetIgnoreCacheException(true);
            //???????????????false?????????????????????Gps??????
            locationOption.setOpenGps(true);
            //???????????????false?????????????????????????????????????????????????????????????????????????????????????????????
            locationOption.setIsNeedAltitude(false);
            //??????????????????LocationClientOption???????????????setLocOption???????????????LocationClient????????????
            mLocClient.setLocOption(locationOption);
            //????????????
            mLocClient.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //???????????????????????????
    private void initListenerMapBtn() {
        RadioGroup mGroupMapType = this.findViewById(R.id.RadioGroupMapType);
        mGroupMapType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.mapNormal) {
                mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
            }

            if (checkedId == R.id.mapSatellite) {
                mBaiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
            }
        });

        ImageButton curPosBtn = this.findViewById(R.id.cur_position);
        curPosBtn.setOnClickListener(v -> resetMap());

        ImageButton zoomInBtn = this.findViewById(R.id.zoom_in);
        zoomInBtn.setOnClickListener(v -> mBaiduMap.animateMapStatus(MapStatusUpdateFactory.zoomIn()));

        ImageButton zoomOutBtn = this.findViewById(R.id.zoom_out);
        zoomOutBtn.setOnClickListener(v -> mBaiduMap.animateMapStatus(MapStatusUpdateFactory.zoomOut()));

        ImageButton inputPosBtn = this.findViewById(R.id.input_pos);
        inputPosBtn.setOnClickListener(v -> {
            AlertDialog dialog;
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("????????????????????????");
            View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.input_position, null);
            builder.setView(view);
            dialog = builder.show();

            EditText dialog_lng = view.findViewById(R.id.joystick_longitude);
            EditText dialog_lat = view.findViewById(R.id.joystick_latitude);
            RadioButton rbBD = view.findViewById(R.id.pos_type_bd);

            Button btnGo = view.findViewById(R.id.input_position_ok);
            btnGo.setOnClickListener(v2 -> {
                String dialog_lng_str = dialog_lng.getText().toString();
                String dialog_lat_str = dialog_lat.getText().toString();

                if (TextUtils.isEmpty(dialog_lng_str) || TextUtils.isEmpty(dialog_lat_str)) {
                    GoUtils.DisplayToast(MainActivity.this,"??????????????????");
                } else {
                    double dialog_lng_double = Double.parseDouble(dialog_lng_str);
                    double dialog_lat_double = Double.parseDouble(dialog_lat_str);

                    if (dialog_lng_double > 180.0 || dialog_lng_double < -180.0 || dialog_lat_double > 90.0 || dialog_lat_double < -90.0) {
                        GoUtils.DisplayToast(MainActivity.this, "?????????????????????!\n-180.0<??????<180.0\n-90.0<??????<90.0");
                    } else {
                        if (rbBD.isChecked()) {
                            mMarkLatLngMap = new LatLng(dialog_lat_double, dialog_lng_double);
                        } else {
                            double[] bdLonLat = MapUtils.wgs2bd09(dialog_lat_double, dialog_lng_double);
                            mMarkLatLngMap = new LatLng(bdLonLat[1], bdLonLat[0]);
                        }

                        markMap();

                        MapStatusUpdate mapstatusupdate = MapStatusUpdateFactory.newLatLng(mMarkLatLngMap);
                        mBaiduMap.setMapStatus(mapstatusupdate);

                        dialog.dismiss();
                    }
                }
            });

            Button btnCancel = view.findViewById(R.id.input_position_cancel);
            btnCancel.setOnClickListener(v1 -> dialog.dismiss());
        });
    }

    //?????????????????????
    private void markMap() {
        if (mMarkLatLngMap != null) {
            MarkerOptions ooA = new MarkerOptions().position(mMarkLatLngMap).icon(mMapIndicator);
            mBaiduMap.clear();
            mBaiduMap.addOverlay(ooA);
        }
    }

    private void resetMap() {
        mBaiduMap.clear();
        mMarkLatLngMap = null;

        MyLocationData locData = new MyLocationData.Builder()
                .latitude(mCurrentLat)
                .longitude(mCurrentLon)
                .direction(mCurrentDirection)
                .build();
        mBaiduMap.setMyLocationData(locData);

        MapStatus.Builder builder = new MapStatus.Builder();
        builder.target(new LatLng(mCurrentLat, mCurrentLon)).zoom(18.0f);
        mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
    }

    //????????????
    private void transformCoordinate(final String longitude, final String latitude) {
        final double error = 0.00000001;
        final String safeCode = getResources().getString(R.string.safecode);
        final String ak = getResources().getString(R.string.ak);
        String mapApiUrl = "https://api.map.baidu.com/geoconv/v1/?coords=" + longitude + "," + latitude +
                "&from=5&to=3&ak=" + ak + "&mcode=" + safeCode;

        okhttp3.Request request = new okhttp3.Request.Builder().url(mapApiUrl).get().build();
        final Call call = mOkHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                XLog.e("ERROR: HTTP GET FAILED");
                //http ???????????? ?????????????????????
                double[] latLng = MapUtils.bd2wgs(Double.parseDouble(longitude), Double.parseDouble(latitude));
                mCurLng = latLng[0];
                mCurLat = latLng[1];
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull okhttp3.Response response) throws IOException {
                ResponseBody responseBody = response.body();
                if (responseBody != null) {
                    String resp = responseBody.string();
                    try {
                        JSONObject getRetJson = new JSONObject(resp);
                        if (Integer.parseInt(getRetJson.getString("status")) == 0) {
                            JSONArray coordinateArr = getRetJson.getJSONArray("result");
                            JSONObject coordinate = coordinateArr.getJSONObject(0);
                            String gcj02Longitude = coordinate.getString("x");
                            String gcj02Latitude = coordinate.getString("y");
                            BigDecimal bigDecimalGcj02Longitude = BigDecimal.valueOf(Double.parseDouble(gcj02Longitude));
                            BigDecimal bigDecimalGcj02Latitude = BigDecimal.valueOf(Double.parseDouble(gcj02Latitude));
                            BigDecimal bigDecimalBd09Longitude = BigDecimal.valueOf(Double.parseDouble(longitude));
                            BigDecimal bigDecimalBd09Latitude = BigDecimal.valueOf(Double.parseDouble(latitude));
                            double gcj02LongitudeDouble = bigDecimalGcj02Longitude.setScale(9, RoundingMode.HALF_UP).doubleValue();
                            double gcj02LatitudeDouble = bigDecimalGcj02Latitude.setScale(9, RoundingMode.HALF_UP).doubleValue();
                            double bd09LongitudeDouble = bigDecimalBd09Longitude.setScale(9, RoundingMode.HALF_UP).doubleValue();
                            double bd09LatitudeDouble = bigDecimalBd09Latitude.setScale(9, RoundingMode.HALF_UP).doubleValue();

                            //??????bd09???gcj02 ??????????????????  ????????????????????????
                            if ((Math.abs(gcj02LongitudeDouble - bd09LongitudeDouble)) <= error && (Math.abs(gcj02LatitudeDouble - bd09LatitudeDouble)) <= error) {
                                mCurLat = Double.parseDouble(latitude);
                                mCurLng = Double.parseDouble(longitude);
                            } else {
                                double[] latLng = MapUtils.gcj02towgs84(Double.parseDouble(gcj02Longitude), Double.parseDouble(gcj02Latitude));
                                mCurLng = latLng[0];
                                mCurLat = latLng[1];
                            }
                        } else {
                            XLog.e("ERROR:http get ");
                            double[] latLng = MapUtils.bd2wgs(Double.parseDouble(longitude), Double.parseDouble(latitude));
                            mCurLng = latLng[0];
                            mCurLat = latLng[1];
                        }
                    } catch (JSONException e) {
                        XLog.e("ERROR: resolve json");
                        e.printStackTrace();
                        double[] latLng = MapUtils.bd2wgs(Double.parseDouble(longitude), Double.parseDouble(latitude));
                        mCurLng = latLng[0];
                        mCurLat = latLng[1];
                    }
                }
            }
        });
    }

    // ????????????????????????
    public static boolean showLocation(String bd09Longitude, String bd09Latitude, String wgs84Longitude, String wgs84Latitude) {
        boolean ret = true;

        try {
            if (!bd09Longitude.isEmpty() && !bd09Latitude.isEmpty()) {
                mMarkLatLngMap = new LatLng(Double.parseDouble(bd09Latitude), Double.parseDouble(bd09Longitude));
                MarkerOptions ooA = new MarkerOptions().position(mMarkLatLngMap).icon(mMapIndicator);
                mBaiduMap.clear();
                mBaiduMap.addOverlay(ooA);
                MapStatusUpdate mapstatusupdate = MapStatusUpdateFactory.newLatLng(mMarkLatLngMap);
                mBaiduMap.setMapStatus(mapstatusupdate);
                mCurLng = Double.parseDouble(wgs84Longitude);
                mCurLat = Double.parseDouble(wgs84Latitude);
            }
        } catch (Exception e) {
            ret = false;
            XLog.e("ERROR: showHistoryLocation");
            e.printStackTrace();
        }

        return ret;
    }

    private void initStoreHistory() {
        try {
            //????????????
            DataBaseHistoryLocation dbLocation = new DataBaseHistoryLocation(getApplicationContext());
            mLocationHistoryDB = dbLocation.getWritableDatabase();
            // ????????????
            DataBaseHistorySearch dbHistory = new DataBaseHistorySearch(getApplicationContext());
            mSearchHistoryDB = dbHistory.getWritableDatabase();
        } catch (Exception e) {
            XLog.e("ERROR: sqlite init error");
            e.printStackTrace();
        }
    }

    //??????????????????
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
            XLog.e("ERROR: query error");
            e.printStackTrace();
        }

        return data;
    }

    // ???????????????????????????
    private void recordGetPositionInfo() {
        //??????????????????bd09
        final String safeCode = getResources().getString(R.string.safecode);
        final String ak = getResources().getString(R.string.ak);
        final String mapType = "bd09ll";
        final double latitude = mMarkLatLngMap.latitude;
        final double longitude = mMarkLatLngMap.longitude;
        //bd09?????????????????????
        String mapApiUrl = "https://api.map.baidu.com/reverse_geocoding/v3/?ak=" + ak + "&output=json&coordtype=" + mapType + "&location=" + latitude + "," + longitude + "&mcode=" + safeCode;

        okhttp3.Request request = new okhttp3.Request.Builder().url(mapApiUrl).get().build();
        final Call call = mOkHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                //http ????????????
                XLog.e("HTTP: HTTP GET FAILED");
                //????????????
                ContentValues contentValues = new ContentValues();
                contentValues.put(DataBaseHistoryLocation.DB_COLUMN_LOCATION, "NULL");
                contentValues.put(DataBaseHistoryLocation.DB_COLUMN_LONGITUDE_WGS84, String.valueOf(mCurLng));
                contentValues.put(DataBaseHistoryLocation.DB_COLUMN_LATITUDE_WGS84, String.valueOf(mCurLat));
                contentValues.put(DataBaseHistoryLocation.DB_COLUMN_TIMESTAMP, System.currentTimeMillis() / 1000);
                contentValues.put(DataBaseHistoryLocation.DB_COLUMN_LONGITUDE_CUSTOM, Double.toString(longitude));
                contentValues.put(DataBaseHistoryLocation.DB_COLUMN_LATITUDE_CUSTOM, Double.toString(latitude));

                DataBaseHistoryLocation.saveHistoryLocation(mLocationHistoryDB, contentValues);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                ResponseBody responseBody = response.body();
                if (responseBody != null) {
                    String resp = responseBody.string();
                    try {
                        JSONObject getRetJson = new JSONObject(resp);

                        //??????????????????
                        if (Integer.parseInt(getRetJson.getString("status")) == 0) {
                            JSONObject posInfoJson = getRetJson.getJSONObject("result");
                            String formatted_address = posInfoJson.getString("formatted_address");
                            //????????????
                            ContentValues contentValues = new ContentValues();
                            contentValues.put(DataBaseHistoryLocation.DB_COLUMN_LOCATION, formatted_address);
                            contentValues.put(DataBaseHistoryLocation.DB_COLUMN_LONGITUDE_WGS84, String.valueOf(mCurLng));
                            contentValues.put(DataBaseHistoryLocation.DB_COLUMN_LATITUDE_WGS84, String.valueOf(mCurLat));
                            contentValues.put(DataBaseHistoryLocation.DB_COLUMN_TIMESTAMP, System.currentTimeMillis() / 1000);
                            contentValues.put(DataBaseHistoryLocation.DB_COLUMN_LONGITUDE_CUSTOM, Double.toString(longitude));
                            contentValues.put(DataBaseHistoryLocation.DB_COLUMN_LATITUDE_CUSTOM, Double.toString(latitude));

                            DataBaseHistoryLocation.saveHistoryLocation(mLocationHistoryDB, contentValues);
                        } else { //??????????????????
                            //????????????
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
                        //????????????
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
                }
            }
        });
    }


    private void doGoLocation() {
        if (!isMockServStart) {
            Intent serviceGoIntent = new Intent(MainActivity.this, ServiceGo.class);
            bindService(serviceGoIntent, mConnection, BIND_AUTO_CREATE);    // ?????????????????????????????????????????????????????????????????????
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
        if (GoUtils.isNetworkAvailable(this)) {
            if (!Settings.canDrawOverlays(getApplicationContext())) {//?????????????????????
                GoUtils.showEnableFloatWindowDialog(this);
                XLog.e("??????????????????!");
            } else {
                if (!GoUtils.isGpsOpened(this)) {
                    GoUtils.showEnableGpsDialog(this);
                } else {
                    if (isMockServStart) {
                        if (mMarkLatLngMap == null) {
                            stopGoLocation();
                            Snackbar.make(v, "?????????????????????", Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();
                            mButtonStart.setImageResource(R.drawable.ic_position);
                        } else {
                            mServiceBinder.setPosition(mCurLng, mCurLat);
                            isMove = true;
                            Snackbar.make(v, "?????????????????????", Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();
                        }
                    } else {
                        if (!GoUtils.isAllowMockLocation(this)) {
                            GoUtils.showEnableMockLocationDialog(this);
                            XLog.e("?????????????????????!");
                        } else {
                            if (mMarkLatLngMap == null) {
                                Snackbar.make(v, "??????????????????????????????????????????", Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();
                            } else {
                                doGoLocation();
                                mButtonStart.setImageResource(R.drawable.ic_fly);
                                Snackbar.make(v, "?????????????????????", Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();
                            }
                        }
                    }
                }
            }
        } else {
            XLog.e("?????????????????????????????????!");
        }
    }

    private void stopGoLocation() {
        if (isMockServStart) {
            unbindService(mConnection); // ???????????????????????????????????????????????????????????????
            Intent serviceGoIntent = new Intent(MainActivity.this, ServiceGo.class);
            stopService(serviceGoIntent);
            isMockServStart = false;
        }
    }

    private void setGoBtnListener() {
        mButtonStart = findViewById(R.id.faBtnStart);
        mButtonStart.setOnClickListener(this::startGoLocation);
    }

    
    private void initUpdateVersion() {
        mDownloadManager =(DownloadManager) MainActivity.this.getSystemService(DOWNLOAD_SERVICE);

        // ????????????????????????????????????????????????
        mDownloadBdRcv = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                installNewVersion();
            }
        };
        registerReceiver(mDownloadBdRcv, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    private void checkUpdateVersion(boolean result) {
        String mapApiUrl = "https://api.github.com/repos/zcshou/gogogo/releases/latest";

        okhttp3.Request request = new okhttp3.Request.Builder().url(mapApiUrl).get().build();
        final Call call = mOkHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                XLog.i("??????????????????");
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull okhttp3.Response response) throws IOException {
                ResponseBody responseBody = response.body();
                if (responseBody != null) {
                    String resp = responseBody.string();
                    // ?????????????????????????????????????????????????????????
                    runOnUiThread(() -> {
                        try {
                            JSONObject getRetJson = new JSONObject(resp);
                            String curVersion = GoUtils.getVersionName(MainActivity.this);

                            if (curVersion != null
                                    && (!getRetJson.getString("name").contains(curVersion)
                                    || !getRetJson.getString("tag_name").contains(curVersion))) {
                                final android.app.AlertDialog alertDialog = new android.app.AlertDialog.Builder(MainActivity.this).create();
                                alertDialog.show();
                                alertDialog.setCancelable(false);
                                Window window = alertDialog.getWindow();
                                if (window != null) {
                                    window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);      // ??????????????????
                                    window.setContentView(R.layout.update);
                                    window.setGravity(Gravity.CENTER);
                                    window.setWindowAnimations(R.style.DialogAnimFadeInFadeOut);

                                    TextView updateTitle = window.findViewById(R.id.update_title);
                                    updateTitle.setText(getRetJson.getString("name"));
                                    TextView updateTime = window.findViewById(R.id.update_time);
                                    updateTime.setText(getRetJson.getString("created_at"));
                                    TextView updateCommit = window.findViewById(R.id.update_commit);
                                    updateCommit.setText(getRetJson.getString("target_commitish"));

                                    TextView updateContent = window.findViewById(R.id.update_content);
                                    SpannableStringBuilder ssb = new SpannableStringBuilder();
                                    ssb.append(getRetJson.getString("body"));
                                    updateContent.setMovementMethod(LinkMovementMethod.getInstance());
                                    updateContent.setText(ssb, TextView.BufferType.SPANNABLE);

                                    Button updateCancel = window.findViewById(R.id.update_ignore);
                                    updateCancel.setOnClickListener(v -> alertDialog.cancel());

                                    /* ?????????????????????????????? */
                                    JSONArray jsonArray = new JSONArray(getRetJson.getString("assets"));
                                    JSONObject jsonObject = jsonArray.getJSONObject(0);
                                    String download_url = jsonObject.getString("browser_download_url");
                                    mUpdateFilename = jsonObject.getString("name");

                                    Button updateAgree = window.findViewById(R.id.update_agree);
                                    updateAgree.setOnClickListener(v -> {
                                        alertDialog.cancel();
                                        GoUtils.DisplayToast(MainActivity.this,"?????????????????????");
                                        downloadNewVersion(download_url);
                                    });
                                }
                            } else {
                                if (result) {
                                    GoUtils.DisplayToast(MainActivity.this,"???????????????????????????????????????????????????");
                                }
                            }
                        } catch (JSONException e) {
                            XLog.e("ERROR:  resolve json");
                            e.printStackTrace();
                        }
                    });
                }
            }
        });
    }

    private void downloadNewVersion(String url) {
        if (mDownloadManager == null) {
            return;
        }

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setAllowedOverRoaming(false);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setTitle(GoUtils.getAppName(this));
        request.setDescription("?????????????????????...");
        request.setMimeType("application/vnd.android.package-archive");

        // DownloadManager???????????????????????????????????????????????????????????????????????????
        File file = new File(getExternalFilesDir("Updates"), mUpdateFilename);
        if (file.exists()) {
            if(!file.delete()) {
                return;
            }
        }
        request.setDestinationUri(Uri.fromFile(file));

        mDownloadId = mDownloadManager.enqueue(request);
    }

    private void installNewVersion() {
        Intent install = new Intent(Intent.ACTION_VIEW);
        Uri downloadFileUri = mDownloadManager.getUriForDownloadedFile(mDownloadId);
        File file = new File(getExternalFilesDir("Updates"), mUpdateFilename);
        if (downloadFileUri != null) {
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            // ???Broadcast???????????????????????????Intent.FLAG_ACTIVITY_NEW_TASK
            install.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);    //???????????????????????????????????????????????????Uri??????????????????
            install.addCategory("android.intent.category.DEFAULT");
            install.setDataAndType(ShareUtils.getUriFromFile(MainActivity.this, file), "application/vnd.android.package-archive");
            startActivity(install);
        } else {
            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName()));
            intent.addCategory("android.intent.category.DEFAULT");
            startActivity(intent);
        }
    }
}
