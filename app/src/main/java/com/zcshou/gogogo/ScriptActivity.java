package com.zcshou.gogogo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.zcshou.script.ScriptParser;
import com.zcshou.script.ScriptRoute;
import com.zcshou.script.ScriptStore;
import com.zcshou.script.ScriptWaypoint;
import com.zcshou.service.ServiceGo;
import com.zcshou.utils.GoUtils;
import com.zcshou.utils.ShareUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 脚本模式：管理脚本并启动“按脚本预设自动移动”的模拟位置。
 *
 * <p>列表里点 ▶ 开始播放（播放后自动退回地图），正在播放的那条按钮变成暂停 / 继续，
 * 播放状态条上也有同样的切换按钮；点条目或铅笔进入编辑页；
 * 删除走工具栏按钮：点一次进入勾选模式，勾选后再次点删除确认。
 * 工具栏还提供脚本的导入 / 导出。</p>
 */
public class ScriptActivity extends BaseActivity {
    /** 编辑页返回时用来判断是否需要刷新 */
    private static final int REQUEST_EDIT = 1001;
    /** 导入脚本选完文件 */
    private static final int REQUEST_IMPORT = 1002;

    private ScriptStore mStore;
    private final List<ScriptRoute> mRoutes = new ArrayList<>();
    private ScriptAdapter mAdapter;

    /** 勾选删除模式 */
    private boolean mSelectMode;
    /** 勾选中的脚本 id */
    private final Set<String> mCheckedIds = new LinkedHashSet<>();

    private ListView mListView;
    private TextView mNoScriptText;

    /* 服务绑定，用于拿到播放状态 */
    private ServiceGo.ServiceGoBinder mServiceBinder;
    private boolean mBound;
    private String mPlayingScriptId;
    /** 刚点过“开始”、但服务连接还没回来时记下的脚本 id，连上后兜底补一次启动 */
    private String mPendingStartId;
    private int mPlayingIndex;
    /** 播放中切换走 / 跑 / 骑的下拉（只在有脚本播放时显示） */
    private Spinner mLiveModeSpinner;
    private View mLiveModeBar;
    /** 操作条上的“暂停 / 继续播放”按钮（跟着播放状态切换文案与图标） */
    private MaterialButton mPauseResumeButton;
    /** 当前脚本是否处于暂停中（由服务端状态回调 / 绑定时同步更新） */
    private volatile boolean mScriptPaused;
    /** 已经点过“暂停 / 继续”、但服务连接还没回来时记下的目标状态，连上后补执行一次 */
    private Boolean mPendingPauseRequest;
    /** 当前生效的移动状态（服务回调 / 绑定时同步过来，用于忽略 setSelection 的回声） */
    private ScriptWaypoint.Mode mEffectiveMode = ScriptWaypoint.Mode.WALK;

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mServiceBinder = (ServiceGo.ServiceGoBinder) service;
            mBound = true;
            mServiceBinder.setScriptListener(mScriptListener);

            if (mPendingStartId != null) {
                // 刚点过“开始播放”：服务端没真的在播就用 binder 补一次，
                // 不再只依赖 onStartCommand 异步送达（没送到 = 点了不动）
                String pending = mPendingStartId;
                mPendingStartId = null;
                ensureScriptPlaying(pending);
            } else {
                mPlayingScriptId = mServiceBinder.getRunningScriptId();
                if (mPlayingScriptId == null) {
                    // 服务端没在播放，存储里的运行记录一定是过期的，一并清掉，
                    // 否则列表会一直显示“正在播放”
                    mStore.setRunningScriptId(null);
                }
            }

            // 挂起的“暂停 / 继续”请求连上后补执行一次（点按钮时服务还没绑上）
            if (mPendingPauseRequest != null) {
                boolean wantPause = mPendingPauseRequest;
                mPendingPauseRequest = null;
                if (wantPause) {
                    mServiceBinder.pauseScript();
                } else {
                    mServiceBinder.resumeScript();
                }
            }
            // 暂停状态以服务端为准（暂停也可能是在通知栏 / 主界面发起的）
            mScriptPaused = mServiceBinder.isScriptPaused();

            ScriptWaypoint.Mode mode = mServiceBinder.getLiveMode();
            if (mode == null) {
                mode = mServiceBinder.getCurrentMode();
            }
            if (mode != null) {
                mEffectiveMode = mode;
            }
            notifyAdapter();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mServiceBinder = null;
            mBound = false;
            notifyAdapter();
        }
    };

    /** 服务端的播放状态变化回调（在定位线程上触发，需要切回主线程刷新界面） */
    private final ServiceGo.ScriptListener mScriptListener = new ServiceGo.ScriptListener() {
        @Override
        public void onScriptSegment(ScriptRoute route, int index, ScriptWaypoint.Mode mode, double speed) {
            mPlayingIndex = index;
            mPlayingScriptId = route == null ? null : route.id;
            if (mode != null) {
                mEffectiveMode = mode;
            }
            notifyAdapter();
        }

        @Override
        public void onScriptFinish(ScriptRoute route) {
            // 跑完即结束：别再让列表行显示“正在播放”
            mPlayingScriptId = null;
            mScriptPaused = false;
            notifyAdapter();
        }

        @Override
        public void onScriptStopped() {
            mPlayingScriptId = null;
            mScriptPaused = false;
            notifyAdapter();
        }

        @Override
        public void onScriptPaused() {
            mScriptPaused = true;
            notifyAdapter();
        }

        @Override
        public void onScriptResumed() {
            mScriptPaused = false;
            notifyAdapter();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_script);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        }

        mStore = new ScriptStore(this);

        mListView = findViewById(R.id.script_list_view);
        mNoScriptText = findViewById(R.id.script_no_textview);

        mAdapter = new ScriptAdapter();
        mListView.setAdapter(mAdapter);

        // 勾选模式下点条目 = 切换勾选；正常模式点条目 = 进编辑页（长按删除已改为工具栏按钮勾选删除）
        mListView.setOnItemClickListener((parent, view, position, id) -> {
            if (mSelectMode) {
                toggleChecked(mRoutes.get(position).id);
            } else {
                openEditor(mRoutes.get(position));
            }
        });

        FloatingActionButton addButton = findViewById(R.id.script_add);
        addButton.setOnClickListener(v -> openEditor(null));

        initLiveModeBar();

        refreshList();
    }

    /** 播放中切换走 / 跑 / 骑的操作条（没有脚本在跑时隐藏） */
    private void initLiveModeBar() {
        mLiveModeBar = findViewById(R.id.script_mode_bar);
        mLiveModeSpinner = findViewById(R.id.script_live_mode);
        mPauseResumeButton = findViewById(R.id.script_pause_resume);
        if (mPauseResumeButton != null) {
            mPauseResumeButton.setOnClickListener(v -> togglePause());
        }
        if (mLiveModeSpinner == null) {
            return;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                new String[] {
                        getResources().getString(R.string.script_mode_walk),
                        getResources().getString(R.string.script_mode_run),
                        getResources().getString(R.string.script_mode_bike)});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mLiveModeSpinner.setAdapter(adapter);

        mLiveModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ScriptWaypoint.Mode mode = modeAt(position);
                // setAdapter / setSelection 触发的回调与当前状态一致，直接忽略
                if (mode == mEffectiveMode || mPlayingScriptId == null) {
                    return;
                }

                boolean ok = mServiceBinder != null && mServiceBinder.switchLiveMode(mode);
                if (ok) {
                    mEffectiveMode = mode;
                    notifyAdapter();
                }
                GoUtils.DisplayToast(ScriptActivity.this, getResources().getString(
                        ok ? R.string.mode_switched : R.string.mode_switch_fail, mode.label));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private static ScriptWaypoint.Mode modeAt(int position) {
        if (position == 1) {
            return ScriptWaypoint.Mode.RUN;
        }
        if (position == 2) {
            return ScriptWaypoint.Mode.BIKE;
        }
        return ScriptWaypoint.Mode.WALK;
    }

    /** 刷新切换条的可见性与选中项（跟随当前播放状态） */
    private void refreshModeBar() {
        if (mLiveModeBar == null || mLiveModeSpinner == null) {
            return;
        }

        boolean playing = mPlayingScriptId != null;
        mLiveModeBar.setVisibility(playing ? View.VISIBLE : View.GONE);
        if (playing) {
            int position = mEffectiveMode.ordinal();
            if (mLiveModeSpinner.getSelectedItemPosition() != position) {
                mLiveModeSpinner.setSelection(position);
            }
        }

        if (mPauseResumeButton != null) {
            // 暂停中按钮变成“继续播放”，图标 暂停 ⇄ 播放
            mPauseResumeButton.setText(mScriptPaused ? R.string.script_resume : R.string.script_pause);
            mPauseResumeButton.setIconResource(mScriptPaused ? R.drawable.ic_play : R.drawable.ic_pause);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // 注意：这里不能无条件 bindService，否则“只是想看看脚本列表”也会把模拟位置服务拉起来
        // （服务会立刻挂上摇杆悬浮窗）。只有确认有脚本在跑时才绑定。
        bindIfScriptRunning();
    }

    /** 有脚本正在播放时绑定服务，用于获取播放进度 */
    private void bindIfScriptRunning() {
        if (mStore.getRunningScriptId() == null) {
            return;
        }

        bindServiceIfNeeded();
    }

    private void bindServiceIfNeeded() {
        if (mBound) {
            return;
        }

        try {
            bindService(new Intent(this, ServiceGo.class), mConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            mBound = false;
        }
    }

    @Override
    protected void onStop() {
        // 解绑后不会再有连接回调，挂起的“补启动”请求无处落地，先作废
        mPendingStartId = null;
        mPendingPauseRequest = null;

        if (mBound && mServiceBinder != null) {
            try {
                mServiceBinder.clearScriptListener();
            } catch (Exception ignored) {
                // 服务已退出时忽略
            }
        }
        if (mBound) {
            try {
                unbindService(mConnection);
            } catch (Exception ignored) {
                // 忽略未绑定异常
            }
            mBound = false;
            mServiceBinder = null;
        }

        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_script_list, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem cancel = menu.findItem(R.id.action_script_select_cancel);
        if (cancel != null) {
            cancel.setVisible(mSelectMode);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        if (item.getItemId() == R.id.action_script_delete) {
            handleDeleteMenu();
            return true;
        }
        if (item.getItemId() == R.id.action_script_select_cancel) {
            exitSelectMode();
            return true;
        }
        if (item.getItemId() == R.id.action_script_export) {
            exportScripts();
            return true;
        }
        if (item.getItemId() == R.id.action_script_import) {
            importScripts();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /*============================== 勾选删除 ==============================*/

    /** 删除按钮：第一次点进入勾选模式，已勾选时弹确认后删除 */
    private void handleDeleteMenu() {
        if (!mSelectMode) {
            mSelectMode = true;
            mCheckedIds.clear();
            notifyAdapter();
            invalidateOptionsMenu();
            GoUtils.DisplayToast(this, getResources().getString(R.string.script_select_hint));
            return;
        }

        if (mCheckedIds.isEmpty()) {
            GoUtils.DisplayToast(this, getResources().getString(R.string.script_select_none));
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.script_delete_title)
                .setMessage(getResources().getString(R.string.script_delete_checked, mCheckedIds.size()))
                .setPositiveButton(R.string.script_delete_title, (dialog, which) -> deleteChecked())
                .setNegativeButton(R.string.input_position_cancel, (dialog, which) -> {
                })
                .show();
    }

    private void deleteChecked() {
        int count = mCheckedIds.size();
        for (String id : mCheckedIds) {
            mStore.delete(id);
            if (id.equals(mPlayingScriptId)) {
                stopPlaying();
            }
        }
        mCheckedIds.clear();
        exitSelectMode();
        refreshList();
        GoUtils.DisplayToast(this, getResources().getString(R.string.script_deleted_n, count));
    }

    private void toggleChecked(String id) {
        if (!mCheckedIds.remove(id)) {
            mCheckedIds.add(id);
        }
        notifyAdapter();
    }

    private void exitSelectMode() {
        mSelectMode = false;
        mCheckedIds.clear();
        notifyAdapter();
        invalidateOptionsMenu();
    }

    /*============================== 导入 / 导出 ==============================*/

    /** 把全部脚本导出成 JSON 文件，并调起系统分享 */
    private void exportScripts() {
        if (mStore.loadAll().isEmpty()) {
            GoUtils.DisplayToast(this, getResources().getString(R.string.script_list_empty));
            return;
        }

        try {
            File dir = new File(getCacheDir(), "scripts");
            if (!dir.exists() && !dir.mkdirs()) {
                throw new IOException("cannot create dir");
            }
            File file = new File(dir, "gogogo_scripts.json");
            try (FileOutputStream out = new FileOutputStream(file)) {
                out.write(mStore.exportJson().getBytes(StandardCharsets.UTF_8));
            }

            ShareUtils.shareFile(this, file, getResources().getString(R.string.script_export));
        } catch (Exception e) {
            GoUtils.DisplayToast(this, getResources().getString(R.string.script_export_fail));
        }
    }

    /** 从系统文件选择器挑一个导出的 JSON 并导入 */
    private void importScripts() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES,
                    new String[] {"application/json", "text/plain", "application/octet-stream"});
            startActivityForResult(Intent.createChooser(intent,
                    getResources().getString(R.string.script_import)), REQUEST_IMPORT);
        } catch (Exception e) {
            GoUtils.DisplayToast(this, getResources().getString(R.string.script_import_fail));
        }
    }

    private void handleImportResult(Intent data) {
        if (data == null || data.getData() == null) {
            return;
        }

        Uri uri = data.getData();
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) {
                GoUtils.DisplayToast(this, getResources().getString(R.string.script_import_fail));
                return;
            }

            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int read;
            while ((read = in.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }

            int count = mStore.importJson(new String(buffer.toByteArray(), StandardCharsets.UTF_8));
            if (count > 0) {
                refreshList();
                GoUtils.DisplayToast(this, getResources().getString(R.string.script_import_ok, count));
            } else {
                GoUtils.DisplayToast(this, getResources().getString(R.string.script_import_fail));
            }
        } catch (Exception e) {
            GoUtils.DisplayToast(this, getResources().getString(R.string.script_import_fail));
        }
    }

    /** 编辑页保存后刷新列表；导入选完文件后解析入库 */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_EDIT) {
            refreshList();
        } else if (requestCode == REQUEST_IMPORT && resultCode == RESULT_OK) {
            handleImportResult(data);
        }
    }

    private void openEditor(ScriptRoute route) {
        Intent intent = new Intent(this, ScriptEditActivity.class);
        if (route != null) {
            intent.putExtra(ScriptEditActivity.EXTRA_ROUTE_ID, route.id);
        }
        startActivityForResult(intent, REQUEST_EDIT);
    }

    private void refreshList() {
        mRoutes.clear();
        mRoutes.addAll(mStore.loadAll());

        // 服务还没绑定回来时，用存储里的运行状态兜底
        if (!mBound) {
            mPlayingScriptId = mStore.getRunningScriptId();
        }

        notifyAdapter();
    }

    private void notifyAdapter() {
        runOnUiThread(() -> {
            if (isFinishing()) {
                return;
            }

            boolean empty = mRoutes.isEmpty();
            mNoScriptText.setVisibility(empty ? View.VISIBLE : View.GONE);
            mListView.setVisibility(empty ? View.GONE : View.VISIBLE);
            refreshModeBar();
            mAdapter.notifyDataSetChanged();
        });
    }

    /*============================== 开始 / 删除 ==============================*/

    private void startPlay(ScriptRoute route) {
        if (route == null || route.points.isEmpty()) {
            GoUtils.DisplayToast(this, getResources().getString(R.string.script_need_point));
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

        // 以脚本第一个路点作为起始位置（ServiceGo 会把脚本对齐到离它最近的轨迹点）
        ScriptWaypoint first = route.points.get(0);
        Intent intent = new Intent(this, ServiceGo.class);
        intent.putExtra(MainActivity.SCRIPT_ROUTE_ID, route.id);
        intent.putExtra(MainActivity.LNG_MSG_ID, first.lng);
        intent.putExtra(MainActivity.LAT_MSG_ID, first.lat);
        intent.putExtra(MainActivity.ALT_MSG_ID, first.alt);

        try {
            startForegroundService(intent);
            mPlayingScriptId = route.id;
            mPlayingIndex = 0;
            // 新开一条：本地先按“播放中”显示，连上后由服务端状态校准
            mScriptPaused = false;
            notifyAdapter();

            // 已经绑定着就立刻校验一次；还没绑上就立刻绑定，连接回调里兜底补启动，
            // 保证“点开始”最终一定会真的进入播放（不只依赖 onStartCommand 送达）
            mPendingStartId = null;
            if (mServiceBinder != null) {
                ensureScriptPlaying(route.id);
            } else {
                mPendingStartId = route.id;
                bindServiceIfNeeded();
            }

            GoUtils.DisplayToast(this, getResources().getString(R.string.script_start));

            // 播放已开始：自动退回地图主界面。本地服务连接通常几十毫秒就回来，
            // 留 500ms 让“连上后兜底启动”先跑完再退
            mListView.postDelayed(this::finish, 500);
        } catch (Exception e) {
            GoUtils.DisplayToast(this, getResources().getString(R.string.script_start_fail));
        }
    }

    /** 校验服务端确实在播放指定脚本；没在播就用 binder 兜底启动一次 */
    private void ensureScriptPlaying(String scriptId) {
        if (mServiceBinder == null || scriptId == null || scriptId.isEmpty()) {
            return;
        }

        try {
            // 服务端“真的”在播同一条脚本就跳过：重复 startScript 会让播放器重新走一遍
            // findAnchor，用当前模拟位置把轨迹重新锚定一次，位置会莫名跳一下。
            // 以前这里无条件重启，是“先启动模拟位置、再起用脚本”表现异常的来源之一。
            //
            // 双条件缺一不可：isScriptPlaying() 是状态 + 播放器双确认（防状态残留误判），
            // id 相同才叫“同一条”（换脚本必须能立刻切过去）。
            boolean playingSame = mServiceBinder.isScriptPlaying()
                    && scriptId.equals(mServiceBinder.getRunningScriptId());
            if (playingSame) {
                // 暂停中又点了一次 ▶：当作继续播放，而不是重新对齐轨迹
                if (mServiceBinder.isScriptPaused() && mServiceBinder.resumeScript()) {
                    mScriptPaused = false;
                }
                mPlayingScriptId = scriptId;
                return;
            }

            boolean ok = mServiceBinder.startScript(scriptId);
            if (ok) {
                mPlayingScriptId = scriptId;
                mScriptPaused = false;
            } else {
                GoUtils.DisplayToast(this, getResources().getString(R.string.script_start_fail));
            }
        } catch (Exception e) {
            // 忽略服务异常
        }
    }

    /**
     * 暂停 / 继续播放当前脚本。
     *
     * <p>暂停 = 位置停在原地、进度保留；继续 = 从暂停处原地接着走（不瞬移、不跳段）。
     * 服务还没绑上时先记住请求，连上后在 onServiceConnected 里补执行，
     * 避免“点了一下没反应”。</p>
     */
    private void togglePause() {
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
            notifyAdapter();
        } catch (Exception e) {
            // 忽略服务异常
        }
    }

    /** 正在播放的脚本被删除时，顺手停掉播放 */
    private void stopPlaying() {
        mPendingStartId = null;

        if (mBound && mServiceBinder != null) {
            try {
                mServiceBinder.stopScript();
            } catch (Exception ignored) {
                // 忽略服务异常
            }
        } else {
            Intent intent = new Intent(this, ServiceGo.class);
            try {
                stopService(intent);
            } catch (Exception ignored) {
                // 忽略服务异常
            }
        }

        mPlayingScriptId = null;
        mScriptPaused = false;
        notifyAdapter();
    }

    private static String safeName(ScriptRoute route) {
        if (route == null || route.name == null || route.name.trim().isEmpty()) {
            return "";
        }
        return route.name.trim();
    }

    /*============================== 列表适配器 ==============================*/

    private class ScriptAdapter extends BaseAdapter {
        private final LayoutInflater inflater = LayoutInflater.from(ScriptActivity.this);

        @Override
        public int getCount() {
            return mRoutes.size();
        }

        @Override
        public Object getItem(int position) {
            return mRoutes.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = inflater.inflate(R.layout.script_item, parent, false);
            }

            ScriptRoute route = mRoutes.get(position);
            if (route == null) {
                return view;
            }

            TextView name = view.findViewById(R.id.script_item_name);
            TextView badge = view.findViewById(R.id.script_item_badge);
            TextView loop = view.findViewById(R.id.script_item_loop);
            TextView info = view.findViewById(R.id.script_item_info);
            TextView status = view.findViewById(R.id.script_item_status);
            ImageButton edit = view.findViewById(R.id.script_item_edit);
            ImageButton start = view.findViewById(R.id.script_item_start);
            CheckBox check = view.findViewById(R.id.script_item_check);

            // 勾选删除模式：显示复选框，隐藏“开始 / 编辑”，条目点击即切换勾选
            check.setVisibility(mSelectMode ? View.VISIBLE : View.GONE);
            check.setChecked(mCheckedIds.contains(route.id));
            check.setOnClickListener(v -> toggleChecked(route.id));
            int actionVisibility = mSelectMode ? View.GONE : View.VISIBLE;
            start.setVisibility(actionVisibility);
            edit.setVisibility(actionVisibility);

            name.setText(safeName(route));

            // 状态角标：单一状态显示“走 / 跑 / 骑”，混用状态提示“含多种状态”
            ScriptWaypoint.Mode mode = primaryMode(route);
            if (mode == null) {
                badge.setVisibility(View.GONE);
            } else {
                badge.setVisibility(View.VISIBLE);
                badge.setText(modeLabel(mode));
                badge.setBackgroundColor(modeColor(mode));
            }

            loop.setText(route.isLoop() ? R.string.script_loop_on : R.string.script_loop_off);
            loop.setBackgroundResource(route.isLoop() ? R.drawable.round_badge_accent : R.drawable.round_badge_gray);

            double distance = ScriptParser.totalDistance(route);
            long seconds = ScriptParser.totalSeconds(route);
            info.setText(getResources().getString(R.string.script_item_info,
                    route.size(),
                    ScriptParser.formatDistance(distance),
                    ScriptParser.formatDuration(seconds)));

            boolean playing = route.id.equals(mPlayingScriptId);
            if (playing) {
                status.setVisibility(View.VISIBLE);
                int index = Math.min(mPlayingIndex + 1, Math.max(1, route.size()));
                // 展示服务端真正生效的状态（播放中手动切换过的也反映出来），暂停中单独提示
                status.setText(getResources().getString(
                        mScriptPaused ? R.string.script_paused_status : R.string.script_playing,
                        index, route.size(),
                        modeLabel(mEffectiveMode != null ? mEffectiveMode : mode)));
            } else {
                status.setVisibility(View.GONE);
                status.setText("");
            }

            // 铅笔按钮：显式的编辑入口（点条目本身也能进编辑页）
            edit.setOnClickListener(v -> openEditor(route));
            if (playing) {
                // 正在跑的这条：按钮变成“暂停 / 继续”，就地切换，不重新启动脚本
                start.setImageResource(mScriptPaused ? R.drawable.ic_play : R.drawable.ic_pause);
                start.setContentDescription(getResources().getString(
                        mScriptPaused ? R.string.script_resume : R.string.script_pause));
                start.setOnClickListener(v -> togglePause());
            } else {
                // 复用行会被反复绑定，图标必须每次显式重置，否则会残留上一行的暂停图标
                start.setImageResource(R.drawable.ic_script_play);
                start.setContentDescription(getResources().getString(R.string.script_start));
                start.setOnClickListener(v -> startPlay(route));
            }

            return view;
        }
    }

    private static ScriptWaypoint.Mode primaryMode(ScriptRoute route) {
        if (route == null || route.points == null || route.points.isEmpty()) {
            return null;
        }

        ScriptWaypoint.Mode mode = route.points.get(0).mode;
        for (ScriptWaypoint point : route.points) {
            if (point.mode != mode) {
                return null;
            }
        }

        return mode;
    }

    private String modeLabel(ScriptWaypoint.Mode mode) {
        if (mode == null) {
            return getResources().getString(R.string.script_badge_multi);
        }
        return mode.label;
    }

    private int modeColor(ScriptWaypoint.Mode mode) {
        int colorRes;
        switch (mode) {
            case RUN:
                colorRes = R.color.darkorange;
                break;
            case BIKE:
                colorRes = R.color.steelblue;
                break;
            case WALK:
            default:
                colorRes = R.color.colorAccent;
                break;
        }

        return ContextCompat.getColor(this, colorRes);
    }
}
