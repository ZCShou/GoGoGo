package com.zcshou.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.location.provider.ProviderProperties;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import com.elvishew.xlog.XLog;
import com.zcshou.gogogo.MainActivity;
import com.zcshou.gogogo.R;
import com.zcshou.joystick.JoyStick;
import com.zcshou.script.ScriptParser;
import com.zcshou.script.ScriptPlayer;
import com.zcshou.script.ScriptRoute;
import com.zcshou.script.ScriptStore;
import com.zcshou.script.ScriptWaypoint;
import com.zcshou.utils.GoUtils;

public class ServiceGo extends Service {
    // 定位相关变量
    public static final double DEFAULT_LAT = 36.667662;
    public static final double DEFAULT_LNG = 117.027707;
    public static final double DEFAULT_ALT = 55.0D;
    public static final float DEFAULT_BEA = 0.0F;
    /* 下面几个位置字段会同时被主线程（绑定调用 / 摇杆）和定位线程（脚本推进）读写，统一加 volatile */
    private volatile double mCurLat = DEFAULT_LAT;
    private volatile double mCurLng = DEFAULT_LNG;
    private volatile double mCurAlt = DEFAULT_ALT;
    private volatile float mCurBea = DEFAULT_BEA;
    private volatile double mSpeed = 1.2;        /* 默认的速度，单位 m/s */
    /*
     * 真实定位与模拟位置是两条独立的链路：真实定位只经由 applyRealLocation() 进来，
     * 且只在“没有任何模拟位置在接管”时才允许写入 mCurLat / mCurLng。
     * 以前两者共用这两个字段，导致“切一次应用脚本就失灵、位置回到真实位置”。
     */
    private static final int HANDLER_MSG_ID = 0;
    private static final String SERVICE_GO_HANDLER_NAME = "ServiceGoLocation";
    /* 服务被系统重新拉起（intent 为 null）时，用这里保存的上次位置恢复，避免拿不到位置 */
    private static final String KEY_LAST_LNG = "service_last_lng";
    private static final String KEY_LAST_LAT = "service_last_lat";
    private static final String KEY_LAST_ALT = "service_last_alt";
    /** 服务被系统重新拉起时，用来恢复正在播放的脚本 */
    private static final String KEY_LAST_SCRIPT_ID = "service_last_script_id";
    /* 脚本播放状态 */
    public static final int SCRIPT_STATE_IDLE = 0;
    public static final int SCRIPT_STATE_PLAYING = 1;
    public static final int SCRIPT_STATE_FINISHED = 2;
    /** 暂停中：位置停在原地、进度保留，随时可以继续播放 */
    public static final int SCRIPT_STATE_PAUSED = 3;
    private volatile int mScriptState = SCRIPT_STATE_IDLE;
    private LocationManager mLocManager;
    private HandlerThread mLocHandlerThread;
    private Handler mLocHandler;
    private boolean isStop = false;
    // 通知栏消息
    private static final int SERVICE_GO_NOTE_ID = 1;
    private static final String SERVICE_GO_NOTE_ACTION_JOYSTICK_SHOW = "ShowJoyStick";
    private static final String SERVICE_GO_NOTE_ACTION_JOYSTICK_HIDE = "HideJoyStick";
    /** 通知栏“暂停 / 继续”动作（脚本播放中才挂这个按钮） */
    private static final String SERVICE_GO_NOTE_ACTION_SCRIPT_PAUSE = "PauseScript";
    private static final String SERVICE_GO_NOTE_ACTION_SCRIPT_RESUME = "ResumeScript";
    private static final String SERVICE_GO_NOTE_CHANNEL_ID = "SERVICE_GO_NOTE";
    private NoteActionReceiver mActReceiver;
    /** 通知栏管理器：状态变化时刷新文案与“暂停 / 继续”按钮 */
    private NotificationManager mNoteManager;
    // 摇杆相关
    private JoyStick mJoyStick;
    // 脚本相关
    private ScriptPlayer mScriptPlayer;
    private ScriptRoute mScriptRoute;
    private ScriptStore mScriptStore;
    private ScriptListener mScriptListener;
    private ClickMoveListener mClickMoveListener;

    /* 点击移动：主界面左下角开关控制，点地图朝目标点按“行走方式”持续走 */
    /** true = 点击移动生效（false = 摇杆移动）；开启时摇杆方向输入暂停 */
    private volatile boolean mClickMoveEnabled;
    /** 有一个未到达的行走目标 */
    private volatile boolean mHasClickTarget;
    /** 点击行走是否暂停中：位置停在原地，目标保留，继续时朝同一目标接着走 */
    private volatile boolean mClickMovePaused;
    private volatile double mClickTargetLng;
    private volatile double mClickTargetLat;
    /** 上一次朝目标推进的时刻（毫秒），按真实间隔步进 */
    private long mLastClickStepMs;

    /** 脚本播放状态回调，供界面展示“第几个点 / 当前状态” */
    public interface ScriptListener {
        void onScriptSegment(ScriptRoute route, int index, ScriptWaypoint.Mode mode, double speed);

        void onScriptFinish(ScriptRoute route);

        void onScriptStopped();

        /** 进入暂停（位置停在原地、进度保留），界面据此把“暂停 / 继续”切到另一侧 */
        void onScriptPaused();

        /** 从暂停处继续播放 */
        void onScriptResumed();
    }

    /**
     * 点击移动“到达一个目标”的回调。
     *
     * <p>目标点列表由主界面维护（可以连着点好几个），服务端只负责“走到下一个”，
     * 到站后通过本回调通知界面，由界面决定是否继续下发后续目标、或者宣告走完。
     * 回调在定位线程触发，实现方需要切主线程再碰界面。</p>
     */
    public interface ClickMoveListener {
        /** 到达一个目标点（经纬度为 WGS-84），界面据此决定是否继续下发下一个 */
        void onClickTargetArrived(double lng, double lat);
    }

    private final ServiceGoBinder mBinder = new ServiceGoBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        XLog.i("SERVICEGO: onCreate");

        mLocManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        mScriptStore = new ScriptStore(this);

        removeTestProviderNetwork();
        addTestProviderNetwork();

        removeTestProviderGPS();
        addTestProviderGPS();

        initGoLocation();

        initNotification();

        initJoyStick();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        double lng = DEFAULT_LNG;
        double lat = DEFAULT_LAT;
        double alt = DEFAULT_ALT;
        String scriptId = null;

        if (intent != null) {
            lng = intent.getDoubleExtra(MainActivity.LNG_MSG_ID, DEFAULT_LNG);
            lat = intent.getDoubleExtra(MainActivity.LAT_MSG_ID, DEFAULT_LAT);
            alt = intent.getDoubleExtra(MainActivity.ALT_MSG_ID, DEFAULT_ALT);
            scriptId = intent.getStringExtra(MainActivity.SCRIPT_ROUTE_ID);
        } else {
            // 服务被系统重新拉起时 intent 为 null（原实现在这里会直接 NPE 闪退），
            // 这里改用上次保存的位置，保证服务能正常恢复
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            lng = parseDouble(preferences.getString(KEY_LAST_LNG, null), DEFAULT_LNG);
            lat = parseDouble(preferences.getString(KEY_LAST_LAT, null), DEFAULT_LAT);
            alt = parseDouble(preferences.getString(KEY_LAST_ALT, null), DEFAULT_ALT);
            scriptId = preferences.getString(KEY_LAST_SCRIPT_ID, null);
            XLog.i("SERVICEGO: restart with null intent");
        }

        mCurLng = lng;
        mCurLat = lat;
        mCurAlt = alt;
        // 顺序很重要：先把 mCur 写好，再保存。
        // 这里刚刚从 intent / 上次位置恢复坐标，此时“脚本还没开始、服务已经活着”，
        // 若 saveLastPosition 内部按“服务活着 = 模拟接管”提前 return，恢复用的坐标就存不下来了
        saveLastPosition();

        if (mJoyStick != null) {
            try {
                mJoyStick.setCurrentPosition(mCurLng, mCurLat, mCurAlt);
            } catch (Exception e) {
                XLog.e("SERVICEGO: ERROR - setCurrentPosition", e);
            }
        }

        // 脚本模式：按脚本预设自动移动；没有脚本 id 时保持原来的“定点点位”行为
        if (!isBlank(scriptId)) {
            startScript(scriptId);
        } else if (intent != null) {
            // 手动传送（带坐标、不带脚本 id）会打断正在播放的脚本：
            // 否则脚本线程会继续覆盖位置，界面却提示“已传送到新位置”，两边打架
            if (mScriptPlayer != null && mScriptPlayer.isPlaying()) {
                XLog.i("SERVICEGO: manual teleport stops running script");
                stopScript();
            }
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        isStop = true;

        if (mScriptPlayer != null) {
            mScriptPlayer.stop();
            mScriptPlayer = null;
        }
        mScriptRoute = null;
        mScriptState = SCRIPT_STATE_IDLE;
        mScriptListener = null;

        // 服务销毁 = 脚本播放彻底结束，把“正在播放”的持久状态一并清掉。
        // 之前只在 stopScript() 里清理，而“没绑定上服务时点停止”走的是 stopService() 路径，
        // 存储里的运行 id 会残留：下次 onResume 读到过期状态，界面把“开始移动”显示成
        // “停止移动”，点开始又走成了停止，表现为“停止后再开始不移动”。
        // 系统杀进程（SIGKILL）不会走到 onDestroy，服务重启恢复脚本的逻辑不受影响。
        try {
            if (mScriptStore != null) {
                mScriptStore.setRunningScriptId(null);
            }
            PreferenceManager.getDefaultSharedPreferences(this).edit()
                    .remove(KEY_LAST_SCRIPT_ID)
                    .apply();
        } catch (Exception e) {
            XLog.e("SERVICEGO: ERROR - clear script state on destroy");
        }

        try {
            if (mLocHandler != null) {
                mLocHandler.removeMessages(HANDLER_MSG_ID);
            }
            if (mLocHandlerThread != null) {
                mLocHandlerThread.quit();
            }
        } catch (Exception e) {
            XLog.e("SERVICEGO: ERROR - stop location thread", e);
        }
        mLocHandler = null;
        mLocHandlerThread = null;

        if (mJoyStick != null) {
            try {
                mJoyStick.destroy();
            } catch (Throwable e) {
                XLog.e("SERVICEGO: ERROR - destroy joystick", e);
            }
            mJoyStick = null;
        }

        removeTestProviderNetwork();
        removeTestProviderGPS();

        if (mActReceiver != null) {
            try {
                unregisterReceiver(mActReceiver);
            } catch (Exception e) {
                XLog.e("SERVICEGO: ERROR - unregisterReceiver", e);
            }
            mActReceiver = null;
        }

        try {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } catch (Exception e) {
            XLog.e("SERVICEGO: ERROR - stopForeground", e);
        }

        super.onDestroy();
    }

    private void initNotification() {
        try {
            mActReceiver = new NoteActionReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(SERVICE_GO_NOTE_ACTION_JOYSTICK_SHOW);
            filter.addAction(SERVICE_GO_NOTE_ACTION_JOYSTICK_HIDE);
            filter.addAction(SERVICE_GO_NOTE_ACTION_SCRIPT_PAUSE);
            filter.addAction(SERVICE_GO_NOTE_ACTION_SCRIPT_RESUME);
            registerReceiver(mActReceiver, filter);

            // 渠道名会原样显示在系统“通知设置”里，必须是给用户看的文字
            String channelName = getResources().getString(R.string.app_note_channel_name);
            NotificationChannel mChannel = new NotificationChannel(SERVICE_GO_NOTE_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_DEFAULT);
            mNoteManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            if (mNoteManager != null) {
                mNoteManager.createNotificationChannel(mChannel);
            }

            startForeground(SERVICE_GO_NOTE_ID, buildNotification());
        } catch (Throwable e) {
            // 前台服务起不来时，系统会在 5 秒后直接杀掉进程，这里主动结束服务更安全
            XLog.e("SERVICEGO: ERROR - initNotification", e);
            stopSelf();
        }
    }

    /**
     * 按当前脚本状态构建通知。
     *
     * <p>脚本播放中会多挂一个“暂停”按钮，暂停后同一个按钮变成“继续”、
     * 文案也换成“已暂停”——通知栏是脚本在后台跑时唯一够得着的控制入口。</p>
     */
    private Notification buildNotification() {
        //准备intent（广播都限定在本应用内，避免同名 action 被别的应用抢先接收）
        Intent clickIntent = new Intent(this, MainActivity.class);
        PendingIntent clickPI = PendingIntent.getActivity(this, 1, clickIntent, PendingIntent.FLAG_IMMUTABLE);
        Intent showIntent = new Intent(SERVICE_GO_NOTE_ACTION_JOYSTICK_SHOW).setPackage(getPackageName());
        PendingIntent showPendingPI = PendingIntent.getBroadcast(this, 0, showIntent, PendingIntent.FLAG_IMMUTABLE);
        Intent hideIntent = new Intent(SERVICE_GO_NOTE_ACTION_JOYSTICK_HIDE).setPackage(getPackageName());
        PendingIntent hidePendingPI = PendingIntent.getBroadcast(this, 0, hideIntent, PendingIntent.FLAG_IMMUTABLE);

        boolean scriptActive = mScriptState == SCRIPT_STATE_PLAYING || mScriptState == SCRIPT_STATE_PAUSED;
        boolean scriptPaused = mScriptState == SCRIPT_STATE_PAUSED;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, SERVICE_GO_NOTE_CHANNEL_ID)
                .setChannelId(SERVICE_GO_NOTE_CHANNEL_ID)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setContentText(getResources().getString(
                        scriptPaused ? R.string.note_script_paused : R.string.app_service_tips))
                .setContentIntent(clickPI)
                .addAction(new NotificationCompat.Action(R.drawable.ic_position, getResources().getString(R.string.note_show), showPendingPI))
                .addAction(new NotificationCompat.Action(R.drawable.ic_fly, getResources().getString(R.string.note_hide), hidePendingPI))
                // 通知小图标不能再用 R.mipmap.ic_launcher：自适应图标在通知栏只取前景层，
                // 而新图标的前景是透明的（整张图在背景层），会显示成空白。
                // 这里直接用图钉矢量，和原版通知图标的轮廓保持一致。
                .setSmallIcon(R.drawable.ic_launcher_foreground);

        if (scriptActive) {
            // 同一个按钮按状态切换动作与图标：播放中 = 暂停，暂停中 = 继续
            Intent pauseIntent = new Intent(scriptPaused
                    ? SERVICE_GO_NOTE_ACTION_SCRIPT_RESUME : SERVICE_GO_NOTE_ACTION_SCRIPT_PAUSE)
                    .setPackage(getPackageName());
            PendingIntent pausePendingPI = PendingIntent.getBroadcast(this, 2, pauseIntent, PendingIntent.FLAG_IMMUTABLE);
            builder.addAction(new NotificationCompat.Action(
                    scriptPaused ? R.drawable.ic_play : R.drawable.ic_pause,
                    getResources().getString(scriptPaused ? R.string.note_script_resume : R.string.note_script_pause),
                    pausePendingPI));
        }

        return builder.build();
    }

    /** 状态变化后刷新通知（文案 + “暂停 / 继续”按钮），失败只记日志不影响播放 */
    private void refreshNotification() {
        try {
            if (mNoteManager == null) {
                mNoteManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            }
            if (mNoteManager != null) {
                mNoteManager.notify(SERVICE_GO_NOTE_ID, buildNotification());
            }
        } catch (Throwable e) {
            XLog.e("SERVICEGO: ERROR - refreshNotification", e);
        }
    }

    private void initJoyStick() {
        try {
            mJoyStick = new JoyStick(this);
            mJoyStick.setListener(new JoyStick.JoyStickClickListener() {
                @Override
                public void onMoveInfo(double speed, double disLng, double disLat, double angle) {
                    // 位置的两种接管者：脚本播放（否则位移不到 100ms 就被覆盖）、
                    // 或点击移动开启中（点击与摇杆是切换关系，此时摇杆方向输入暂停）
                    if (isScriptDrivingPosition() || mClickMoveEnabled) {
                        return;
                    }

                    mSpeed = speed;
                    // 根据当前的经纬度和距离，计算下一个经纬度
                    // Latitude: 1 deg = 110.574 km // 纬度的每度的距离大约为 110.574km
                    // Longitude: 1 deg = 111.320*cos(latitude) km  // 经度的每度的距离从0km到111km不等
                    // 具体见：http://wp.mlab.tw/?p=2200
                    mCurLng += disLng / (111.320 * Math.cos(Math.abs(mCurLat) * Math.PI / 180));
                    mCurLat += disLat / 110.574;
                    mCurBea = (float) angle;
                }

                @Override
                public void onPositionInfo(double lng, double lat, double alt) {
                    // 脚本正在接管位置：忽略这次选点传送并给出提示。
                    // 以前会直接改位置——表现就是“位置被拉回选点处、脚本移动被打断”
                    if (isScriptDrivingPosition()) {
                        GoUtils.DisplayToast(ServiceGo.this,
                                getString(R.string.joystick_ignore_teleport));
                        return;
                    }

                    mCurLng = lng;
                    mCurLat = lat;
                    mCurAlt = alt;
                    // 与 binder.setPosition 的手动传送保持一致：速度、方向复位，
                    // 否则“跑完脚本（speed=0）再手动选点”会一直注入 speed=0，上层应用会判定为没在移动
                    mSpeed = 1.2;
                    mCurBea = DEFAULT_BEA;
                    // 传送到了新位置，进行中的点击移动目标作废
                    mHasClickTarget = false;
                    mClickMovePaused = false;
                    saveLastPosition();
                }
            });
            mJoyStick.show();
        } catch (Throwable e) {
            // 悬浮窗（摇杆）创建失败不应该让整个进程崩溃：模拟位置本身仍然可用
            mJoyStick = null;
            XLog.e("SERVICEGO: ERROR - initJoyStick", e);
        }
    }

    private void initGoLocation() {
        if (mLocManager == null) {
            XLog.e("SERVICEGO: ERROR - LocationManager is null");
            return;
        }

        // 创建 HandlerThread 实例，第一个参数是线程的名字
        mLocHandlerThread = new HandlerThread(SERVICE_GO_HANDLER_NAME, Process.THREAD_PRIORITY_FOREGROUND);
        // 启动 HandlerThread 线程
        mLocHandlerThread.start();
        // Handler 对象与 HandlerThread 的 Looper 对象的绑定
        mLocHandler = new Handler(mLocHandlerThread.getLooper()) {
            // 这里的Handler对象可以看作是绑定在HandlerThread子线程中，所以handlerMessage里的操作是在子线程中运行的
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (isStop) {
                    return;
                }

                try {
                    Thread.sleep(100);

                    if (!isStop) {
                        // 顺序：先把位置推进到“本拍”，再注入。
                        //
                        // 以前点击移动排在注入前、脚本 onTick 排在注入后，
                        // 结果脚本每轮注入的都是上一拍算出的坐标，恒定滞后一个 tick（约 100ms），
                        // 而点击移动不滞后——两者表现不一致。现在统一“先推进、后注入”。
                        //
                        // 两者互斥：appendClickTarget 在脚本播放中会直接拒绝，
                        // startScript 也会作废进行中的点击目标，所以用 else 表达这个互斥。
                        if (isScriptDrivingPosition()) {
                            mScriptPlayer.onTick();
                        } else {
                            advanceClickTarget();
                        }

                        setLocationNetwork();
                        setLocationGPS();
                    }
                } catch (InterruptedException e) {
                    XLog.e("SERVICEGO: ERROR - handleMessage", e);
                    Thread.currentThread().interrupt();
                    return;
                } catch (Throwable e) {
                    // 注入模拟位置失败不能让进程崩溃，下一轮继续重试
                    XLog.e("SERVICEGO: ERROR - handleMessage", e);
                }

                if (!isStop) {
                    sendEmptyMessage(HANDLER_MSG_ID);
                }
            }
        };

        mLocHandler.sendEmptyMessage(HANDLER_MSG_ID);
    }

    private static double parseDouble(String value, double defaultValue) {
        if (value == null) {
            return defaultValue;
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * 注入用的朝向：统一到 0~360 并避开 0。
     *
     * <p>Location.setBearing(0) 会把“有朝向”标记清掉（hasBearing() 变成 false），
     * 地图定位图层就会改画小人而不是方向箭头；脚本到达终点段的 bearing(点,点)、
     * 服务默认朝向都会算出 0，这里统一挡掉，保证人物箭头始终显示。</p>
     */
    private float injectionBearing() {
        float bearing = (mCurBea + 360.0f) % 360.0f;
        return bearing == 0.0f ? 0.01f : bearing;
    }

    /*============================== 点击移动 ==============================*/

    /** 切换“点击移动 ⇄ 摇杆移动”（主界面左下角开关）；切换即作废进行中的行走目标 */
    private void setClickMoveMode(boolean enabled) {
        mClickMoveEnabled = enabled;
        mHasClickTarget = false;
        // 目标都作废了，“暂停中”自然也无从谈起，一并复位避免残留
        mClickMovePaused = false;

        if (mJoyStick != null) {
            try {
                // 收起 / 恢复方向摇杆（点击与摇杆为切换关系，行走方式按钮保留）
                mJoyStick.setClickMoveEnabled(enabled);
            } catch (Exception e) {
                XLog.e("SERVICEGO: ERROR - setClickMoveEnabled", e);
            }
        }
    }

    /**
     * 暂停点击行走：位置停在原地，当前目标与朝向都保留。
     *
     * <p>暂停只是“不再朝目标推进”，与“停止行走”（放弃当前目标）不同：
     * 继续时从原地朝同一个目标接着走。同时把注入速度记为 0，
     * 避免上层应用看到“位置不动却还在移动”。</p>
     *
     * @return 是否进入暂停（没在朝目标走时返回 false）
     */
    public boolean pauseClickMove() {
        if (!mClickMoveEnabled || !mHasClickTarget) {
            return false;
        }
        if (mClickMovePaused) {
            return true;
        }

        mClickMovePaused = true;
        mSpeed = 0;

        // 刚好在这一瞬间走到了目标（目标已作废）：没有可暂停的行走。
        // 回退暂停标记，避免留下“暂停中但没有目标”的残留状态
        if (!mHasClickTarget) {
            mClickMovePaused = false;
            return false;
        }
        return true;
    }

    /** 继续点击行走，从暂停处朝同一目标接着走；没有暂停中的行走时返回 false */
    public boolean resumeClickMove() {
        if (!mClickMovePaused) {
            return false;
        }

        mClickMovePaused = false;
        return true;
    }

    /** 点击行走是否处于暂停中（界面据此把“暂停 / 继续”按钮切到正确一侧） */
    public boolean isClickMovePaused() {
        return mClickMovePaused;
    }

    /**
     * 点击移动当前使用的速度（米/秒）：跟随悬浮摇杆上的走 / 跑 / 骑选择。
     *
     * <p>悬浮窗创建失败时 mJoyStick 为空，这里回落到设置里的步行速度，
     * 否则点击移动会直接不动。</p>
     */
    private double clickMoveSpeed() {
        double speed = mJoyStick != null ? mJoyStick.getCurrentSpeed() : 0;
        if (speed <= 0) {
            speed = getModeSpeeds()[0];
        }
        return speed <= 0 ? 1.2D : speed;
    }

    /**
     * 点击移动：沿线朝当前目标持续前进；到站后由界面接上下一个目标。
     *
     * <p>目标由主界面维护（可以连着点好几个），这里只负责“走到它”，
     * 到达后回调 {@link ClickMoveListener#onClickTargetArrived}，
     * 由界面决定把下一个点发下来还是宣告走完。这样即使用户在行走途中
     * 继续点地图，也只是往队列尾部追加，不会打断当前这一段。</p>
     */
    private void advanceClickTarget() {
        if (!mClickMoveEnabled || !mHasClickTarget) {
            return;
        }
        // 播放中与暂停中都由脚本占有位置：暂停只是“不推进”，点击移动同样不能插队
        if (isScriptDrivingPosition()) {
            return;
        }

        long now = System.currentTimeMillis();
        long last = mLastClickStepMs;
        mLastClickStepMs = now;

        // 暂停中：位置停在原地，只把步进计时往前挪——否则恢复那一拍会按“暂停的时长”
        // 算 dt，一步跨出去老远（骑行能一次跳 10 米）。目标与朝向都保留，
        // 继续时从原地朝同一个目标接着走
        if (mClickMovePaused) {
            return;
        }

        double dt = Math.min(1.0D, Math.max(0.0D, (now - last) / 1000.0D));
        if (dt <= 0) {
            return;
        }

        double speed = clickMoveSpeed();
        double step = speed * dt;

        double distance = ScriptPlayer.distanceMeters(mCurLng, mCurLat, mClickTargetLng, mClickTargetLat);
        float bearing = bearingBetween(mCurLng, mCurLat, mClickTargetLng, mClickTargetLat);
        mCurBea = bearing;
        mSpeed = speed;

        boolean arrived = distance <= Math.max(step, 0.05D);
        if (arrived) {
            // 到达：落点精确对齐目标
            mCurLng = mClickTargetLng;
            mCurLat = mClickTargetLat;
            mHasClickTarget = false;
            // 没有目标可暂停了（也顺带消掉“暂停与到达同时发生”的竞态残留）
            mClickMovePaused = false;
        } else {
            // 一步的位移很小，平面近似的误差可以忽略
            double ratio = step / distance;
            mCurLng += (mClickTargetLng - mCurLng) * ratio;
            mCurLat += (mClickTargetLat - mCurLat) * ratio;
        }

        if (mJoyStick != null) {
            try {
                mJoyStick.setCurrentPosition(mCurLng, mCurLat, mCurAlt);
            } catch (Exception e) {
                XLog.e("SERVICEGO: ERROR - click setCurrentPosition", e);
            }
        }

        // 到达一个目标：交给界面决定“接着走下一个”还是“走完了”
        if (arrived) {
            ClickMoveListener listener = mClickMoveListener;
            if (listener != null) {
                try {
                    listener.onClickTargetArrived(mCurLng, mCurLat);
                } catch (Exception e) {
                    XLog.e("SERVICEGO: ERROR - onClickTargetArrived", e);
                }
            }
        }
    }

    /**
     * 两点间方位角（0~360，正北为 0）。
     *
     * <p>公式统一走 {@link ScriptParser#bearing(double, double, double, double)}，
     * 这里只保留注入端特有的处理：避开恰好 0 的朝向（见 {@link #injectionBearing()}）。</p>
     */
    private static float bearingBetween(double lng1, double lat1, double lng2, double lat2) {
        float bearing = (float) ScriptParser.bearing(lng1, lat1, lng2, lat2);
        return bearing == 0.0f ? 0.01f : bearing;
    }

    /*============================== 脚本模式 ==============================*/

    /** 创建一个绑定到本服务的脚本播放器（只创建一次） */
    private void initScriptPlayer() {
        mScriptPlayer = new ScriptPlayer();
        mScriptPlayer.setHandler(new ScriptPlayer.Handler() {
            @Override
            public void onScriptStart(ScriptRoute route, double lng, double lat, double alt) {
                XLog.i("SERVICEGO: script start - " + route.name);
            }

            @Override
            public synchronized void onScriptPosition(double lng, double lat, double alt, ScriptWaypoint.Mode mode,
                                         double speed, double bearing) {
                // 注意：这里运行在定位线程上，写的是 volatile 字段，随后由定位循环注入给系统
                mCurLng = lng;
                mCurLat = lat;
                mCurAlt = alt;
                mSpeed = speed;
                mCurBea = (float) bearing;

                if (mJoyStick != null) {
                    try {
                        mJoyStick.setCurrentPosition(lng, lat, alt);
                    } catch (Exception e) {
                        XLog.e("SERVICEGO: ERROR - script setCurrentPosition", e);
                    }
                }
            }

            @Override
            public synchronized void onScriptSegment(ScriptRoute route, int index, ScriptWaypoint.Mode mode, double speed) {
                XLog.i("SERVICEGO: script segment " + index + " - " + mode.key + " " + speed + "m/s");
                if (mScriptListener != null) {
                    mScriptListener.onScriptSegment(route, index, mode, speed);
                }
            }

            @Override
            public synchronized void onScriptFinish(ScriptRoute route) {
                mScriptState = SCRIPT_STATE_FINISHED;
                XLog.i("SERVICEGO: script finished - " + route.name);
                // 跑完只“不再推进”，坐标保持在终点：服务还活着，注入循环会继续维持这个位置
                // （这里若交还真实定位，表现就是“脚本跑完位置突然跳回手机所在地”）。
                // 同时立刻清掉“正在播放”的持久状态与重启恢复标记，否则界面会一直按“播放中”
                // 处理——路线条显示“停止”、要先停一次才能再模拟、点击移动被拒、
                // 单点传送被要求确认，表现为“脚本结束后模拟位置不能立刻实现”
                try {
                    if (mScriptStore != null) {
                        mScriptStore.setRunningScriptId(null);
                    }
                    // 注意：这里在匿名 Handler 内部，this 是匿名对象不是 Service，必须写 ServiceGo.this
                    PreferenceManager.getDefaultSharedPreferences(ServiceGo.this).edit()
                            .remove(KEY_LAST_SCRIPT_ID)
                            .apply();
                } catch (Exception e) {
                    XLog.e("SERVICEGO: ERROR - clear script state on finish");
                }
                // 跑完后通知栏撤掉“暂停”按钮，文案回到默认
                refreshNotification();
                if (mScriptListener != null) {
                    mScriptListener.onScriptFinish(route);
                }
            }

            @Override
            public synchronized void onScriptError(String message) {
                mScriptState = SCRIPT_STATE_IDLE;
                XLog.e("SERVICEGO: script error - " + message);
                refreshNotification();
                if (mScriptListener != null) {
                    mScriptListener.onScriptStopped();
                }
            }
        });
    }

    /** 读取设置里各状态的默认速度，顺序为 步行 / 跑步 / 骑行 */
    private double[] getModeSpeeds() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        double walk = parseDouble(preferences.getString("setting_walk", "1.2"), 1.2);
        double run = parseDouble(preferences.getString("setting_run", "3.6"), 3.6);
        double bike = parseDouble(preferences.getString("setting_bike", "10.0"), 10.0);

        return ScriptParser.modeSpeeds(walk, run, bike);
    }

    private double getSettingAltitude() {
        return parseDouble(
                PreferenceManager.getDefaultSharedPreferences(this).getString("setting_altitude", "55.0"), DEFAULT_ALT);
    }

    /**
     * 脚本此刻是否真正在接管位置：状态与播放器双确认。
     *
     * <p>只看 mScriptState 会被“状态残留”骗到（状态是 PLAYING、播放器其实早已停了），
     * 表现为摇杆怎么推都没反应；双确认后，残留状态既锁不住摇杆，也会在下次启动时被纠正。</p>
     *
     * <p>暂停中也算接管：脚本只是不推进，位置仍归它保持，恢复时才能原地继续。</p>
     */
    private boolean isScriptDrivingPosition() {
        return (mScriptState == SCRIPT_STATE_PLAYING || mScriptState == SCRIPT_STATE_PAUSED)
                && mScriptPlayer != null
                && mScriptPlayer.isPlaying();
    }

    /**
     * 当前是否由“模拟位置”接管坐标。判定刻意做得宽松（宁可多拦，不可漏拦）：
     *
     * <ul>
     *   <li>脚本：状态 + 播放器双确认，状态残留时不会误锁摇杆；</li>
     *   <li>点击移动：目标还没走完就一直算接管；</li>
     *   <li>单点模拟：没有“正在进行中”的标记，就用“服务还活着”兜底——
     *       注入循环一直在跑，真实定位此时绝不能插进来。</li>
     * </ul>
     *
     * <p>只看前两条会漏掉单点模拟（表现：模拟位置刚启动就被真实定位拉回手机所在地），
     * 所以必须带上 {@code !isStop} 这一层。</p>
     */
    private boolean isMockDrivingPosition() {
        if (isScriptDrivingPosition()) {
            return true;
        }
        if (mClickMoveEnabled && mHasClickTarget) {
            return true;
        }
        // 服务活着就认为模拟位置在接管：mLocHandler 一直在循环注入坐标
        return !isStop;
    }

    /**
     * 接收一次真实定位（主界面 / 摇杆从系统定位拿到坐标时回调）。
     *
     * <p>模拟位置在接管时直接丢弃，不写坐标也不打日志——这是每秒都会来好几次的
     * 正常路径，刷日志只会把有用信息淹掉。切换应用时系统会补发一次真实定位，
     * 若这里直接覆盖，就会出现“切一次应用，模拟位置回到手机真实位置、脚本失灵”。</p>
     *
     * <p>反过来说，真的走到“没有模拟在接管却仍收到定位”这条分支才算异常，
     * 这时才记一条日志，方便排查。</p>
     *
     * @return 是否把当前位置也更新了
     */
    public boolean applyRealLocation(double lng, double lat) {
        if (lng == 0 && lat == 0) {
            return false;
        }

        if (isMockDrivingPosition()) {
            // 模拟位置在接管：丢弃真实定位（不写坐标）
            return false;
        }

        XLog.i("SERVICEGO: mock is not driving, adopt real location " + lng + "," + lat);
        mCurLng = lng;
        mCurLat = lat;
        return true;
    }

    /**
     * 开始播放指定 id 的脚本。
     *
     * <p>位置对齐、状态切换、循环都由 {@link ScriptPlayer} 负责，这里只做加载与启动。</p>
     */
    private void startScript(String scriptId) {
        if (mScriptStore == null) {
            mScriptStore = new ScriptStore(this);
        }

        ScriptRoute route = mScriptStore.load(scriptId);
        if (route == null || route.points.isEmpty()) {
            XLog.e("SERVICEGO: script not found - " + scriptId);
            // 加载失败也把状态拉回 IDLE：否则“状态停在 PLAYING 但播放器没在跑”的残留
            // 会同时锁死摇杆（被当成播放中忽略）并让界面的兜底启动误判“已在播”而跳过
            mScriptState = SCRIPT_STATE_IDLE;
            refreshNotification();
            if (mScriptListener != null) {
                mScriptListener.onScriptStopped();
            }
            return;
        }

        // 每次启动都按最新文本重新解析，保证编辑过的脚本立刻生效
        if (!isBlank(route.text)) {
            // 坐标系优先跟着脚本自己走；旧脚本没有记录时才回落到全局设置，
            // 避免用户改一次全局单选就让这条脚本整体偏移几百米
            boolean fromBd09 = route.fromBd09 != null
                    ? route.fromBd09
                    : PreferenceManager.getDefaultSharedPreferences(this)
                    .getBoolean(ScriptParser.KEY_SCRIPT_FROM_BD09, false);
            ScriptParser.ParseResult parseResult = ScriptParser.parse(route.text, fromBd09, getModeSpeeds());
            if (parseResult.isOk()) {
                route.points = parseResult.route.points;
                // 脚本里不写海拔时，用设置里的海拔补齐
                double alt = getSettingAltitude();
                for (ScriptWaypoint point : route.points) {
                    point.alt = alt;
                }
            } else {
                XLog.e("SERVICEGO: script parse failed - " + parseResult.error);
            }
        }

        if (mScriptPlayer == null) {
            initScriptPlayer();
        }

        // 先把测试提供者 / 注入线程准备好，再启动脚本：
        // 否则脚本在算坐标，但没人往系统里注入，表现就是“脚本不好用”
        ensureMockEnvironment();

        mScriptRoute = route;
        mScriptState = SCRIPT_STATE_PLAYING;
        mScriptStore.setRunningScriptId(route.id);
        // 脚本开始接管位置：作废进行中的点击移动目标
        mHasClickTarget = false;
        mClickMovePaused = false;

        PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putString(KEY_LAST_SCRIPT_ID, route.id)
                .apply();

        mScriptPlayer.start(route, mCurLng, mCurLat);

        // 脚本起点也记一份，服务重启后能回到轨迹附近
        saveLastPosition();

        // 通知栏挂上“暂停”按钮
        refreshNotification();
    }

    /** 停止脚本播放，回到手动控制（当前坐标保持不变） */
    public void stopScript() {
        if (mScriptPlayer != null) {
            mScriptPlayer.stop();
        }

        boolean hadScript = mScriptRoute != null || mScriptState != SCRIPT_STATE_IDLE;
        mScriptRoute = null;
        mScriptState = SCRIPT_STATE_IDLE;
        // 当前坐标保持不变（与方法契约一致）：服务还活着，注入循环继续维持这个位置。
        // 这里绝不能把坐标“交还”给真实定位，否则“停止脚本”会变成
        // “位置跳回手机所在地”，那正是要修的现象

        if (mScriptStore != null) {
            mScriptStore.setRunningScriptId(null);
        }

        PreferenceManager.getDefaultSharedPreferences(this).edit()
                .remove(KEY_LAST_SCRIPT_ID)
                .apply();

        // 通知栏撤掉“暂停”按钮、文案回到默认
        refreshNotification();

        if (hadScript && mScriptListener != null) {
            mScriptListener.onScriptStopped();
        }
    }

    /**
     * 暂停脚本播放：位置停在原地，时间轴冻结，进度保留。
     *
     * <p>暂停期间脚本仍占有位置（{@link #isScriptDrivingPosition()} 同样把暂停算作接管），
     * 摇杆 / 传送 / 点击移动保持被拦，恢复播放时才能从暂停处原地接着走。
     * 同时把注入速度记为 0，避免上层应用看到“位置不动却还在移动”。</p>
     *
     * @return 是否进入暂停（没在播放脚本时返回 false）
     */
    public boolean pauseScript() {
        if (mScriptState != SCRIPT_STATE_PLAYING || mScriptPlayer == null || !mScriptPlayer.isPlaying()) {
            return false;
        }

        // 没能真正进入暂停 = 这一拍刚好跑到终点（或已在暂停）：
        // 不能顺手把 FINISHED / 残留状态改写成“暂停中”，否则位置会被永久锁住
        if (!mScriptPlayer.pause() && !mScriptPlayer.isPaused()) {
            return false;
        }

        mScriptState = SCRIPT_STATE_PAUSED;
        mSpeed = 0;
        refreshNotification();
        XLog.i("SERVICEGO: script paused");

        if (mScriptListener != null) {
            mScriptListener.onScriptPaused();
        }
        return true;
    }

    /**
     * 继续播放暂停中的脚本，从暂停的位置原地接着走。
     *
     * @return 是否恢复成功（没有脚本在暂停时返回 false）
     */
    public boolean resumeScript() {
        if (mScriptState != SCRIPT_STATE_PAUSED || mScriptPlayer == null) {
            return false;
        }

        // 恢复前照启动时一样把测试提供者 / 注入线程补齐（暂停期间可能被别的流程收走过）
        ensureMockEnvironment();
        if (!mScriptPlayer.resume()) {
            // 播放器已不在暂停态（例如刚被 stop 过）：把残留的“暂停”状态纠正回 IDLE
            XLog.e("SERVICEGO: resume requested but player is not paused");
            mScriptState = SCRIPT_STATE_IDLE;
            refreshNotification();
            if (mScriptListener != null) {
                mScriptListener.onScriptStopped();
            }
            return false;
        }

        mScriptState = SCRIPT_STATE_PLAYING;
        refreshNotification();
        XLog.i("SERVICEGO: script resumed");

        if (mScriptListener != null) {
            mScriptListener.onScriptResumed();
        }
        return true;
    }

    /**
     * 脚本启动前的准备：确保测试提供者存在，并且后台注入线程正在跑。
     *
     * <p>“进入模拟位置之后再起用脚本就不好用”的最可能原因：单点模拟那条路径
     * 结束时会 stopService / 解绑，服务一旦被系统收走，测试提供者和注入线程
     * 就都没了；此时脚本即使被主界面兜底启动，注入端也不存在。
     * 这里在真正 start 之前自检一次，缺什么补什么。</p>
     */
    private void ensureMockEnvironment() {
        try {
            // 必须最先复位：注入线程的 handleMessage 一进来就检查 isStop，
            // 若这里放最后，新线程的第一条消息可能先看到 true，直接退出循环不再重排，
            // 表现就是“脚本在算坐标，但坐标永远注入不进去”
            isStop = false;

            if (mLocManager == null) {
                mLocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            }

            // 测试提供者可能被别的流程移除过（removeTestProvider 是先删后加），这里补建
            addTestProviderGPS();
            addTestProviderNetwork();

            // 注入线程必须活着，否则脚本算出来的坐标没人往系统里写
            if (mLocHandler == null || mLocHandlerThread == null || !mLocHandlerThread.isAlive()) {
                XLog.i("SERVICEGO: location thread is gone, restart it for script");
                initGoLocation();
            } else {
                mLocHandler.removeMessages(HANDLER_MSG_ID);
                mLocHandler.sendEmptyMessage(HANDLER_MSG_ID);
            }
        } catch (Exception e) {
            XLog.e("SERVICEGO: ERROR - ensureMockEnvironment", e);
        }
    }

    private void saveLastPosition() {
        try {
            // 注：脚本运行期间这里不写盘。保存的坐标是“服务被系统重启后恢复用”的，
            // 若每 tick 都把脚本行进中的坐标写进去，服务重启后会从脚本半途接上，
            // 反而让“重启恢复”变得不可预期；脚本起点在 startScript 里已经存过一次。
            //
            // 这里只判“脚本是否在播”，不要用 isMockDrivingPosition()：后者把
            // “服务活着”也算作接管，会让 onStartCommand 里刚恢复的坐标存不下去。
            if (isScriptDrivingPosition()) {
                return;
            }

            PreferenceManager.getDefaultSharedPreferences(this).edit()
                    .putString(KEY_LAST_LNG, Double.toString(mCurLng))
                    .putString(KEY_LAST_LAT, Double.toString(mCurLat))
                    .putString(KEY_LAST_ALT, Double.toString(mCurAlt))
                    .apply();
        } catch (Exception e) {
            XLog.e("SERVICEGO: ERROR - saveLastPosition", e);
        }
    }

    private void removeTestProviderGPS() {
        if (mLocManager == null) {
            return;
        }

        try {
            if (mLocManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                mLocManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, false);
                mLocManager.removeTestProvider(LocationManager.GPS_PROVIDER);
            }
        } catch (Throwable e) {
            // 某些 ROM 在没有测试提供者时就会抛异常，属于预期内的情况
            XLog.e("SERVICEGO: ERROR - removeTestProviderGPS");
        }
    }

    // 注意下面临时添加 @SuppressLint("wrongconstant") 以处理 addTestProvider 参数值的 lint 错误
    @SuppressLint("wrongconstant")
    private void addTestProviderGPS() {
        if (mLocManager == null) {
            return;
        }

        try {
            // 注意，由于 android api 问题，下面的参数会提示错误(以下参数是通过相关API获取的真实GPS参数，不是随便写的)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mLocManager.addTestProvider(LocationManager.GPS_PROVIDER, false, true, false,
                        false, true, true, true, ProviderProperties.POWER_USAGE_HIGH, ProviderProperties.ACCURACY_FINE);
            } else {
                mLocManager.addTestProvider(LocationManager.GPS_PROVIDER, false, true, false,
                        false, true, true, true, Criteria.POWER_HIGH, Criteria.ACCURACY_FINE);
            }
            if (!mLocManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                mLocManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);
            }
        } catch (Throwable e) {
            XLog.e("SERVICEGO: ERROR - addTestProviderGPS");
        }
    }

    private void setLocationGPS() {
        if (mLocManager == null) {
            return;
        }

        try {
            // 尽可能模拟真实的 GPS 数据
            Location loc = new Location(LocationManager.GPS_PROVIDER);
            loc.setAccuracy(Criteria.ACCURACY_FINE);    // 设定此位置的估计水平精度，以米为单位。
            loc.setAltitude(mCurAlt);                     // 设置高度，在 WGS 84 参考坐标系中的米
            loc.setBearing(injectionBearing());           // 方向（度；避开 0，见 injectionBearing）
            loc.setLatitude(mCurLat);                   // 纬度（度）
            loc.setLongitude(mCurLng);                  // 经度（度）
            loc.setTime(System.currentTimeMillis());    // 本地时间
            loc.setSpeed((float) mSpeed);
            loc.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
            Bundle bundle = new Bundle();
            bundle.putInt("satellites", 7);
            loc.setExtras(bundle);

            mLocManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, loc);
        } catch (Throwable e) {
            XLog.e("SERVICEGO: ERROR - setLocationGPS");
        }
    }

    private void removeTestProviderNetwork() {
        if (mLocManager == null) {
            return;
        }

        try {
            if (mLocManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                mLocManager.setTestProviderEnabled(LocationManager.NETWORK_PROVIDER, false);
                mLocManager.removeTestProvider(LocationManager.NETWORK_PROVIDER);
            }
        } catch (Throwable e) {
            // 某些 ROM 在没有测试提供者时就会抛异常，属于预期内的情况
            XLog.e("SERVICEGO: ERROR - removeTestProviderNetwork");
        }
    }

    // 注意下面临时添加 @SuppressLint("wrongconstant") 以处理 addTestProvider 参数值的 lint 错误
    @SuppressLint("wrongconstant")
    private void addTestProviderNetwork() {
        if (mLocManager == null) {
            return;
        }

        try {
            // 注意，由于 android api 问题，下面的参数会提示错误(以下参数是通过相关API获取的真实NETWORK参数，不是随便写的)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mLocManager.addTestProvider(LocationManager.NETWORK_PROVIDER, true, false,
                        true, true, true, true,
                        true, ProviderProperties.POWER_USAGE_LOW, ProviderProperties.ACCURACY_COARSE);
            } else {
                mLocManager.addTestProvider(LocationManager.NETWORK_PROVIDER, true, false,
                        true, true, true, true,
                        true, Criteria.POWER_LOW, Criteria.ACCURACY_COARSE);
            }
            if (!mLocManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                mLocManager.setTestProviderEnabled(LocationManager.NETWORK_PROVIDER, true);
            }
        } catch (Throwable e) {
            // 这里原来只捕获 SecurityException，一旦 ROM 抛出 IllegalArgumentException
            // 就会直接把整个进程带崩（点“模拟位置”后闪退的元凶之一），统一改为捕获 Throwable
            XLog.e("SERVICEGO: ERROR - addTestProviderNetwork");
        }
    }

    private void setLocationNetwork() {
        if (mLocManager == null) {
            return;
        }

        try {
            // 尽可能模拟真实的 NETWORK 数据
            Location loc = new Location(LocationManager.NETWORK_PROVIDER);
            loc.setAccuracy(Criteria.ACCURACY_COARSE);  // 设定此位置的估计水平精度，以米为单位。
            loc.setAltitude(mCurAlt);                     // 设置高度，在 WGS 84 参考坐标系中的米
            loc.setBearing(injectionBearing());           // 方向（度；避开 0，见 injectionBearing）
            loc.setLatitude(mCurLat);                   // 纬度（度）
            loc.setLongitude(mCurLng);                  // 经度（度）
            loc.setTime(System.currentTimeMillis());    // 本地时间
            loc.setSpeed((float) mSpeed);
            loc.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());

            mLocManager.setTestProviderLocation(LocationManager.NETWORK_PROVIDER, loc);
        } catch (Throwable e) {
            XLog.e("SERVICEGO: ERROR - setLocationNetwork");
        }
    }

    public class NoteActionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals(SERVICE_GO_NOTE_ACTION_JOYSTICK_SHOW)) {
                    if (mJoyStick != null) {
                        mJoyStick.show();
                    }
                }

                if (action.equals(SERVICE_GO_NOTE_ACTION_JOYSTICK_HIDE)) {
                    if (mJoyStick != null) {
                        mJoyStick.hide();
                    }
                }

                // 通知栏“暂停 / 继续”：后台跑脚本时唯一够得着的控制入口
                if (action.equals(SERVICE_GO_NOTE_ACTION_SCRIPT_PAUSE)) {
                    pauseScript();
                }

                if (action.equals(SERVICE_GO_NOTE_ACTION_SCRIPT_RESUME)) {
                    resumeScript();
                }
            }
        }
    }

    public class ServiceGoBinder extends Binder {
        public void setPosition(double lng, double lat, double alt) {
            if (mLocHandler == null) {
                XLog.e("SERVICEGO: ERROR - setPosition before handler ready");
                return;
            }

            // 手动传送会打断脚本播放，也会作废进行中的点击移动目标
            if (mScriptPlayer != null && mScriptPlayer.isPlaying()) {
                stopScript();
            }
            mHasClickTarget = false;
            mClickMovePaused = false;

            mLocHandler.removeMessages(HANDLER_MSG_ID);
            mCurLng = lng;
            mCurLat = lat;
            mCurAlt = alt;
            mSpeed = 1.2;
            mCurBea = DEFAULT_BEA;
            saveLastPosition();
            mLocHandler.sendEmptyMessage(HANDLER_MSG_ID);

            if (mJoyStick != null) {
                mJoyStick.setCurrentPosition(mCurLng, mCurLat, mCurAlt);
            }
        }

        /** 开始播放脚本（重复调用会切换到新的脚本） */
        public boolean startScript(String scriptId) {
            if (isBlank(scriptId)) {
                return false;
            }

            ServiceGo.this.startScript(scriptId);
            return mScriptState == SCRIPT_STATE_PLAYING;
        }

        /** 停止脚本播放 */
        public void stopScript() {
            ServiceGo.this.stopScript();
        }

        /** 暂停脚本播放（位置停在原地，进度保留）；没在播放时返回 false */
        public boolean pauseScript() {
            return ServiceGo.this.pauseScript();
        }

        /** 继续播放暂停中的脚本；没有脚本在暂停时返回 false */
        public boolean resumeScript() {
            return ServiceGo.this.resumeScript();
        }

        /** 脚本是否处于暂停中（用于界面把“暂停 / 继续”按钮切到正确一侧） */
        public boolean isScriptPaused() {
            return mScriptState == SCRIPT_STATE_PAUSED;
        }

        /** 切换“点击移动 ⇄ 摇杆移动”（主界面左下角开关） */
        public void setClickMoveMode(boolean enabled) {
            ServiceGo.this.setClickMoveMode(enabled);
        }

        /** 暂停点击行走（位置停在原地，目标保留）；没在朝目标走时返回 false */
        public boolean pauseClickMove() {
            return ServiceGo.this.pauseClickMove();
        }

        /** 继续点击行走，从暂停处朝同一目标接着走；没有暂停中的行走时返回 false */
        public boolean resumeClickMove() {
            return ServiceGo.this.resumeClickMove();
        }

        /** 点击行走是否处于暂停中 */
        public boolean isClickMovePaused() {
            return mClickMovePaused;
        }

        /** 注册点击移动“到达一个目标”的回调（定位线程触发） */
        public void setClickMoveListener(ClickMoveListener listener) {
            mClickMoveListener = listener;
        }

        public void clearClickMoveListener() {
            mClickMoveListener = null;
        }

        /**
         * 追加一个行进目标：立刻朝它走，走到后回调界面继续下发下一个。
         *
         * <p>与脚本互斥：脚本真正播放中由脚本接管位置，这里直接拒绝。
         * 判定用 {@link ServiceGo#isScriptDrivingPosition()}（状态 + 播放器双确认），
         * 只看 mScriptState 会被“状态停在 PLAYING、播放器早停了”的残留挡掉，
         * 表现为“点地图加目标没反应”。</p>
         *
         * @param lng 目标经度（WGS-84）
         * @param lat 目标纬度（WGS-84）
         * @return 未开启点击移动或脚本正在播放时返回 false
         */
        public boolean appendClickTarget(double lng, double lat) {
            if (!mClickMoveEnabled || isScriptDrivingPosition()) {
                return false;
            }

            mClickTargetLng = lng;
            mClickTargetLat = lat;
            mLastClickStepMs = System.currentTimeMillis();
            mHasClickTarget = true;
            return true;
        }

        /** 清空所有还未到达的目标（撤回 / 清空 / 停止时用） */
        public void clearClickTargets() {
            mHasClickTarget = false;
            mClickMovePaused = false;
        }

        /**
         * 播放过程中切换移动状态（走 / 跑 / 骑），立即对剩余路段生效。
         *
         * @param mode 新的移动状态
         * @return 是否切换成功（没在播放脚本时返回 false）
         */
        public boolean switchLiveMode(ScriptWaypoint.Mode mode) {
            if (mScriptPlayer == null || !mScriptPlayer.isPlaying() || mode == null) {
                return false;
            }

            double[] speeds = getModeSpeeds();
            double speed = speeds[Math.max(0, Math.min(speeds.length - 1, mode.ordinal()))];
            return mScriptPlayer.setLiveMode(mode, speed);
        }

        /** 当前播放中手动切换过的状态，没有切换过或没在播放时返回 null */
        public ScriptWaypoint.Mode getLiveMode() {
            return mScriptPlayer == null ? null : mScriptPlayer.getLiveMode();
        }

        /** 当前正在生效的移动状态（手动切换过返回切换值，否则是脚本当前段的状态） */
        public ScriptWaypoint.Mode getCurrentMode() {
            return mScriptPlayer == null ? null : mScriptPlayer.getMode();
        }

        public int getScriptState() {
            return mScriptState;
        }

        /** 脚本是否真正在接管位置（用于界面避免重复 startScript 重置轨迹） */
        public boolean isScriptPlaying() {
            return isScriptDrivingPosition();
        }

        /**
         * 上报一次真实定位（主界面 / 摇杆从系统定位拿到坐标时调用）。
         *
         * <p>服务内部会判断当前是否由脚本或点击移动接管：接管期间只记录不覆盖，
         * 避免切换应用时真实定位把模拟位置拉回去。</p>
         */
        public void onRealLocation(double lng, double lat) {
            applyRealLocation(lng, lat);
        }

        /** 当前正在播放的脚本 id；没在播放（含已跑完、已停止）时返回 null，暂停中照常返回 */
        public String getRunningScriptId() {
            if ((mScriptState != SCRIPT_STATE_PLAYING && mScriptState != SCRIPT_STATE_PAUSED)
                    || mScriptRoute == null) {
                return null;
            }
            return mScriptRoute.id;
        }

        public String getRunningScriptName() {
            return mScriptRoute == null ? null : mScriptRoute.name;
        }

        public int getRunningScriptPointCount() {
            return mScriptRoute == null ? 0 : mScriptRoute.size();
        }

        public void setScriptListener(ScriptListener listener) {
            mScriptListener = listener;
        }

        public void clearScriptListener() {
            mScriptListener = null;
        }

        /** 把当前位置同步给摇杆（例如脚本运行时界面重新绑定） */
        public void syncJoyStick() {
            if (mJoyStick != null) {
                mJoyStick.setCurrentPosition(mCurLng, mCurLat, mCurAlt);
            }
        }
    }
}

