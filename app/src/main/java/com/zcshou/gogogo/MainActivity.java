package com.zcshou.gogogo;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.DashPathEffect;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.preference.PreferenceManager;

import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.infowindow.InfoWindow;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zcshou.service.ServiceGo;
import com.zcshou.database.DataBaseHistoryLocation;
import com.zcshou.database.DataBaseHistorySearch;
import com.zcshou.script.ScriptParser;
import com.zcshou.script.ScriptRoute;
import com.zcshou.script.ScriptStore;
import com.zcshou.script.ScriptWaypoint;
import com.zcshou.utils.ShareUtils;
import com.zcshou.utils.GoUtils;
import com.zcshou.utils.MapUtils;
import com.zcshou.utils.OsmGeocoder;
import com.zcshou.utils.TileSourceUtils;

import com.elvishew.xlog.XLog;

import okhttp3.OkHttpClient;

public class MainActivity extends BaseActivity implements SensorEventListener {
    /* 对外 */
    public static final String LAT_MSG_ID = "LAT_VALUE";
    public static final String LNG_MSG_ID = "LNG_VALUE";
    public static final String ALT_MSG_ID = "ALT_VALUE";

    public static final String POI_NAME = "POI_NAME";
    public static final String POI_ADDRESS = "POI_ADDRESS";
    public static final String POI_LONGITUDE = "POI_LONGITUDE";
    public static final String POI_LATITUDE = "POI_LATITUDE";

    /** 启动脚本模式时传给 ServiceGo 的脚本 id */
    public static final String SCRIPT_ROUTE_ID = "SCRIPT_ROUTE_ID";

    private OkHttpClient mOkHttpClient;
    private SharedPreferences sharedPreferences;

    /*============================== 主界面地图 相关 ==============================*/
    /************** 地图 *****************/
    /**
     * 选点标记的图标资源。osmdroid 的每个 Marker 需要各自持有 Drawable，
     * 所以这里只暴露资源 id，由使用方自行创建。
     */
    public final static int MAP_INDICATOR_RES = R.drawable.icon_gcoding;
    /**
     * 当前标记的地图点。
     * 注意：这里存放的始终是 <b>BD-09</b> 坐标（与原实现保持一致），
     * 只有在绘制到地图、以及地图回调返回坐标时，才通过 {@link MapUtils} 做 BD-09 / WGS-84 转换。
     */
    private static GeoPoint mMarkLatLngMap = new GeoPoint(36.547743718042415, 117.07018449827267);
    private static String mMarkName = null;
    /** 主界面地图（static 是为了让 HistoryActivity 能通过静态方法 showLocation 回填选点） */
    private static MapView mMapView;
    private static Marker mMarkMarker = null;
    private static Drawable sMapIndicatorDrawable = null;
    private OsmGeocoder mGeoCoder;
    private MyLocationNewOverlay mMyLocationOverlay;
    private LocationManager mSysLocManager;
    private LocationListener mSysLocListener;
    private SensorManager mSensorManager;
    private Sensor mSensorAccelerometer;
    private Sensor mSensorMagnetic;
    private float[] mAccValues = new float[3];//加速度传感器数据
    private float[] mMagValues = new float[3];//地磁传感器数据
    private final float[] mR = new float[9];//旋转矩阵，用来保存磁场和加速度的数据
    private final float[] mDirectionValues = new float[3];//模拟方向传感器的数据（原始数据为弧度）
    /************** 定位 *****************/
    private double mCurrentLat = 0.0;       // 当前位置的纬度（WGS-84）
    private double mCurrentLon = 0.0;       // 当前位置的经度（WGS-84）
    private float mCurrentDirection = 0.0f;
    private boolean isFirstLoc = true; // 是否首次定位
    private boolean isMockServStart = false;
    private ServiceGo.ServiceGoBinder mServiceBinder;
    /** 是否已经绑定过 ServiceGo（与 isMockServStart 分开记，退出时要保证解绑） */
    private boolean mBoundToService = false;
    private ServiceConnection mConnection;
    private FloatingActionButton mButtonStart;
    /** 脚本模式入口按钮 */
    private FloatingActionButton mButtonScript;
    /** 当前正在播放的脚本 id，null 表示没有在播放脚本（由定位线程回调更新） */
    private volatile String mRunningScriptId;
    /**
     * 已经发出“开始移动”、但服务连接还没回来时记下的脚本 id。
     *
     * <p>连上服务后用它兜底补一次启动：以前“点开始”只依赖 onStartCommand 异步送达，
     * 一旦没送到（或服务端状态过期），界面显示在播放、位置却不动，点停止也没反应。</p>
     */
    private String mPendingScriptId;
    /** 已经发出“停止移动”、但服务连接还没回来时的挂起标记，连上后再补一次停止 */
    private boolean mPendingStop;
    /**
     * 脚本是否处于暂停中（由服务端状态回调 / 每次同步更新）。
     *
     * <p>暂停与播放共用“正在播放”的本地缓存 {@link #mRunningScriptId}，
     * 单独记一个标记才能把“暂停 / 继续”按钮切到正确一侧。</p>
     */
    private volatile boolean mScriptPaused;
    /** 已经点过“暂停 / 继续”、但服务连接还没回来时记下的目标状态，连上后补执行一次 */
    private Boolean mPendingPauseRequest;
    /** 左下角开关的状态：true = 点击移动，false = 摇杆移动（默认由“移动类型”设置决定） */
    private boolean mClickMoveEnabled;
    /** 左下角“点击移动 ⇄ 摇杆移动”开关，模拟位置运行期间才显示 */
    private Button mClickMoveSwitch;
    /** 上一次刷新时脚本是否在播（用于检测“没在播 → 在播”并开启地图跟随） */
    private boolean mWasScriptRunning;
    /** 定位图层自动重试是否已挂起（同一时刻只保留一条重试链） */
    private boolean mMyLocationRetryPending;
    /*============================== 主界面连续选点（连贯移动） ==============================*/
    /**
     * 主界面选点生成的路线在脚本存储里的固定 id。
     * 每次重新选点都覆盖同一条，避免脚本列表里堆满“地图路线”。
     */
    private static final String MAP_ROUTE_ID = "map_route_from_main_map";
    /** 记录最近一次“主界面选点”生成的路线 id（被编辑过会换成新 id） */
    private static final String KEY_MAIN_MAP_ROUTE_ID = "main_map_route_id";
    /** 路线选点模式开关：开启时点地图是“往路线末尾加一个路点”，而不是原来的“选一个传送点” */
    private boolean mRoutePicking = false;
    /** 已选路点（WGS-84，与 ServiceGo / ScriptPlayer 使用的坐标语义一致） */
    private final List<ScriptWaypoint> mRoutePoints = new ArrayList<>();
    /** 新路点使用的状态（步行 / 跑步 / 骑行），速度取“设置”里对应的值 */
    private ScriptWaypoint.Mode mRouteMode = ScriptWaypoint.Mode.WALK;
    private FloatingActionButton mButtonRoute;
    // route_bar / click_move_bar 的根节点是 MaterialCardView（卡片化），
    // 字段声明成 View 而不是 LinearLayout：findViewById<T> 会插入 checkcast，
    // 类型对不上会直接 ClassCastException。两者实际只调用 setVisibility。
    private View mRouteBar;
    private TextView mRouteHint;
    private Spinner mRouteModeSpinner;
    private MaterialButton mRouteGoButton;
    /** 路线操作条上的“暂停 / 继续”按钮（路线没有在跑时隐藏） */
    private MaterialButton mRoutePauseButton;
    /** 路线连线：点地图加路点时实时重画 */
    private Polyline mRouteLine;
    private final List<Marker> mRouteMarkers = new ArrayList<>();
    /** 地图点击派发器：始终保持在图层最上面，保证点地图不会被路线图钉 / 连线吃掉 */
    private MapEventsOverlay mMapEventsOverlay;
    /*============================== 点击移动（多目标 + 行进轨迹） ==============================*/
    /**
     * 点击移动的待走目标队列（WGS-84）。
     *
     * <p>队列里<b>只有还没走到的点</b>：走到一个就从队头删掉（图钉与连线随之消失），
     * 所以“下一个要下发的目标”永远是 {@code get(0)}，不需要额外的下标。
     * 队头即当前正在走向的目标，队尾是最后追加的。</p>
     */
    private final List<ScriptWaypoint> mClickMovePoints = new ArrayList<>();
    /** 从上一个目标到当前位置的连线（只有起点时才显示，形成完整的行进轨迹） */
    private Polyline mClickMoveLine;
    private final List<Marker> mClickMoveMarkers = new ArrayList<>();
    /** 点击移动的操作条：撤回 / 清空 / 开始行走（根节点同为 MaterialCardView，见 mRouteBar） */
    private View mClickMoveBar;
    private TextView mClickMoveHint;
    /** 主操作按钮，用 MaterialButton 才能随状态切换图标（开始 ↔ 停止） */
    private MaterialButton mClickMoveGoButton;
    /** 点击移动是否正在行进（由服务端到达回调驱动） */
    private volatile boolean mClickMoveRunning;
    /*============================== 历史记录 相关 ==============================*/
    private SQLiteDatabase mLocationHistoryDB;
    private SQLiteDatabase mSearchHistoryDB;
    /*============================== SearchView 相关 ==============================*/
    private SearchView searchView;
    private ListView mSearchList;
    // 这两个下拉层在 main_content.xml 里也做成了 MaterialCardView；
    // 注意 activity_history.xml 里还有一个同名的 search_linear 且仍是 LinearLayout，
    // 那是 HistoryActivity 自己的布局，两者按各自 layout 解析，互不影响。
    private View mSearchLayout;
    private ListView mSearchHistoryList;
    private View mHistoryLayout;
    private MenuItem searchItem;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.nav_drawer_open, R.string.nav_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        XLog.i("MainActivity: onCreate");

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mOkHttpClient = new OkHttpClient();

        initNavigationView();

        initMap();

        initMapLocation();

        initMapButton();

        initGoBtn();

        initRoutePicking();

        initClickMoveBar();

        initClickMoveSwitch();

        // 恢复旋屏前还没开始移动的选点
        if (savedInstanceState != null) {
            mRoutePoints.clear();
            mRoutePoints.addAll(ScriptStore.decodePoints(savedInstanceState.getString(STATE_ROUTE_POINTS, "")));
            ScriptWaypoint.Mode[] modes = ScriptWaypoint.Mode.values();
            int modeOrdinal = savedInstanceState.getInt(STATE_ROUTE_MODE, 0);
            if (modeOrdinal >= 0 && modeOrdinal < modes.length) {
                mRouteMode = modes[modeOrdinal];
                // 让下拉跟上恢复出来的状态，避免回显成默认的“步行”
                if (mRouteModeSpinner != null) {
                    mRouteModeSpinner.setSelection(modeOrdinal);
                }
            }
            // 恢复“正在选点”的界面状态，但不弹提示（旋屏不该再提示一次）
            if (savedInstanceState.getBoolean(STATE_ROUTE_PICKING, false)) {
                mRoutePicking = true;
                if (mRouteBar != null) {
                    mRouteBar.setVisibility(View.VISIBLE);
                }
                if (mButtonRoute != null) {
                    mButtonRoute.setImageResource(R.drawable.ic_close);
                }
            }
            redrawRoute();

            // “单点模拟中”是实例字段，必须跨重建活下来：否则重建后 FAB 图标退回未启动态、
            // “终止模拟”入口失效，而且重建后的实例退出时不再 stopService（服务泄漏）
            isMockServStart = savedInstanceState.getBoolean(STATE_MOCK_SERVING, false);
            if (mButtonStart != null) {
                mButtonStart.setImageResource(isMockServStart ? R.drawable.ic_fly : R.drawable.ic_position);
            }
            refreshClickMoveSwitch();
        }

        mConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mServiceBinder = (ServiceGo.ServiceGoBinder)service;
                // 脚本播放状态变化时刷新入口按钮（脚本播放可能在脚本模式页面里启动）
                mServiceBinder.setScriptListener(mScriptListener);
                // 点击移动“走到下一个目标”的回调：目标队列在主界面，服务端只管走
                mServiceBinder.setClickMoveListener(new ServiceGo.ClickMoveListener() {
                    @Override
                    public void onClickTargetArrived(double lng, double lat) {
                        onServiceClickArrived();
                    }
                });
                syncStateWithService();
                // 每次连上都把当前的“点击移动 ⇄ 摇杆移动”开关状态推给服务端
                applyClickMoveMode();
                // 重新绑定（切应用 / 切页面回来）时把最后一次真实定位补报一次：
                // 服务端在脚本接管期间只记录、不覆盖，所以不会把脚本位置拉回真实位置
                if (mCurrentLat != 0 || mCurrentLon != 0) {
                    try {
                        mServiceBinder.onRealLocation(mCurrentLon, mCurrentLat);
                    } catch (Exception e) {
                        XLog.e("ERROR: onRealLocation on connect", e);
                    }
                }
                updateScriptButton();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                // 服务异常退出：本地引用已经失效，必须清掉，
                // 否则之后的“停止 / 开始”全都打在一个死对象上，表现为点了没反应
                mServiceBinder = null;
                mBoundToService = false;
                mClickMoveRunning = false;
                updateScriptButton();
            }
        };

        initStoreHistory();

        initSearchView();
    }

    @Override
    protected void onPause() {
        XLog.i("MainActivity: onPause");
        if (mMapView != null) {
            mMapView.onPause();
        }
        unregisterSensorListener();
        super.onPause();
    }

    /** 旋屏 / 重建时保存的“还没开始移动的路线选点” */
    private static final String STATE_ROUTE_POINTS = "state_route_points";
    private static final String STATE_ROUTE_MODE = "state_route_mode";
    private static final String STATE_ROUTE_PICKING = "state_route_picking";
    /** 旋屏 / 重建时保存的“单点模拟是否在跑” */
    private static final String STATE_MOCK_SERVING = "state_mock_serving";

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        // 选点还没变成一条真正在跑的路线，只有内存里这一份，不存就白点一遍
        outState.putString(STATE_ROUTE_POINTS, ScriptStore.encodePoints(mRoutePoints));
        outState.putInt(STATE_ROUTE_MODE, mRouteMode.ordinal());
        outState.putBoolean(STATE_ROUTE_PICKING, mRoutePicking);
        // 单点模拟的运行标记：跨重建保存，重建后 FAB 状态与退出收尾都靠它
        outState.putBoolean(STATE_MOCK_SERVING, isMockServStart);
    }

    @Override
    protected void onResume() {
        XLog.i("MainActivity: onResume");
        if (mMapView != null) {
            mMapView.onResume();
            // 回到界面前模拟位置服务可能移除过测试提供者，这一帧 enable 会失败：
            // 立即补一次，再延迟补一次，等提供者建回来箭头就能恢复
            ensureMyLocationEnabled();
            mMapView.postDelayed(this::ensureMyLocationEnabled, 800);
        }
        if (mSensorManager != null && mSensorAccelerometer != null) {
            mSensorManager.registerListener(this, mSensorAccelerometer, SensorManager.SENSOR_DELAY_UI);
        }
        if (mSensorManager != null && mSensorMagnetic != null) {
            mSensorManager.registerListener(this, mSensorMagnetic, SensorManager.SENSOR_DELAY_UI);
        }

        // 回到主界面时同步一次脚本播放状态（脚本可能是在脚本模式页面里启动的）
        String running = new ScriptStore(this).getRunningScriptId();
        if (running != null) {
            mRunningScriptId = running;
            // 存储里有运行记录就把服务绑上：既让“停止移动”走 binder（而不是直接
            // stopService 把服务打死），也方便连上后用服务端真实状态纠正本地缓存
            bindServiceIfNeeded();
        } else if (mServiceBinder == null) {
            // 存储里没有运行记录，本地残留的“正在播放”一定是过期状态，纠正掉
            // （否则按钮会一直显示“停止移动”，点下去却是停一个没在跑的脚本）
            mRunningScriptId = null;
        }
        if (isMockServStart && mServiceBinder == null) {
            // 单点模拟在跑但还没绑上：先绑好，否则 FAB 首次按下只补绑、位置要第二次才生效
            bindServiceIfNeeded();
        }
        updateScriptButton();

        super.onResume();
    }

    @Override
    protected void onStop() {
        XLog.i("MainActivity: onStop");
        //取消注册传感器监听
        unregisterSensorListener();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        XLog.i("MainActivity: onDestroy");

        // 只有真正退出界面（isFinishing）才停服务：旋屏 / 配置变化导致的重建只解绑，
        // 否则单点模拟和播放中的脚本会被旧实例顺手 kill 掉（“转个屏脚本就停了”）。
        // 重建后的实例已通过 onSaveInstanceState 恢复 isMockServStart，最终退出时仍会正确收尾。
        if (isFinishing() && isMockServStart) {
            try {
                unbindServiceIfNeeded();
                Intent serviceGoIntent = new Intent(MainActivity.this, ServiceGo.class);
                stopService(serviceGoIntent);
            } catch (Exception e) {
                XLog.e("ERROR: stop ServiceGo");
            }
            isMockServStart = false;
        } else {
            unbindServiceIfNeeded();
        }

        unregisterSensorListener();

        // 退出时销毁定位
        if (mSysLocManager != null && mSysLocListener != null) {
            try {
                mSysLocManager.removeUpdates(mSysLocListener);
            } catch (Exception e) {
                XLog.e("ERROR: removeUpdates");
            }
        }
        // 关闭定位图层
        if (mMyLocationOverlay != null) {
            try {
                mMyLocationOverlay.disableMyLocation();
            } catch (Exception e) {
                XLog.e("ERROR: disableMyLocation");
            }
        }
        if (mMapView != null) {
            mMapView.onDetach();
        }

        //close db
        if (mLocationHistoryDB != null) {
            mLocationHistoryDB.close();
        }
        if (mSearchHistoryDB != null) {
            mSearchHistoryDB.close();
        }

        super.onDestroy();
    }

    /** 注销传感器监听（mSensorManager 可能因为初始化失败而为空） */
    private void unregisterSensorListener() {
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }
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

                if (!data.isEmpty()) {
                    SimpleAdapter simAdapt = new SimpleAdapter(
                            MainActivity.this,
                            data,
                            R.layout.search_item,
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
        searchView.setIconified(false);// 设置searchView处于展开状态
        searchView.onActionViewExpanded();// 当展开无输入内容的时候，没有关闭的图标
        searchView.setIconifiedByDefault(true);//默认为true在框内，设置false则在框外
        searchView.setSubmitButtonEnabled(false);//显示提交按钮
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                try {
                    doSearch(query);
                    //搜索历史 插表参数
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(DataBaseHistorySearch.DB_COLUMN_KEY, query);
                    contentValues.put(DataBaseHistorySearch.DB_COLUMN_DESCRIPTION, "搜索关键字");
                    contentValues.put(DataBaseHistorySearch.DB_COLUMN_IS_LOCATION, DataBaseHistorySearch.DB_SEARCH_TYPE_KEY);
                    contentValues.put(DataBaseHistorySearch.DB_COLUMN_TIMESTAMP, System.currentTimeMillis() / 1000);

                    DataBaseHistorySearch.saveHistorySearch(mSearchHistoryDB, contentValues);
                    clearMark(mMapView, mMarkMarker);
                    mSearchLayout.setVisibility(View.INVISIBLE);
                } catch (Exception e) {
                    GoUtils.DisplayToast(MainActivity.this, getResources().getString(R.string.app_error_search));
                    XLog.d(getResources().getString(R.string.app_error_search));
                }

                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                //当输入框内容改变的时候回调
                //搜索历史置为不可见
                mHistoryLayout.setVisibility(View.INVISIBLE);

                if (newText != null && !newText.isEmpty()) {
                    try {
                        doSearch(newText);
                    } catch (Exception e) {
                        GoUtils.DisplayToast(MainActivity.this, getResources().getString(R.string.app_error_search));
                        XLog.d(getResources().getString(R.string.app_error_search));
                    }
                }

                return true;
            }
        });

        // 搜索框的清除按钮(该按钮属于安卓系统图标)
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
        mCurrentDirection = (float) Math.toDegrees(mDirectionValues[0]);    // 弧度转角度
        if (mCurrentDirection < 0) {    // 由 -180 ~ + 180 转为 0 ~ 360
            mCurrentDirection += 360;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    /*============================== NavigationView 相关 ==============================*/
    private void initNavigationView() {
        /*============================== NavigationView 相关 ==============================*/
        NavigationView mNavigationView = findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_history) {
                Intent intent = new Intent(MainActivity.this, HistoryActivity.class);

                startActivity(intent);
            } else if (id == R.id.nav_script) {
                Intent intent = new Intent(MainActivity.this, ScriptActivity.class);

                startActivity(intent);
            } else if (id == R.id.nav_settings) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            } else if (id == R.id.nav_dev) {
                if (!GoUtils.isDeveloperOptionsEnabled(this)) {
                    GoUtils.DisplayToast(this, getResources().getString(R.string.app_error_dev));
                } else {
                    try {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
                        startActivity(intent);
                    } catch (Exception e) {
                        GoUtils.DisplayToast(this, getResources().getString(R.string.app_error_dev));
                    }
                }
            } else if (id == R.id.nav_feedback) {
                File file = new File(getExternalFilesDir("Logs"), GoApplication.LOG_FILE_NAME);
                ShareUtils.shareFile(this, file, item.getTitle().toString());
            } else if (id == R.id.nav_contact) {
                Uri uri = Uri.parse("https://github.com/MilkyPark142008/GoGoGo");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }

            DrawerLayout drawer = findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);

            return true;
        });

        // 直接获取第 0 个头部视图
        View headerView = mNavigationView.getHeaderView(0);
        TextView app_version = headerView.findViewById(R.id.app_version);
        app_version.setText(GoUtils.getVersionName(this));
    }

    /*============================== 主界面地图 相关 ==============================*/
    private void initMap() {
        // 地图初始化
        mMapView = findViewById(R.id.map_main);
        mMapView.setBuiltInZoomControls(false);
        mMapView.setMultiTouchControls(true);
        mMapView.setTilesScaledToDpi(true);
        // 默认使用 OpenStreetMap
        mMapView.setTileSource(TileSourceUtils.OSM_STANDARD);
        mMapView.getController().setZoom(18.0);

        sMapIndicatorDrawable = ContextCompat.getDrawable(this, MAP_INDICATOR_RES);

        // 地点检索 / 逆地理编码（OpenStreetMap Nominatim）
        mGeoCoder = new OsmGeocoder(mOkHttpClient);

        View poiView = View.inflate(MainActivity.this, R.layout.location_poi_info, null);
        TextView poiAddress = poiView.findViewById(R.id.poi_address);
        TextView poiLongitude = poiView.findViewById(R.id.poi_longitude);
        TextView poiLatitude = poiView.findViewById(R.id.poi_latitude);
        ImageButton ibSave = poiView.findViewById(R.id.poi_save);
        ibSave.setOnClickListener(v -> {
            recordCurrentLocation(mMarkLatLngMap.getLongitude(), mMarkLatLngMap.getLatitude());
            GoUtils.DisplayToast(this, getResources().getString(R.string.app_location_save));
        });
        ImageButton ibCopy = poiView.findViewById(R.id.poi_copy);
        ibCopy.setOnClickListener(v -> {
            //获取剪贴板管理器：
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            // 创建普通字符型ClipData
            ClipData mClipData = ClipData.newPlainText("Label",
                    mMarkLatLngMap.getLongitude() + "," + mMarkLatLngMap.getLatitude());
            // 将 ClipData内容放到系统剪贴板里。
            cm.setPrimaryClip(mClipData);

            GoUtils.DisplayToast(this,  getResources().getString(R.string.app_location_copy));
        });
        ImageButton ibShare = poiView.findViewById(R.id.poi_share);
        ibShare.setOnClickListener(v -> ShareUtils.shareText(MainActivity.this, "分享位置", poiLongitude.getText()+","+poiLatitude.getText()));
        ImageButton ibFly = poiView.findViewById(R.id.poi_fly);
        ibFly.setOnClickListener(this::doGoLocation);
        // 选点标记
        mMarkMarker = createMarkMarker();
        if (mMarkMarker != null) {
            mMarkMarker.setInfoWindow(new PoiInfoWindow(poiView, mMapView));
            // 单击标记默认会弹出信息气泡，这里屏蔽掉，只在长按逆地理编码成功后主动弹出
            mMarkMarker.setOnMarkerClickListener((marker, mapView) -> true);
            mMapView.getOverlays().add(mMarkMarker);
        }

        // 地图单击 / 长按事件。osmdroid 通过 MapEventsOverlay 派发，回调里拿到的是 WGS-84 坐标；
        // 双击缩放是 osmdroid 内置行为，不需要额外代码。
        MapEventsOverlay eventsOverlay = new MapEventsOverlay(new MapEventsReceiver() {
            /**
             * 单击地图
             */
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                // 路线选点模式优先：点地图 = 往路线末尾加一个路点，不改动原来的单点传送标记
                if (mRoutePicking) {
                    addRoutePoint(p.getLongitude(), p.getLatitude());
                    return true;
                }

                // 点击移动：开关处于“点击移动”且模拟位置在跑时，点哪朝哪走（脚本播放中会提示并忽略）
                if (handleClickMoveTap(p)) {
                    return true;
                }

                mMarkLatLngMap = wgs84ToBd09(p.getLongitude(), p.getLatitude());
                markMap();
                return true;
            }

            /**
             * 长按地图
             */
            @Override
            public boolean longPressHelper(GeoPoint p) {
                if (mRoutePicking) {
                    addRoutePoint(p.getLongitude(), p.getLatitude());
                    return true;
                }

                mMarkLatLngMap = wgs84ToBd09(p.getLongitude(), p.getLatitude());
                markMap();
                // 逆地理编码，传入 WGS-84 坐标
                mGeoCoder.reverse(p.getLatitude(), p.getLongitude(), poi -> {
                    if (poi == null) {
                        XLog.i("逆地理位置失败!");
                        return;
                    }
                    mMarkName = String.valueOf(poi.name);
                    // 界面上一律沿用原有的 BD-09 语义展示
                    GeoPoint bd09 = wgs84ToBd09(poi.longitude, poi.latitude);
                    poiLatitude.setText(String.valueOf(bd09.getLatitude()));
                    poiLongitude.setText(String.valueOf(bd09.getLongitude()));
                    poiAddress.setText(poi.address);
                    if (mMarkMarker != null) {
                        mMarkMarker.setTitle(poi.address);
                        mMarkMarker.setSnippet(poi.name);
                        mMarkMarker.showInfoWindow();
                    }
                });
                return true;
            }
        });
        mMapEventsOverlay = eventsOverlay;
        mMapView.getOverlays().add(eventsOverlay);

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

    /**
     * 开启地图的定位图层。
     * 原实现依赖百度定位 SDK，这里改为系统 LocationManager + osmdroid 的定位图层，
     * 显示的同样是系统位置（也就是 ServiceGo 注入的模拟位置）。
     */
    private void initMapLocation() {
        try {
            // 定位属于运行时权限：WelcomeActivity 已申请，这里再显式检查一次，
            // 既避免权限被撤销后抛 SecurityException，也满足 lint 的 MissingPermission 检查。
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                XLog.e("ERROR: location permission is not granted");
                return;
            }

            // 定位图层
            mMyLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mMapView);
            mMyLocationOverlay.enableMyLocation();
            mMapView.getOverlays().add(mMyLocationOverlay);

            // 系统定位，用于首次进入时把地图移动到当前位置
            mSysLocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (mSysLocManager == null) {
                return;
            }

            mSysLocListener = new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    if (location == null || mMapView == null) {// mapview 销毁后不在处理新接收的位置
                        return;
                    }

                    mCurrentLat = location.getLatitude();
                    mCurrentLon = location.getLongitude();

                    // 把真实定位交给服务端：脚本 / 点击移动正在接管位置时它只记录不覆盖。
                    // 以前这里（以及服务端的真实定位回调）会直接写当前位置，
                    // 切一次应用就把模拟坐标拉回手机所在地，表现为“脚本失灵 + 回到真实位置”。
                    if (mServiceBinder != null) {
                        try {
                            mServiceBinder.onRealLocation(mCurrentLon, mCurrentLat);
                        } catch (Exception e) {
                            XLog.e("ERROR: onRealLocation", e);
                        }
                    }

                    if (isFirstLoc) {
                        isFirstLoc = false;
                        // 内部依旧保存 BD-09 坐标
                        mMarkLatLngMap = wgs84ToBd09(mCurrentLon, mCurrentLat);
                        mMapView.getController().setZoom(18.0);
                        // 脚本正在接管位置时不要抢镜头（否则切一次应用，地图就飞回真实位置，
                        // 看起来和“模拟位置回到正确位置”一模一样）
                        if (!isScriptPlaying()) {
                            mMapView.getController().animateTo(new GeoPoint(mCurrentLat, mCurrentLon));
                        }

                        XLog.i("First WGS84 LatLng: " + mCurrentLat + "," + mCurrentLon);
                    }
                }

                @Override
                public void onProviderEnabled(@NonNull String provider) {
                }

                @Override
                public void onProviderDisabled(@NonNull String provider) {
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {
                }
            };

            for (String provider : mSysLocManager.getProviders(true)) {
                try {
                    mSysLocManager.requestLocationUpdates(provider, 1000L, 0.0f,
                            mSysLocListener, Looper.getMainLooper());
                } catch (Exception e) {
                    XLog.e("ERROR: requestLocationUpdates - " + provider);
                }
            }

            // 先用最后一次已知位置把地图定位过去，避免等待首次回调
            Location lastLocation = null;
            try {
                lastLocation = mSysLocManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastLocation == null) {
                    lastLocation = mSysLocManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
            } catch (Exception e) {
                XLog.e("ERROR: getLastKnownLocation");
            }
            if (lastLocation != null) {
                mSysLocListener.onLocationChanged(lastLocation);
            }
        } catch (Exception e) {
            XLog.e("ERROR: initMapLocation");
        }
    }

    //地图上各按键的监听
    private void initMapButton() {
        RadioGroup mGroupMapType = this.findViewById(R.id.RadioGroupMapType);
        mGroupMapType.setOnCheckedChangeListener((group, checkedId) -> {
            if (mMapView == null) {
                return;
            }

            if (checkedId == R.id.mapNormal) {
                // OpenStreetMap 标准地图
                mMapView.setTileSource(TileSourceUtils.OSM_STANDARD);
            }

            if (checkedId == R.id.mapSatellite) {
                // Esri World Imagery 卫星影像（瓦片顺序 Z/Y/X）
                mMapView.setTileSource(TileSourceUtils.ESRI_WORLD_IMAGERY);
            }

            mMapView.invalidate();
        });

        ImageButton curPosBtn = this.findViewById(R.id.cur_position);
        curPosBtn.setOnClickListener(v -> resetMap());

        ImageButton zoomInBtn = this.findViewById(R.id.zoom_in);
        zoomInBtn.setOnClickListener(v -> {
            if (mMapView != null) {
                mMapView.getController().zoomIn();
            }
        });

        ImageButton zoomOutBtn = this.findViewById(R.id.zoom_out);
        zoomOutBtn.setOnClickListener(v -> {
            if (mMapView != null) {
                mMapView.getController().zoomOut();
            }
        });

        ImageButton inputPosBtn = this.findViewById(R.id.input_pos);
        inputPosBtn.setOnClickListener(v -> showInputPositionDialog());
    }

    /**
     * 手动输入经纬度定位：经度、纬度分栏输入。
     *
     * <p>剪贴板（或“粘贴”按钮）里的“经度,纬度”整串坐标会自动分填到两格；
     * 旧版是单框 + 自动识别，容易看岔格式，改成两格后所见即所得。</p>
     */
    private void showInputPositionDialog() {
        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(MainActivity.this);
        builder.setTitle(R.string.input_button);
        View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.location_input, null);
        builder.setView(view);

        final AlertDialog dialog = builder.show();

        final EditText lngEdit = view.findViewById(R.id.input_position_lng);
        final EditText latEdit = view.findViewById(R.id.input_position_lat);
        RadioButton rbGps = view.findViewById(R.id.pos_type_gps);
        Button btnPaste = view.findViewById(R.id.input_position_paste);

        // 剪贴板里如果有经纬度，直接分填两格，省去手动粘贴
        fillLngLatFromClipboard(lngEdit, latEdit);

        btnPaste.setOnClickListener(v -> {
            if (!fillLngLatFromClipboard(lngEdit, latEdit)) {
                GoUtils.DisplayToast(MainActivity.this,
                        getResources().getString(R.string.input_position_clip_invalid));
            }
        });

        Button btnGo = view.findViewById(R.id.input_position_ok);
        btnGo.setOnClickListener(v2 -> {
            double[] lngLat = readLngLatFields(lngEdit, latEdit);
            if (lngLat == null) {
                return;     // 校验失败的提示已经在 readLngLatFields 里发过了
            }

            if (rbGps.isChecked()) {
                // GPS 坐标系（WGS-84）：内部仍按 BD-09 保存，绘制时再转回来
                double[] bd09 = MapUtils.wgs2bd09(lngLat[0], lngLat[1]);
                mMarkLatLngMap = new GeoPoint(bd09[1], bd09[0]);
            } else {
                mMarkLatLngMap = new GeoPoint(lngLat[1], lngLat[0]);
            }

            mMarkName = getResources().getString(R.string.input_button);
            markMap();
            mMapView.getController().setZoom(18.0);
            mMapView.getController().setCenter(
                    bd09ToWgs84(mMarkLatLngMap.getLongitude(), mMarkLatLngMap.getLatitude()));

            dialog.dismiss();
            GoUtils.DisplayToast(MainActivity.this, getResources().getString(R.string.input_position_ok_toast));
        });

        Button btnCancel = view.findViewById(R.id.input_position_cancel);
        btnCancel.setOnClickListener(v1 -> dialog.dismiss());
    }

    /**
     * 把剪贴板里的经纬度解析出来分填到两个输入框。
     *
     * @return 解析成功返回 true；剪贴板为空或不是经纬度返回 false
     */
    private boolean fillLngLatFromClipboard(EditText lngEdit, EditText latEdit) {
        double[] lngLat = MapUtils.parseLngLat(getClipboardText());
        if (lngLat == null) {
            return false;
        }

        lngEdit.setText(formatCoordInput(lngLat[0]));
        latEdit.setText(formatCoordInput(lngLat[1]));
        lngEdit.setSelection(lngEdit.getText().length());
        return true;
    }

    /** 输入框里的坐标文本：保留 6 位精度并去掉多余的 0（116.397000 -> 116.397） */
    private static String formatCoordInput(double value) {
        String text = String.format(java.util.Locale.US, "%.6f", value);
        text = text.replaceFirst("0+$", "").replaceFirst("\\.$", "");
        return text.isEmpty() ? "0" : text;
    }

    /**
     * 读取并校验经度 / 纬度两个输入框。
     *
     * @return {经度, 纬度}；非法时弹提示并返回 null
     */
    private double[] readLngLatFields(EditText lngEdit, EditText latEdit) {
        String lngText = lngEdit.getText().toString().trim();
        String latText = latEdit.getText().toString().trim();
        if (TextUtils.isEmpty(lngText) || TextUtils.isEmpty(latText)) {
            GoUtils.DisplayToast(this, getResources().getString(R.string.input_position_empty));
            return null;
        }

        double lng;
        double lat;
        try {
            lng = Double.parseDouble(lngText);
            lat = Double.parseDouble(latText);
        } catch (NumberFormatException e) {
            GoUtils.DisplayToast(this, getResources().getString(R.string.app_error_input));
            return null;
        }

        if (lng < -180.0 || lng > 180.0 || lat < -90.0 || lat > 90.0) {
            GoUtils.DisplayToast(this, getResources().getString(R.string.coord_range_error));
            return null;
        }

        return new double[] {lng, lat};
    }

    /*============================== 跳转经纬度 / 地址 ==============================*/

    /** 读取剪贴板文本，失败或为空时返回 null */
    private String getClipboardText() {
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm == null || !cm.hasPrimaryClip()) {
                return null;
            }

            ClipData clipData = cm.getPrimaryClip();
            if (clipData == null || clipData.getItemCount() <= 0) {
                return null;
            }

            CharSequence text = clipData.getItemAt(0).coerceToText(this);
            return text == null ? null : text.toString().trim();
        } catch (Exception e) {
            XLog.e("ERROR: getClipboardText");
            return null;
        }
    }

    //标定选择的位置
    private void markMap() {
        markMapOnView(mMapView, mMarkMarker, mMarkLatLngMap);
    }

    /**
     * 把 BD-09 的选点绘制到地图上（绘制前转换成 WGS-84）
     */
    private static void markMapOnView(MapView mapView, Marker marker, GeoPoint bd09Point) {
        if (mapView == null || marker == null || bd09Point == null) {
            return;
        }
        marker.closeInfoWindow();
        marker.setPosition(bd09ToWgs84(bd09Point.getLongitude(), bd09Point.getLatitude()));
        if (!mapView.getOverlays().contains(marker)) {
            mapView.getOverlays().add(marker);
        }
        mapView.invalidate();
    }

    /**
     * 清除选点标记
     */
    private static void clearMark(MapView mapView, Marker marker) {
        if (mapView == null) {
            return;
        }
        if (marker != null) {
            marker.closeInfoWindow();
            mapView.getOverlays().remove(marker);
        }
        mapView.invalidate();
    }

    /**
     * 创建选点标记。osmdroid 的 Marker 需要各自持有一份 Drawable，
     * 所以这里从 ConstantState 新建，避免与摇杆悬浮窗地图共用同一个实例。
     */
    private Marker createMarkMarker() {
        if (mMapView == null || sMapIndicatorDrawable == null) {
            return null;
        }
        Drawable icon = sMapIndicatorDrawable;
        if (sMapIndicatorDrawable.getConstantState() != null) {
            icon = sMapIndicatorDrawable.getConstantState().newDrawable();
        }
        Marker marker = new Marker(mMapView);
        marker.setIcon(icon);
        // 图标底部中点对准坐标点，与原百度地图标记的视觉一致
        marker.setAnchor(0.5f, 1.0f);
        return marker;
    }

    /**
     * BD-09 -> WGS-84
     *
     * @return WGS-84 坐标点，可直接交给 osmdroid 绘制
     */
    private static GeoPoint bd09ToWgs84(double bd09Lng, double bd09Lat) {
        double[] wgs84 = MapUtils.bd2wgs(bd09Lng, bd09Lat);
        return new GeoPoint(wgs84[1], wgs84[0]);
    }

    /**
     * WGS-84 -> BD-09
     *
     * @return BD-09 坐标点，用于保持工程内部原有的坐标语义
     */
    private static GeoPoint wgs84ToBd09(double wgs84Lng, double wgs84Lat) {
        double[] bd09 = MapUtils.wgs2bd09(wgs84Lng, wgs84Lat);
        return new GeoPoint(bd09[1], bd09[0]);
    }

    private void resetMap() {
        clearMark(mMapView, mMarkMarker);
        mMarkLatLngMap = null;

        // 定位图层由 mMyLocationOverlay 负责刷新，这里只需要把视野移回当前位置
        if (mMapView != null && (mCurrentLat != 0.0 || mCurrentLon != 0.0)) {
            mMapView.getController().setZoom(18.0);
            mMapView.getController().animateTo(new GeoPoint(mCurrentLat, mCurrentLon));
        }
    }

    // 在地图上显示位置
    public static boolean showLocation(String name, String bd09Longitude, String bd09Latitude) {
        boolean ret = true;

        try {
            if (!bd09Longitude.isEmpty() && !bd09Latitude.isEmpty()) {
                mMarkName = name;
                mMarkLatLngMap = new GeoPoint(Double.parseDouble(bd09Latitude), Double.parseDouble(bd09Longitude));
                markMapOnView(mMapView, mMarkMarker, mMarkLatLngMap);
                mMapView.getController().setCenter(
                        bd09ToWgs84(mMarkLatLngMap.getLongitude(), mMarkLatLngMap.getLatitude()));
            }
        } catch (Exception e) {
            ret = false;
            XLog.e("ERROR: showHistoryLocation");
        }

        return ret;
    }

    /**
     * 主界面地图上的 POI 信息气泡，对应原实现里的百度 InfoWindow。
     * 布局与按钮逻辑完全复用 location_poi_info。
     */
    private static class PoiInfoWindow extends InfoWindow {
        PoiInfoWindow(View view, MapView mapView) {
            super(view, mapView);
        }

        @Override
        public void onOpen(Object item) {
        }

        @Override
        public void onClose() {
        }
    }

    private void initGoBtn() {
        mButtonStart = findViewById(R.id.faBtnStart);
        mButtonStart.setOnClickListener(this::doGoLocation);

        mButtonScript = findViewById(R.id.faBtnScript);
        if (mButtonScript != null) {
            mButtonScript.setOnClickListener(this::doScriptLocation);
        }
    }

    /** 脚本播放状态回调（可能在定位线程触发，刷新界面要切回主线程） */
    private final ServiceGo.ScriptListener mScriptListener = new ServiceGo.ScriptListener() {
        @Override
        public void onScriptSegment(com.zcshou.script.ScriptRoute route, int index,
                                    com.zcshou.script.ScriptWaypoint.Mode mode, double speed) {
            mRunningScriptId = route == null ? null : route.id;
            runOnUiThread(MainActivity.this::updateScriptButton);
        }

        @Override
        public void onScriptFinish(com.zcshou.script.ScriptRoute route) {
            // 跑完 = 结束：本地也按“没在播”处理，路线条直接回到“开始移动”、
            // 传送/点击移动不再被“播放中”拦截，模拟位置可以立刻继续
            mRunningScriptId = null;
            mScriptPaused = false;
            runOnUiThread(() -> {
                updateScriptButton();
                GoUtils.DisplayToast(MainActivity.this, getResources().getString(R.string.script_finished));
            });
        }

        @Override
        public void onScriptStopped() {
            mRunningScriptId = null;
            mScriptPaused = false;
            runOnUiThread(MainActivity.this::updateScriptButton);
        }

        @Override
        public void onScriptPaused() {
            mScriptPaused = true;
            runOnUiThread(MainActivity.this::updateScriptButton);
        }

        @Override
        public void onScriptResumed() {
            mScriptPaused = false;
            runOnUiThread(MainActivity.this::updateScriptButton);
        }
    };

    /** 按当前是否在播放脚本切换入口按钮的图标（路线操作条上的按钮随之刷新） */
    private void updateScriptButton() {
        // 脚本“没在播 → 在播”的沿变化：脚本启动会把位置对齐到路线首点（常在视野外），
        // 开启跟随让地图追着人物走，白色箭头始终在视野里；用户手动拖地图会自动退出跟随
        boolean scriptRunning = mRunningScriptId != null;
        if (scriptRunning && !mWasScriptRunning && mMyLocationOverlay != null) {
            try {
                mMyLocationOverlay.enableFollowLocation();
            } catch (Exception e) {
                XLog.e("ERROR: enableFollowLocation", e);
            }
        }
        mWasScriptRunning = scriptRunning;

        // 脚本启停可能伴随模拟位置服务重建，顺手确保定位图层还开着
        ensureMyLocationEnabled();
        // 模拟位置（脚本）的启停决定左下角开关的显隐
        refreshClickMoveSwitch();
        // 点击移动的“开始行走 / 停止行走”跟着运行状态刷新
        updateClickMoveUi();

        if (mButtonScript != null) {
            boolean running = mRunningScriptId != null;
            mButtonScript.setImageResource(running ? R.drawable.ic_close : R.drawable.ic_script);
        }

        updateRouteUi();
    }

    /**
     * 确保地图的定位图层（人物箭头）处于开启状态，并重新注册一次监听。
     *
     * <p>模拟位置服务销毁时会移除 GPS / 网络测试提供者，这期间回到界面对图层调
     * enableMyLocation() 会因为“没有可用提供者”**静默失败**（GpsMyLocationProvider
     * 吞掉异常返回 false，图层的 mIsLocationEnabled 置为 false 且不再自动重试），
     * 表现就是“切换到脚本 / 从脚本切回摇杆后白色箭头不见了”，而模拟位置本身照常。</p>
     *
     * <p>脚本启动、停止走 stopService 都正好落在“提供者先删后加”的窗口里，
     * 所以这里失败时会挂一个 1 秒后的自动重试（同一时刻只允许一条重试链，
     * 成功即停），提供者一建回来箭头立刻恢复，不必干等下一个界面事件。</p>
     */
    private void ensureMyLocationEnabled() {
        if (mMyLocationOverlay == null) {
            return;
        }

        try {
            boolean ok = mMyLocationOverlay.enableMyLocation();
            if (ok) {
                mMyLocationRetryPending = false;
                return;
            }

            if (!mMyLocationRetryPending && mMapView != null) {
                mMyLocationRetryPending = true;
                mMapView.postDelayed(() -> {
                    mMyLocationRetryPending = false;
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    ensureMyLocationEnabled();
                }, 1000);
            }
        } catch (Exception e) {
            XLog.e("ERROR: enableMyLocation", e);
        }
    }

    /**
     * 脚本模式入口：直接打开脚本列表。
     *
     * <p>上一次模拟的位置已经由 ServiceGo 保存在 SharedPreferences 里，脚本编辑页可以直接引用，
     * 因此这里不需要绑定服务。</p>
     */
    private void doScriptLocation(View v) {
        // 已经在播放脚本时，这个按钮变成“停止脚本”
        if (mRunningScriptId != null && mServiceBinder != null) {
            boolean finished = mServiceBinder.getScriptState() == ServiceGo.SCRIPT_STATE_FINISHED;
            try {
                mServiceBinder.stopScript();
                mRunningScriptId = null;
                mScriptPaused = false;
                updateScriptButton();
                Snackbar.make(v, getResources().getString(
                                finished ? R.string.script_finished : R.string.script_stopped),
                        Snackbar.LENGTH_LONG).setAction("Action", null).show();

                // 单次脚本已经跑完，顺手把服务停掉，避免前台通知一直挂着
                // 注意：stopService 只有在没有绑定时才会真正销毁服务，所以这里要先解绑
                if (finished) {
                    unbindServiceIfNeeded();
                    stopService(new Intent(MainActivity.this, ServiceGo.class));
                    isMockServStart = false;
                }
                return;
            } catch (Exception e) {
                XLog.e("ERROR: stopScript", e);
            }
        }

        startActivity(new Intent(MainActivity.this, ScriptActivity.class));
    }

    /*============================== 主界面连续选点成路线（连贯移动） ==============================*/

    /** 路线选点入口按钮、操作条与连线图层 */
    private void initRoutePicking() {
        mButtonRoute = findViewById(R.id.faBtnRoute);
        if (mButtonRoute != null) {
            mButtonRoute.setOnClickListener(v -> setRoutePicking(!mRoutePicking));
        }

        mRouteBar = findViewById(R.id.route_bar);
        mRouteHint = findViewById(R.id.route_hint);
        mRouteGoButton = findViewById(R.id.route_go);
        mRoutePauseButton = findViewById(R.id.route_pause);

        mRouteModeSpinner = findViewById(R.id.route_mode);
        if (mRouteModeSpinner != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                    new String[] {
                            getResources().getString(R.string.script_mode_walk),
                            getResources().getString(R.string.script_mode_run),
                            getResources().getString(R.string.script_mode_bike)});
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mRouteModeSpinner.setAdapter(adapter);
            mRouteModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    ScriptWaypoint.Mode mode = position == 1 ? ScriptWaypoint.Mode.RUN
                            : position == 2 ? ScriptWaypoint.Mode.BIKE : ScriptWaypoint.Mode.WALK;
                    // setAdapter / setSelection 触发的回声与当前取值一致时忽略，
                    // 避免进入选点模式的瞬间误切一次移动状态
                    if (mode == mRouteMode) {
                        return;
                    }

                    mRouteMode = mode;
                    // 路线正在跑时，切换同时对剩余路段生效（走 / 跑 / 骑随时切）
                    applyLiveMode(mode);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }

        View undoButton = findViewById(R.id.route_undo);
        if (undoButton != null) {
            undoButton.setOnClickListener(v -> undoRoutePoint());
        }
        View clearButton = findViewById(R.id.route_clear);
        if (clearButton != null) {
            clearButton.setOnClickListener(v -> clearRoutePoints());
        }
        View closeButton = findViewById(R.id.route_close);
        if (closeButton != null) {
            closeButton.setOnClickListener(v -> setRoutePicking(false));
        }
        if (mRouteGoButton != null) {
            mRouteGoButton.setOnClickListener(v -> {
                if (isMapRoutePlaying()) {
                    stopMapRoute();
                } else {
                    startMapRoute();
                }
            });
        }
        if (mRoutePauseButton != null) {
            mRoutePauseButton.setOnClickListener(v -> toggleMapRoutePause());
        }

        // 连线图层：先放进地图，选点过程中不断更新坐标列表
        if (mMapView != null) {
            mRouteLine = new Polyline(mMapView);
            mRouteLine.getOutlinePaint().setColor(ContextCompat.getColor(this, R.color.colorAccent));
            mRouteLine.getOutlinePaint().setStrokeWidth(8.0f);
            addRouteOverlay(mRouteLine);
        }

        updateRouteUi();
    }

    /**
     * 加入路线图层（连线 / 图钉）。
     *
     * <p>osmdroid 的事件按图层反序派发：这些图层只用来“看”，如果排在
     * {@link MapEventsOverlay} 之后，点在已有图钉或连线上就触达不到地图回调，
     * 也就没法继续加路点。所以每次加完都把点击派发器挪回最上面。</p>
     */
    private void addRouteOverlay(Overlay overlay) {
        if (mMapView == null || overlay == null) {
            return;
        }

        List<Overlay> overlays = mMapView.getOverlays();
        overlays.add(overlay);
        if (mMapEventsOverlay != null && overlays.remove(mMapEventsOverlay)) {
            overlays.add(mMapEventsOverlay);
        }
    }

    /** 开关“路线选点”模式 */
    private void setRoutePicking(boolean picking) {
        mRoutePicking = picking;
        if (mRouteBar != null) {
            mRouteBar.setVisibility(picking ? View.VISIBLE : View.GONE);
        }
        if (mButtonRoute != null) {
            mButtonRoute.setImageResource(picking ? R.drawable.ic_close : R.drawable.ic_route);
        }

        // 选点模式与点击移动的目标点操作条互斥：两条操作条都贴顶部，同时显示会叠在一起
        refreshClickMoveSwitch();

        GoUtils.DisplayToast(this, getResources().getString(
                picking ? R.string.route_mode_on : R.string.route_mode_off));
        updateRouteUi();
    }

    /**
     * 往路线末尾追加一个路点。
     *
     * @param lngWgs84 经度（WGS-84：地图回调给的就是这个坐标系，也正是 ServiceGo 注入系统的坐标系）
     * @param latWgs84 纬度（WGS-84）
     */
    private void addRoutePoint(double lngWgs84, double latWgs84) {
        mRoutePoints.add(new ScriptWaypoint(lngWgs84, latWgs84, getSettingAltitude(),
                mRouteMode, getModeSpeed(mRouteMode), 0));
        redrawRoute();
    }

    /** 撤销最后一个路点 */
    private void undoRoutePoint() {
        if (mRoutePoints.isEmpty()) {
            GoUtils.DisplayToast(this, getResources().getString(R.string.route_empty));
            return;
        }

        mRoutePoints.remove(mRoutePoints.size() - 1);
        redrawRoute();
    }

    /** 清空所有路点（连同地图上的连线与图钉） */
    private void clearRoutePoints() {
        mRoutePoints.clear();
        redrawRoute();
    }

    /**
     * 画一组“路点图钉 + 连线”的公共实现。
     *
     * <p>路线选点和点击移动的画法完全一样，只差数据源、图层对象、标题文案和
     * 画完之后要刷新哪条操作条，所以这四样参数化，避免两份一模一样的循环
     * 日后改了一处忘了另一处（例如以后要给“队头那个正在走向的点”换个图标）。</p>
     *
     * @param points    路点（WGS-84）
     * @param markers   这组路点当前持有的图钉，会先全部摘掉再按 points 重建
     * @param line      连线图层，可为 null（只画图钉）
     * @param titleRes  图钉标题文案，形如“第 %1$d 个点”
     * @param refresh   画完之后需要刷新的操作条
     */
    private void redrawPoints(List<ScriptWaypoint> points, List<Marker> markers,
                              Polyline line, int titleRes, Runnable refresh) {
        if (mMapView == null) {
            return;
        }

        for (Marker marker : markers) {
            marker.closeInfoWindow();
            mMapView.getOverlays().remove(marker);
        }
        markers.clear();

        List<GeoPoint> geoPoints = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            ScriptWaypoint point = points.get(i);
            GeoPoint geoPoint = new GeoPoint(point.lat, point.lng);
            geoPoints.add(geoPoint);

            Marker marker = createMarkMarker();
            if (marker != null) {
                marker.setPosition(geoPoint);
                marker.setTitle(getResources().getString(titleRes, i + 1));
                addRouteOverlay(marker);
                markers.add(marker);
            }
        }

        if (line != null) {
            line.setPoints(geoPoints);
        }

        mMapView.invalidate();
        refresh.run();
    }

    /** 重画路线：连线 + 每个路点一个图钉 */
    private void redrawRoute() {
        redrawPoints(mRoutePoints, mRouteMarkers, mRouteLine,
                R.string.route_point_title, this::updateRouteUi);
    }

    /** 刷新操作条：已选点数 + 主按钮是“开始移动”还是“停止移动”，以及暂停 / 继续按钮的状态 */
    private void updateRouteUi() {
        if (mRouteHint != null) {
            int count = mRoutePoints.size();
            mRouteHint.setText(count == 0
                    ? getResources().getString(R.string.route_hint_empty)
                    : getResources().getString(R.string.route_hint_count, count));
        }

        if (mRouteGoButton != null) {
            boolean playing = isMapRoutePlaying();
            // 图标跟着文案一起切：开始 = 移动箭头，停止 = 叉。
            // 只在 XML 里写死一个图标的话，文案变成“停止移动”时图标还指着移动，会误导
            mRouteGoButton.setText(playing ? R.string.route_stop : R.string.route_go);
            mRouteGoButton.setIconResource(playing ? R.drawable.ic_close : R.drawable.ic_move);
        }

        if (mRoutePauseButton != null) {
            // 暂停按钮只在这条路线跑着时出现；暂停中换成“继续”（图标 暂停 ⇄ 播放）
            boolean active = isMapRoutePlaying();
            mRoutePauseButton.setVisibility(active ? View.VISIBLE : View.GONE);
            if (active) {
                mRoutePauseButton.setText(mScriptPaused ? R.string.route_resume : R.string.route_pause);
                mRoutePauseButton.setIconResource(mScriptPaused ? R.drawable.ic_play : R.drawable.ic_pause);
            }
        }
    }

    /**
     * 路线操作条上的“暂停 / 继续”切换。
     *
     * <p>暂停 = 位置停在原地、进度保留；继续 = 从暂停处原地接着走（不会瞬移、不会跳段）。
     * 服务还没绑上时先记住请求，连上后在 {@link #syncStateWithService()} 里补执行，
     * 避免“点了一下没反应”。</p>
     */
    private void toggleMapRoutePause() {
        if (mServiceBinder == null) {
            mPendingPauseRequest = !mScriptPaused;
            bindServiceIfNeeded();
            return;
        }

        try {
            if (mScriptPaused) {
                boolean ok = mServiceBinder.resumeScript();
                if (ok) {
                    mScriptPaused = false;
                }
                GoUtils.DisplayToast(this, getResources().getString(
                        ok ? R.string.script_resumed_toast : R.string.script_resume_fail));
            } else {
                boolean ok = mServiceBinder.pauseScript();
                if (ok) {
                    mScriptPaused = true;
                }
                GoUtils.DisplayToast(this, getResources().getString(
                        ok ? R.string.script_paused_toast : R.string.script_pause_fail));
            }
            updateRouteUi();
        } catch (Exception e) {
            XLog.e("ERROR: toggleMapRoutePause", e);
        }
    }

    /** 当前正在跑的路线是不是主界面选的这条（含被编辑后另存的替换路线） */
    private boolean isMapRoutePlaying() {
        if (mRunningScriptId == null) {
            return false;
        }

        String preferredId = sharedPreferences.getString(KEY_MAIN_MAP_ROUTE_ID, MAP_ROUTE_ID);
        return mRunningScriptId.equals(preferredId) || MAP_ROUTE_ID.equals(mRunningScriptId);
    }

    /**
     * 播放中切换移动状态；没在播放时静默返回（下拉的取值已记在 mRouteMode 里，
     * 等开始移动时作为新路点的默认状态）。
     */
    private void applyLiveMode(ScriptWaypoint.Mode mode) {
        if (mRunningScriptId == null || mode == null) {
            return;
        }

        if (mServiceBinder == null) {
            // 脚本是在脚本模式页面启动的，这里还没绑上；补绑一次，稍后切换才会生效
            bindServiceIfNeeded();
            return;
        }

        try {
            boolean ok = mServiceBinder.switchLiveMode(mode);
            GoUtils.DisplayToast(this, getResources().getString(
                    ok ? R.string.mode_switched : R.string.mode_switch_fail, mode.label));
        } catch (Exception e) {
            XLog.e("ERROR: switchLiveMode", e);
        }
    }

    /** 把当前选点存成一条脚本路线，并让 ServiceGo 沿路线平滑移动 */
    private void startMapRoute() {
        if (mRoutePoints.size() < 2) {
            GoUtils.DisplayToast(this, getResources().getString(R.string.route_need_two));
            return;
        }

        if (!GoUtils.isNetworkAvailable(this)) {
            GoUtils.DisplayToast(this, getResources().getString(R.string.app_error_network));
            return;
        }
        if (!GoUtils.isGpsOpened(this)) {
            GoUtils.showEnableGpsDialog(this);
            return;
        }
        if (!Settings.canDrawOverlays(getApplicationContext())) {
            GoUtils.showEnableFloatWindowDialog(this);
            return;
        }
        if (!GoUtils.isAllowMockLocation(this)) {
            GoUtils.showEnableMockLocationDialog(this);
            return;
        }

        ScriptRoute route = buildMapRoute();
        new ScriptStore(this).save(route);

        ScriptWaypoint first = route.points.get(0);
        Intent intent = new Intent(MainActivity.this, ServiceGo.class);
        intent.putExtra(SCRIPT_ROUTE_ID, route.id);
        intent.putExtra(LNG_MSG_ID, first.lng);
        intent.putExtra(LAT_MSG_ID, first.lat);
        intent.putExtra(ALT_MSG_ID, first.alt);

        try {
            mPendingStop = false;
            if (mServiceBinder == null) {
                // 先记下待启动的脚本：连接回调里会用 binder 兜底启动一次，
                // 不再只依赖 onStartCommand 异步送达（送达失败 = 点开始不动）
                mPendingScriptId = route.id;
                bindServiceIfNeeded();
            }
            startForegroundService(intent);
            mRunningScriptId = route.id;
            mScriptPaused = false;
            updateScriptButton();
            // 服务已经在跑时立刻校验一次：服务端没在播就用 binder 补启动
            ensureScriptPlaying(route.id);
            // 服务可能正在（重）建测试提供者：延迟再补一次，让定位图层重新挂上监听
            if (mMapView != null) {
                mMapView.postDelayed(this::ensureMyLocationEnabled, 600);
            }
            GoUtils.DisplayToast(this, getResources().getString(R.string.route_started,
                    route.points.size(), ScriptParser.formatDistance(ScriptParser.totalDistance(route))));
        } catch (Exception e) {
            XLog.e("ERROR: startMapRoute", e);
            GoUtils.DisplayToast(this, getResources().getString(R.string.app_error_service));
        }
    }

    /** 停止沿路线移动（当前位置保持不变） */
    private void stopMapRoute() {
        mPendingScriptId = null;    // 还没落地的“开始”作废

        if (mServiceBinder != null) {
            try {
                mServiceBinder.stopScript();
            } catch (Exception e) {
                XLog.e("ERROR: stopMapRoute", e);
            }
        } else {
            // 先把持久状态清掉：以前这一步只靠服务端 stopScript，
            // 服务没绑上时走 stopService，存储里的运行 id 会残留，
            // 下次进来按钮还显示“停止移动”，点“开始”实际执行的是停止
            new ScriptStore(this).setRunningScriptId(null);

            if (mBoundToService) {
                // 已发起绑定、回调还没回来：直接 stopService 停不掉“已绑定”的服务，
                // 记下来等连上后再补一次停止
                mPendingStop = true;
            } else {
                try {
                    stopService(new Intent(MainActivity.this, ServiceGo.class));
                } catch (Exception e) {
                    XLog.e("ERROR: stopMapRoute");
                }
            }
        }

        mRunningScriptId = null;
        mScriptPaused = false;
        updateScriptButton();
        GoUtils.DisplayToast(this, getResources().getString(R.string.script_stopped));
    }

    /**
     * 校验服务端确实在播放指定脚本；没在播就用 binder 兜底启动一次。
     *
     * <p>“点开始不动”的兜底：不管 onStartCommand 有没有送到，只要服务连上了，
     * 最终一定会真正进入播放状态。启动失败（脚本数据异常等）给明确提示，不再静默。</p>
     *
     * <p>唯一例外是“服务端已经在播同一条脚本”：这时重复启动会让播放器重新走一遍
     * {@code findAnchor}，用当前模拟位置把轨迹重新锚定一次，位置会莫名跳一下，
     * 这正是“切一次应用脚本就失灵”的来源。所以这里按“播放器双确认 + id 相同”跳过：
     * 同一条真的在播就不重启（不会漏掉“停止 → 再开始”的重启，那时已经不在播），
     * 换一条脚本则照常切过去，不会出现“换了脚本还在跑旧路线”。</p>
     */
    private void ensureScriptPlaying(String scriptId) {
        if (mServiceBinder == null || TextUtils.isEmpty(scriptId)) {
            return;
        }

        try {
            // 服务端“真的”在播同一条脚本时才跳过启动：重复 startScript 会让播放器
            // 重新走一遍 findAnchor，用当前模拟位置把轨迹重新锚定一次，
            // 表现就是切一次应用 / 切一次页面脚本就跳一下甚至失灵。
            //
            // 这里必须同时满足两个条件：
            //   1) isScriptPlaying() —— 状态 + 播放器双确认。只看 getRunningScriptId()
            //      会被“状态停在 PLAYING 但播放器早就停了”的残留骗到，误跳过启动；
            //   2) id 相同 —— 换一条脚本必须立刻切过去，否则会一直跑旧路线。
            boolean playingSame = mServiceBinder.isScriptPlaying()
                    && scriptId.equals(mServiceBinder.getRunningScriptId());
            if (playingSame) {
                // 暂停中又点了一次“开始”：当作继续播放，而不是重新对齐轨迹
                if (mServiceBinder.isScriptPaused() && mServiceBinder.resumeScript()) {
                    mScriptPaused = false;
                    updateRouteUi();
                }
                mRunningScriptId = scriptId;
                return;
            }

            boolean ok = mServiceBinder.startScript(scriptId);
            if (ok) {
                mRunningScriptId = scriptId;
                mScriptPaused = false;
            } else {
                GoUtils.DisplayToast(this, getResources().getString(R.string.script_start_fail));
            }
        } catch (Exception e) {
            XLog.e("ERROR: ensureScriptPlaying", e);
        }
    }

    /** 服务连上后把挂起的开始 / 停止请求落地，并用服务端状态校准本地缓存 */
    private void syncStateWithService() {
        if (mServiceBinder == null) {
            return;
        }

        try {
            // 挂起的“暂停 / 继续”请求连上后先落地（点按钮时服务还没绑上）
            if (mPendingPauseRequest != null) {
                boolean wantPause = mPendingPauseRequest;
                mPendingPauseRequest = null;
                if (wantPause) {
                    mServiceBinder.pauseScript();
                } else {
                    mServiceBinder.resumeScript();
                }
            }

            if (mPendingStop) {
                mPendingStop = false;
                mServiceBinder.stopScript();
                mRunningScriptId = null;
            } else if (mPendingScriptId != null) {
                String pending = mPendingScriptId;
                mPendingScriptId = null;
                ensureScriptPlaying(pending);
            } else {
                // 常规同步：以服务端为准（本地可能是服务销毁前留下的过期状态）
                mRunningScriptId = mServiceBinder.getRunningScriptId();
                if (mRunningScriptId == null) {
                    // 服务端没在播放，存储里的运行记录也一定是过期的，一并清掉，
                    // 否则每次 onResume 都会被它带回“停止移动”的错误状态
                    new ScriptStore(MainActivity.this).setRunningScriptId(null);
                }
            }

            // 暂停状态同样以服务端为准（暂停可能是在通知栏 / 脚本页发起的）
            mScriptPaused = mServiceBinder.isScriptPaused();
        } catch (Exception e) {
            XLog.e("ERROR: syncStateWithService", e);
        }
    }

    /**
     * 把主界面选的点整理成一条脚本路线。
     *
     * <p>同时按脚本格式生成文本：一是让 ServiceGo 重新解析文本后依旧得到这些 WGS-84 坐标
     * （文本按“脚本模式记住的坐标系”输出，见 {@link ScriptParser#KEY_SCRIPT_FROM_BD09}），
     * 二是让这条路线在脚本模式里能直接看到和编辑。</p>
     */
    private ScriptRoute buildMapRoute() {
        // 上一次“主界面选点”生成的路线 id；它被手动编辑过就不再覆盖，另存后记住新的 id
        String preferredId = sharedPreferences.getString(KEY_MAIN_MAP_ROUTE_ID, MAP_ROUTE_ID);
        ScriptStore store = new ScriptStore(this);
        ScriptRoute existing = store.load(preferredId);

        ScriptRoute route = new ScriptRoute();
        if (existing != null && existing.edited) {
            // 这条路线已经被用户在脚本界面改过，不能再覆盖它：本次选点另存为一条新脚本，
            // 并记住新 id，后续选点继续覆盖这条新的
            sharedPreferences.edit().putString(KEY_MAIN_MAP_ROUTE_ID, route.id).apply();
        } else {
            route.id = preferredId;
        }
        route.name = getResources().getString(R.string.route_default_name);
        route.endMode = ScriptRoute.END_STOP;
        route.points = new ArrayList<>(mRoutePoints);

        // 这条路线由主界面选点生成，坐标本身就是 WGS-84，并把坐标系记在脚本自己身上，
        // 之后不管全局单选怎么变，重新解析 route.text 都不会偏移
        boolean fromBd09 = false;
        route.fromBd09 = Boolean.FALSE;
        StringBuilder text = new StringBuilder();
        for (ScriptWaypoint point : route.points) {
            text.append(ScriptParser.formatPoint(point, fromBd09)).append('\n');
        }
        route.text = text.toString();

        return route;
    }

    /** “设置”里某个状态对应的速度（米/秒） */
    private double getModeSpeed(ScriptWaypoint.Mode mode) {
        double[] speeds = getModeSpeeds();
        return speeds[Math.max(0, Math.min(speeds.length - 1, mode.ordinal()))];
    }

    /** “设置”里的步行 / 跑步 / 骑行速度，顺序与 {@link ScriptWaypoint.Mode} 一致 */
    private double[] getModeSpeeds() {
        return ScriptParser.modeSpeeds(
                getDoubleSetting("setting_walk", 1.2),
                getDoubleSetting("setting_run", 3.6),
                getDoubleSetting("setting_bike", 10.0));
    }

    /** 读取浮点型设置项，非法输入时退回默认值 */
    private double getDoubleSetting(String key, double defaultValue) {
        try {
            return Double.parseDouble(sharedPreferences.getString(key, Double.toString(defaultValue)));
        } catch (Exception e) {
            XLog.e("ERROR: invalid setting - " + key);
            return defaultValue;
        }
    }

    /** 读取设置里的海拔高度，非法输入时退回默认值，避免 NumberFormatException 闪退 */
    private double getSettingAltitude() {
        try {
            return Double.parseDouble(sharedPreferences.getString("setting_altitude", "55.0"));
        } catch (Exception e) {
            XLog.e("ERROR: setting_altitude is not a number");
            return 55.0;
        }
    }

    private void bindServiceIfNeeded() {
        if (mBoundToService) {
            return;
        }

        try {
            if (bindService(new Intent(MainActivity.this, ServiceGo.class), mConnection, BIND_AUTO_CREATE)) {
                mBoundToService = true;
            }
        } catch (Exception e) {
            // 绑定失败不能影响“开始移动”：启动走的是 startForegroundService，与绑定无关
            mBoundToService = false;
            XLog.e("ERROR: bindService", e);
        }
    }

    /** 统一解绑，避免退出时因为“服务不是本界面启动的”而漏掉解绑 */
    private void unbindServiceIfNeeded() {
        // 解绑后不会再有连接回调，挂起的开始 / 停止请求无处落地，先作废
        mPendingScriptId = null;
        mPendingStop = false;
        mPendingPauseRequest = null;

        if (!mBoundToService && !isMockServStart) {
            return;
        }

        try {
            if (mServiceBinder != null) {
                mServiceBinder.clearScriptListener();
                mServiceBinder.clearClickMoveListener();
            }
            unbindService(mConnection);
        } catch (Exception e) {
            XLog.e("ERROR: unbindService");
        }
        mBoundToService = false;
        mServiceBinder = null;
        // 解绑后收不到“到达 / 走完”回调，界面按“没在走”处理，避免按钮一直停在“停止行走”
        mClickMoveRunning = false;
    }

    private void startGoLocation() {
        if (mMarkLatLngMap == null) {
            GoUtils.DisplayToast(this, getResources().getString(R.string.app_error_location));
            return;
        }

        Intent serviceGoIntent = new Intent(MainActivity.this, ServiceGo.class);
        double[] latLng = MapUtils.bd2wgs(mMarkLatLngMap.getLongitude(), mMarkLatLngMap.getLatitude());
        serviceGoIntent.putExtra(LNG_MSG_ID, latLng[0]);
        serviceGoIntent.putExtra(LAT_MSG_ID, latLng[1]);
        serviceGoIntent.putExtra(ALT_MSG_ID, getSettingAltitude());

        try {
            if (!mBoundToService) {
                // 已经绑定过就不要再 bind：同一个 ServiceConnection 重复绑定会直接抛异常，
                // 把后面的 startForegroundService 一并跳过（表现为点传送 / 开始没反应）
                bindService(serviceGoIntent, mConnection, BIND_AUTO_CREATE);    // 绑定服务和活动，之后活动就可以去调服务的方法了
                mBoundToService = true;
            }
            startForegroundService(serviceGoIntent);
            XLog.d("startForegroundService: ServiceGo");

            isMockServStart = true;
            refreshClickMoveSwitch();
        } catch (Exception e) {
            XLog.e("ERROR: startGoLocation", e);
            GoUtils.DisplayToast(this, getResources().getString(R.string.app_error_service));
        }
    }

    private void stopGoLocation() {
        unbindServiceIfNeeded();
        Intent serviceGoIntent = new Intent(MainActivity.this, ServiceGo.class);
        stopService(serviceGoIntent);
        isMockServStart = false;
        refreshClickMoveSwitch();
    }

    /*============================== 点击移动 ⇄ 摇杆移动 开关 ==============================*/

    /** 初始化左下角开关：默认状态由“移动类型”设置决定（选“点击移动”时默认开） */
    private void initClickMoveSwitch() {
        mClickMoveSwitch = findViewById(R.id.click_move_switch);
        if (mClickMoveSwitch == null) {
            return;
        }

        mClickMoveEnabled = "2".equals(sharedPreferences.getString("setting_joystick_type", "0"));
        mClickMoveSwitch.setText(mClickMoveEnabled
                ? R.string.move_mode_click : R.string.move_mode_joystick);
        mClickMoveSwitch.setOnClickListener(v -> {
            mClickMoveEnabled = !mClickMoveEnabled;
            // 内部会推给服务端并刷新“目标点操作条”（切到点击移动才显示，切回摇杆就收起）
            applyClickMoveMode();
            if (mServiceBinder == null) {
                // 状态要推给服务端；连接回来时 onServiceConnected 还会再补推一次，
                // 否则切换只生效在本地，点地图会一直被服务端拒绝
                bindServiceIfNeeded();
            }
        });

        refreshClickMoveSwitch();
    }

    /** 把开关状态推给服务端（没连上时连接回调会再推一次）并刷新按钮文案 */
    private void applyClickMoveMode() {
        if (mClickMoveSwitch != null) {
            mClickMoveSwitch.setText(mClickMoveEnabled
                    ? R.string.move_mode_click : R.string.move_mode_joystick);
        }

        if (mServiceBinder != null) {
            try {
                mServiceBinder.setClickMoveMode(mClickMoveEnabled);
            } catch (Exception e) {
                XLog.e("ERROR: setClickMoveMode", e);
            }
        }

        // 开关状态决定“目标点操作条”是否出现（切到点击移动才显示，切回摇杆就收起）
        refreshClickMoveSwitch();
        updateClickMoveUi();
    }

    /** 开关只在模拟位置（单点 / 脚本）运行期间显示，结束后消失 */
    private void refreshClickMoveSwitch() {
        if (mClickMoveSwitch == null) {
            return;
        }

        // 服务连着 = 还在注入位置（脚本跑完后位置冻结但服务仍在），点击移动仍可用
        boolean running = isMockServStart || mRunningScriptId != null || mServiceBinder != null;
        mClickMoveSwitch.setVisibility(running ? View.VISIBLE : View.GONE);

        // 点击移动开启时显示目标点操作条（撤回 / 清空 / 开始行走），与路线选点一致
        if (mClickMoveBar != null) {
            boolean showBar = running && mClickMoveEnabled && !mRoutePicking;
            mClickMoveBar.setVisibility(showBar ? View.VISIBLE : View.GONE);
        }
    }

    /** 初始化点击移动的目标点操作条（点击地图连续添加目标时的撤回 / 清空 / 开始行走） */
    private void initClickMoveBar() {
        mClickMoveBar = findViewById(R.id.click_move_bar);
        mClickMoveHint = findViewById(R.id.click_move_hint);
        mClickMoveGoButton = findViewById(R.id.click_move_go);

        View undo = findViewById(R.id.click_move_undo);
        if (undo != null) {
            undo.setOnClickListener(v -> undoClickMovePoint());
        }
        View clear = findViewById(R.id.click_move_clear);
        if (clear != null) {
            clear.setOnClickListener(v -> clearClickMovePoints());
        }
        if (mClickMoveGoButton != null) {
            mClickMoveGoButton.setOnClickListener(v -> {
                if (mClickMoveRunning) {
                    // 停止行走：让服务端放弃当前这一段。目标点仍留在队列里，
                    // 之后点“开始行走”会从队头接着走
                    cancelServiceClickTarget();
                    updateClickMoveUi();
                    GoUtils.DisplayToast(this, getResources().getString(R.string.click_move_stop));
                } else if (mClickMovePoints.isEmpty()) {
                    GoUtils.DisplayToast(this, getResources().getString(R.string.click_move_empty));
                } else {
                    // 重新开始行走：从第一个还没走到的目标接着走
                    dispatchNextClickTarget();
                }
            });
        }

        // 轨迹连线图层：与脚本路线的连线画法一致（虚线区分“点击移动的临时目标”），
        // 两条线各自持有图层对象，避免互相覆盖
        if (mMapView != null) {
            mClickMoveLine = new Polyline(mMapView);
            mClickMoveLine.getOutlinePaint().setColor(ContextCompat.getColor(this, R.color.colorAccent));
            mClickMoveLine.getOutlinePaint().setStrokeWidth(6.0f);
            mClickMoveLine.getOutlinePaint().setPathEffect(
                    new DashPathEffect(new float[] {18.0f, 12.0f}, 0.0f));
            addRouteOverlay(mClickMoveLine);
        }

        updateClickMoveUi();
    }

    /**
     * 主界面地图单击的“点击移动”处理。
     *
     * <p>每点一次追加一个目标点：地图上立刻出现图钉与连线，
     * 服务端按队列逐个走过去（走完一个自动接下一个）。</p>
     *
     * @return true 表示本次点击已按点击移动处理（不再走原来的选中标记逻辑）
     */
    private boolean handleClickMoveTap(GeoPoint p) {
        if (mClickMoveSwitch == null || mClickMoveSwitch.getVisibility() != View.VISIBLE
                || !mClickMoveEnabled) {
            return false;
        }

        if (isScriptPlaying()) {
            GoUtils.DisplayToast(this, getResources().getString(R.string.click_move_script_busy));
            return true;
        }

        if (mServiceBinder == null) {
            // 服务连接还没回来：补绑一次，连上后 onServiceConnected 会把开关状态推过去
            bindServiceIfNeeded();
            GoUtils.DisplayToast(this, getResources().getString(R.string.app_error_service));
            return true;
        }

        // 点地图 = 往目标队列末尾追加一个点；还没开始走时，第一个点会立刻开走
        addClickMovePoint(p.getLongitude(), p.getLatitude());
        return true;
    }

    /*============================== 点击移动：目标队列 / 轨迹 ==============================*/

    /** 追加一个点击移动目标点，并在地图上画出图钉与连线 */
    private void addClickMovePoint(double lngWgs84, double latWgs84) {
        mClickMovePoints.add(new ScriptWaypoint(lngWgs84, latWgs84, getSettingAltitude(),
                mRouteMode, getModeSpeed(mRouteMode), 0));
        redrawClickMove();

        // 还没开始走：立刻把队首目标下发，服务端随即起步
        if (!mClickMoveRunning) {
            dispatchNextClickTarget();
        }
    }

    /**
     * 把队头目标下发给服务端。
     *
     * <p>队列里只存待走目标，所以队头（{@code get(0)}）就是下一个要走的点：
     * 走到后由 {@link #onServiceClickArrived} 从队头删掉，再回到这里发下一个。
     * 这样即使用户在行走途中继续点地图，也只是往队尾追加，不会打断当前这一段。</p>
     */
    private void dispatchNextClickTarget() {
        if (mServiceBinder == null) {
            return;
        }

        if (mClickMovePoints.isEmpty()) {
            // 没有下一个目标了：行进结束
            mClickMoveRunning = false;
            updateClickMoveUi();
            return;
        }

        ScriptWaypoint target = mClickMovePoints.get(0);
        try {
            if (mServiceBinder.appendClickTarget(target.lng, target.lat)) {
                mClickMoveRunning = true;
            } else {
                // 服务端拒绝（脚本抢走了位置或开关没开）：不要卡在“假运行”状态
                mClickMoveRunning = false;
            }
        } catch (Exception e) {
            XLog.e("ERROR: appendClickTarget", e);
            mClickMoveRunning = false;
        }

        updateClickMoveUi();
    }

    /** 服务端到达队头目标：把它删掉（走过的不再显示），再接着走下一个（没有就收工） */
    private void onServiceClickArrived() {
        runOnUiThread(() -> {
            if (!mClickMovePoints.isEmpty()) {
                mClickMovePoints.remove(0);
            }

            if (mClickMovePoints.isEmpty()) {
                // 走完了：队列空了，行进结束。先改状态再重画，按钮才能正确显示“开始行走”
                mClickMoveRunning = false;
                redrawClickMove();
                GoUtils.DisplayToast(MainActivity.this,
                        getResources().getString(R.string.click_move_finished));
                return;
            }

            // 还有下一个：先把已走过的点从地图上摘掉，再把新的队头发下去
            redrawClickMove();
            dispatchNextClickTarget();
        });
    }

    /**
     * 让服务端放弃当前目标，并把本地运行标记复位。
     *
     * <p>撤回、清空、停止行走三处都要做同一件事，统一走这里，
     * 免得哪处漏了 {@code clearClickTargets()}，位置就继续朝着已删除的点走。</p>
     */
    private void cancelServiceClickTarget() {
        mClickMoveRunning = false;

        if (mServiceBinder != null) {
            try {
                mServiceBinder.clearClickTargets();
            } catch (Exception e) {
                XLog.e("ERROR: clearClickTargets", e);
            }
        }
    }

    /** 撤回最后一个还没走到的目标（走过的点已随到达被删掉，这里只处理待走队列） */
    private void undoClickMovePoint() {
        if (mClickMovePoints.isEmpty()) {
            GoUtils.DisplayToast(this, getResources().getString(R.string.click_move_empty));
            return;
        }

        mClickMovePoints.remove(mClickMovePoints.size() - 1);

        // 只有“队里就剩这一个”时，删的才是服务端正在走向的队头，
        // 此时必须连服务端的目标一起撤掉，否则位置会继续朝着已删除的点走
        if (mClickMovePoints.isEmpty() && mClickMoveRunning) {
            cancelServiceClickTarget();
        }

        redrawClickMove();
        // 显式刷新：redrawPoints 在地图已销毁时会提前返回，不能指望它替我们刷新按钮
        updateClickMoveUi();
    }

    /** 清空目标点与轨迹（正在走向的目标也一并取消） */
    private void clearClickMovePoints() {
        mClickMovePoints.clear();
        cancelServiceClickTarget();
        redrawClickMove();
        updateClickMoveUi();
    }

    /** 重画点击移动的图钉与连线（虚线，和脚本路线的实线区分开） */
    private void redrawClickMove() {
        redrawPoints(mClickMovePoints, mClickMoveMarkers, mClickMoveLine,
                R.string.click_move_point_title, this::updateClickMoveUi);
    }

    /**
     * 刷新点击移动相关的按钮与提示文案。
     *
     * <p>点击移动模式下点地图连续添加目标，这条操作条负责撤回 / 清空 / 开始行走，
     * 与脚本路线的选点操作方式保持一致。</p>
     */
    private void updateClickMoveUi() {
        if (mClickMoveHint != null) {
            int count = mClickMovePoints.size();
            mClickMoveHint.setText(count == 0
                    ? getResources().getString(R.string.click_move_hint_empty)
                    : getResources().getString(R.string.click_move_hint_count, count));
        }

        if (mClickMoveGoButton != null) {
            // 同 updateRouteUi：图标与文案一起在“开始 / 停止”之间切换
            boolean running = mClickMoveRunning;
            mClickMoveGoButton.setText(running
                    ? R.string.click_move_stop : R.string.click_move_go);
            mClickMoveGoButton.setIconResource(running
                    ? R.drawable.ic_close : R.drawable.ic_move);
        }
    }

    private void doGoLocation(View v) {
        if (!GoUtils.isNetworkAvailable(this)) {
            GoUtils.DisplayToast(this, getResources().getString(R.string.app_error_network));
            return;
        }

        if (!GoUtils.isGpsOpened(this)) {
            GoUtils.showEnableGpsDialog(this);
            return;
        }

        if (!Settings.canDrawOverlays(getApplicationContext())) {//悬浮窗权限判断
            GoUtils.showEnableFloatWindowDialog(this);
            XLog.e("无悬浮窗权限!");
            return;
        }

        // 脚本正在播放：传送 / 终止模拟都会打断路线移动。以前这里是“静默打断”，
        // 播放中误碰一下按钮，位置就跳回标记点、脚本也被顺手停掉。
        // 现在先弹确认：取消则什么都不动，脚本继续走。
        if (isScriptPlaying()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.script_stop_confirm_title)
                    .setMessage(R.string.script_stop_confirm_message)
                    .setPositiveButton(R.string.script_stop_confirm_ok,
                            (dialog, which) -> doGoLocationAction(v))
                    .setNegativeButton(R.string.input_position_cancel, null)
                    .show();
            return;
        }

        doGoLocationAction(v);
    }

    /** 脚本是否正在播放或暂停中（服务没绑定时以持久状态为准，跑完时服务端会把它清掉） */
    private boolean isScriptPlaying() {
        if (mServiceBinder != null) {
            int state = mServiceBinder.getScriptState();
            // 暂停也算“脚本占着位置”：传送 / 终止同样会打断它，先弹确认
            return state == ServiceGo.SCRIPT_STATE_PLAYING
                    || state == ServiceGo.SCRIPT_STATE_PAUSED;
        }
        return new ScriptStore(this).getRunningScriptId() != null;
    }

    /** 真正执行“传送 / 终止模拟”（doGoLocation 前置检查与脚本确认之后的动作） */
    private void doGoLocationAction(View v) {
        if (isMockServStart) {
            if (mMarkLatLngMap == null) {
                stopGoLocation();
                Snackbar.make(v, "模拟位置已终止", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                mButtonStart.setImageResource(R.drawable.ic_position);
            } else {
                if (mServiceBinder == null) {
                    // 服务还没连接上（例如进程刚被系统重启），重新绑定，避免空指针闪退
                    bindServiceIfNeeded();
                    GoUtils.DisplayToast(this, getResources().getString(R.string.app_error_service));
                    return;
                }

                double[] latLng = MapUtils.bd2wgs(mMarkLatLngMap.getLongitude(), mMarkLatLngMap.getLatitude());
                double alt = getSettingAltitude();
                mServiceBinder.setPosition(latLng[0], latLng[1], alt);
                Snackbar.make(v, "已传送到新位置", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();

                recordCurrentLocation(mMarkLatLngMap.getLongitude(), mMarkLatLngMap.getLatitude());

                clearMark(mMapView, mMarkMarker);
                mMarkLatLngMap = null;

                if (GoUtils.isWifiEnabled(MainActivity.this)) {
                    GoUtils.showDisableWifiDialog(MainActivity.this);
                }
            }
        } else {
            if (!GoUtils.isAllowMockLocation(this)) {
                GoUtils.showEnableMockLocationDialog(this);
                XLog.e("无模拟位置权限!");
            } else {
                if (mMarkLatLngMap == null) {
                    Snackbar.make(v, "请先点击地图位置或者搜索位置", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                } else {
                    startGoLocation();
                    mButtonStart.setImageResource(R.drawable.ic_fly);
                    Snackbar.make(v, "模拟位置已启动", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();

                    recordCurrentLocation(mMarkLatLngMap.getLongitude(), mMarkLatLngMap.getLatitude());
                    clearMark(mMapView, mMarkMarker);
                    mMarkLatLngMap = null;

                    if (GoUtils.isWifiEnabled(MainActivity.this)) {
                        GoUtils.showDisableWifiDialog(MainActivity.this);
                    }
                }
            }
        }
    }

    /*============================== 历史记录 相关 ==============================*/
    private void initStoreHistory() {
        try {
            // 定位历史
            DataBaseHistoryLocation dbLocation = new DataBaseHistoryLocation(getApplicationContext());
            mLocationHistoryDB = dbLocation.getWritableDatabase();
            // 搜索历史
            DataBaseHistorySearch dbHistory = new DataBaseHistorySearch(getApplicationContext());
            mSearchHistoryDB = dbHistory.getWritableDatabase();
        } catch (Exception e) {
            XLog.e("ERROR: sqlite init error");
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
                searchHistoryItem.put(DataBaseHistorySearch.DB_COLUMN_LONGITUDE_CUSTOM, cursor.getString(7));
                searchHistoryItem.put(DataBaseHistorySearch.DB_COLUMN_LATITUDE_CUSTOM, cursor.getString(8));
                data.add(searchHistoryItem);
            }
            cursor.close();
        } catch (Exception e) {
            XLog.e("ERROR: getSearchHistory");
        }

        return data;
    }

    // 记录请求的位置信息
    private void recordCurrentLocation(double lng, double lat) {
        //参数坐标系：bd09
        final double[] latLng = MapUtils.bd2wgs(lng, lat);
        // 逆地理编码改为 OpenStreetMap Nominatim，传入 WGS-84 坐标
        mGeoCoder.reverse(latLng[1], latLng[0], poi -> {
            try {
                String address = (poi == null || poi.address.isEmpty()) ? null : poi.address;
                if (address == null) {
                    address = mMarkName != null ? mMarkName
                            : getResources().getString(R.string.history_location_default_name);
                }

                //插表参数
                ContentValues contentValues = new ContentValues();
                contentValues.put(DataBaseHistoryLocation.DB_COLUMN_LOCATION, address);
                contentValues.put(DataBaseHistoryLocation.DB_COLUMN_LONGITUDE_WGS84, String.valueOf(latLng[0]));
                contentValues.put(DataBaseHistoryLocation.DB_COLUMN_LATITUDE_WGS84, String.valueOf(latLng[1]));
                contentValues.put(DataBaseHistoryLocation.DB_COLUMN_TIMESTAMP, System.currentTimeMillis() / 1000);
                contentValues.put(DataBaseHistoryLocation.DB_COLUMN_LONGITUDE_CUSTOM, Double.toString(lng));
                contentValues.put(DataBaseHistoryLocation.DB_COLUMN_LATITUDE_CUSTOM, Double.toString(lat));

                DataBaseHistoryLocation.saveHistoryLocation(mLocationHistoryDB, contentValues);
            } catch (Exception e) {
                XLog.e("ERROR: recordCurrentLocation");
            }
        });
    }

    /*============================== SearchView 相关 ==============================*/
    private void initSearchView() {
        mSearchLayout = findViewById(R.id.search_linear);
        mHistoryLayout = findViewById(R.id.search_history_linear);

        mSearchList = findViewById(R.id.search_list_view);
        mSearchList.setOnItemClickListener((parent, view, position, id) -> {
            String lng = ((TextView) view.findViewById(R.id.poi_longitude)).getText().toString();
            String lat = ((TextView) view.findViewById(R.id.poi_latitude)).getText().toString();
            mMarkName = ((TextView) view.findViewById(R.id.poi_name)).getText().toString();
            mMarkLatLngMap = new GeoPoint(Double.parseDouble(lat), Double.parseDouble(lng));
            mMapView.getController().setCenter(
                    bd09ToWgs84(mMarkLatLngMap.getLongitude(), mMarkLatLngMap.getLatitude()));

            markMap();

            double[] latLng = MapUtils.bd2wgs(mMarkLatLngMap.getLongitude(), mMarkLatLngMap.getLatitude());

            // mSearchList.setVisibility(View.GONE);
            //搜索历史 插表参数
            ContentValues contentValues = new ContentValues();
            contentValues.put(DataBaseHistorySearch.DB_COLUMN_KEY, mMarkName);
            contentValues.put(DataBaseHistorySearch.DB_COLUMN_DESCRIPTION, ((TextView) view.findViewById(R.id.poi_address)).getText().toString());
            contentValues.put(DataBaseHistorySearch.DB_COLUMN_IS_LOCATION, DataBaseHistorySearch.DB_SEARCH_TYPE_RESULT);
            contentValues.put(DataBaseHistorySearch.DB_COLUMN_LONGITUDE_CUSTOM, lng);
            contentValues.put(DataBaseHistorySearch.DB_COLUMN_LATITUDE_CUSTOM, lat);
            contentValues.put(DataBaseHistorySearch.DB_COLUMN_LONGITUDE_WGS84, String.valueOf(latLng[0]));
            contentValues.put(DataBaseHistorySearch.DB_COLUMN_LATITUDE_WGS84, String.valueOf(latLng[1]));
            contentValues.put(DataBaseHistorySearch.DB_COLUMN_TIMESTAMP, System.currentTimeMillis() / 1000);

            DataBaseHistorySearch.saveHistorySearch(mSearchHistoryDB, contentValues);
            mSearchLayout.setVisibility(View.INVISIBLE);
            searchItem.collapseActionView();
        });
        //搜索历史列表的点击监听
        mSearchHistoryList = findViewById(R.id.search_history_list_view);
        mSearchHistoryList.setOnItemClickListener((parent, view, position, id) -> {
            String searchDescription = ((TextView) view.findViewById(R.id.search_description)).getText().toString();
            String searchKey = ((TextView) view.findViewById(R.id.search_key)).getText().toString();
            String searchIsLoc = ((TextView) view.findViewById(R.id.search_isLoc)).getText().toString();

            //如果是定位搜索
            if (searchIsLoc.equals("1")) {
                String lng = ((TextView) view.findViewById(R.id.search_longitude)).getText().toString();
                String lat = ((TextView) view.findViewById(R.id.search_latitude)).getText().toString();
                // mMarkName = ((TextView) view.findViewById(R.id.poi_name)).getText().toString();
                mMarkLatLngMap = new GeoPoint(Double.parseDouble(lat), Double.parseDouble(lng));
                mMapView.getController().setCenter(
                        bd09ToWgs84(mMarkLatLngMap.getLongitude(), mMarkLatLngMap.getLatitude()));

                markMap();

                double[] latLng = MapUtils.bd2wgs(mMarkLatLngMap.getLongitude(), mMarkLatLngMap.getLatitude());

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
                contentValues.put(DataBaseHistorySearch.DB_COLUMN_LONGITUDE_WGS84, String.valueOf(latLng[0]));
                contentValues.put(DataBaseHistorySearch.DB_COLUMN_LATITUDE_WGS84, String.valueOf(latLng[1]));
                contentValues.put(DataBaseHistorySearch.DB_COLUMN_TIMESTAMP, System.currentTimeMillis() / 1000);

                DataBaseHistorySearch.saveHistorySearch(mSearchHistoryDB, contentValues);
            } else if (searchIsLoc.equals("0")) { //如果仅仅是搜索
                try {
                    searchView.setQuery(searchKey, true);
                } catch (Exception e) {
                    GoUtils.DisplayToast(this, getResources().getString(R.string.app_error_search));
                    XLog.e(getResources().getString(R.string.app_error_search));
                }
            } else {
                XLog.e(getResources().getString(R.string.app_error_param));
            }
        });
        mSearchHistoryList.setOnItemLongClickListener((parent, view, position, id) -> {
            new MaterialAlertDialogBuilder(MainActivity.this)
                    .setTitle("警告")//这里是表头的内容
                    .setMessage("确定要删除该项搜索记录吗?")//这里是中间显示的具体信息
                    .setPositiveButton("确定",(dialog, which) -> {
                        String searchKey = ((TextView) view.findViewById(R.id.search_key)).getText().toString();

                        try {
                            mSearchHistoryDB.delete(DataBaseHistorySearch.TABLE_NAME, DataBaseHistorySearch.DB_COLUMN_KEY + " = ?", new String[] {searchKey});
                            //删除成功
                            //展示搜索历史
                            List<Map<String, Object>> data = getSearchHistory();

                            if (!data.isEmpty()) {
                                SimpleAdapter simAdapt = new SimpleAdapter(
                                        MainActivity.this,
                                        data,
                                        R.layout.search_item,
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
                            XLog.e("ERROR: delete database error");
                            GoUtils.DisplayToast(MainActivity.this,getResources().getString(R.string.history_delete_error));
                        }
                    })
                    .setNegativeButton("取消",
                            (dialog, which) -> {
                            })
                    .show();
            return true;
        });
    }

    /**
     * 关键字检索（OpenStreetMap Nominatim）。
     * 结果统一转换成 BD-09 后再交给列表，列表点击选点的下游逻辑因此完全不用改。
     */
    private void doSearch(String keyword) {
        mGeoCoder.search(keyword, pois -> {
            if (pois == null || pois.isEmpty()) {
                GoUtils.DisplayToast(this, getResources().getString(R.string.app_search_null));
                return;
            }

            SimpleAdapter simAdapt = new SimpleAdapter(
                    MainActivity.this,
                    getMapList(pois),
                    R.layout.search_poi_item,
                    new String[] {POI_NAME, POI_ADDRESS, POI_LONGITUDE, POI_LATITUDE}, // 与下面数组元素要一一对应
                    new int[] {R.id.poi_name, R.id.poi_address, R.id.poi_longitude, R.id.poi_latitude});
            mSearchList.setAdapter(simAdapt);
            mSearchLayout.setVisibility(View.VISIBLE);
        });
    }

    @NonNull
    private static List<Map<String, Object>> getMapList(List<OsmGeocoder.Poi> pois) {
        List<Map<String, Object>> data = new ArrayList<>();

        for (OsmGeocoder.Poi poi : pois) {
            // 列表里的经纬度沿用原有 BD-09 语义
            double[] bd09 = MapUtils.wgs2bd09(poi.longitude, poi.latitude);

            Map<String, Object> poiItem = new HashMap<>();
            poiItem.put(POI_NAME, poi.name);
            poiItem.put(POI_ADDRESS, poi.address);
            poiItem.put(POI_LONGITUDE, "" + bd09[0]);
            poiItem.put(POI_LATITUDE, "" + bd09[1]);
            data.add(poiItem);
        }
        return data;
    }

}
