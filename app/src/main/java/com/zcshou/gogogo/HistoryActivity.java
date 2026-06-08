package com.zcshou.gogogo;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.view.Gravity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Locale;
import java.util.List;

import com.zcshou.database.DataBaseHistoryLocation;
import com.zcshou.utils.GoUtils;

public class HistoryActivity extends BaseActivity {

    private RecyclerView mRecordListView;
    private TextView noRecordText;
    private LinearLayout mSearchLayout;
    private SQLiteDatabase mHistoryLocationDB;
    private List<HistoryItem> mAllItems;
    private SharedPreferences sharedPreferences;
    private SearchView mSearchView;
    private HistoryAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* 为了启动欢迎页全屏，状态栏被设置了透明，但是会导致其他页面状态栏空白
         * 这里设计如下：
         * 1. 除了 WelcomeActivity 之外的所有 Activity 均继承 BaseActivity
         * 2. WelcomeActivity 单独处理，其他 Activity 手动填充 StatusBar
         * */
        getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimary, this.getTheme()));

        setContentView(R.layout.activity_history);

        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        initLocationDataBase();

        initSearchView();

        initRecordListView();
    }

    @Override
    protected void onDestroy() {
        mHistoryLocationDB.close();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this add items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_history, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            this.finish(); // back button
            return true;
        } else if (id ==  R.id.action_delete) {
            new AlertDialog.Builder(HistoryActivity.this)
                    .setTitle("警告")//这里是表头的内容
                    .setMessage("确定要删除全部历史记录吗?")//这里是中间显示的具体信息
                    .setPositiveButton("确定",
                            (dialog, which) -> {
                                if (deleteRecord(-1)) {
                                    GoUtils.DisplayToast(this, getResources().getString(R.string.history_delete_ok));
                                    updateRecordList();
                                }
                            })
                    .setNegativeButton("取消",
                            (dialog, which) -> {
                            })
                    .show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initLocationDataBase() {
        try {
            DataBaseHistoryLocation hisLocDBHelper = new DataBaseHistoryLocation(getApplicationContext());
            mHistoryLocationDB = hisLocDBHelper.getWritableDatabase();
        } catch (Exception e) {
            Log.e("HistoryActivity", "ERROR - initLocationDataBase");
        }

        recordArchive();
    }

    //sqlite 操作 查询所有记录
    private List<HistoryItem> fetchAllRecord() {
        List<HistoryItem> data = new ArrayList<>();

        try {
            Cursor cursor = mHistoryLocationDB.query(DataBaseHistoryLocation.TABLE_NAME, null,
                    DataBaseHistoryLocation.DB_COLUMN_ID + " > ?", new String[] {"0"},
                    null, null, DataBaseHistoryLocation.DB_COLUMN_TIMESTAMP + " DESC", null);

            while (cursor.moveToNext()) {
                int ID = cursor.getInt(0);
                String Location = cursor.getString(1);
                String Longitude = cursor.getString(2);
                String Latitude = cursor.getString(3);
                long TimeStamp = cursor.getInt(4);
                String BD09Longitude = cursor.getString(5);
                String BD09Latitude = cursor.getString(6);
                Log.d("TB", ID + "\t" + Location + "\t" + Longitude + "\t" + Latitude + "\t" + TimeStamp + "\t" + BD09Longitude + "\t" + BD09Latitude);
                BigDecimal bigDecimalLongitude = BigDecimal.valueOf(Double.parseDouble(Longitude));
                BigDecimal bigDecimalLatitude = BigDecimal.valueOf(Double.parseDouble(Latitude));
                BigDecimal bigDecimalBDLongitude = BigDecimal.valueOf(Double.parseDouble(BD09Longitude));
                BigDecimal bigDecimalBDLatitude = BigDecimal.valueOf(Double.parseDouble(BD09Latitude));
                double doubleLongitude = bigDecimalLongitude.setScale(11, RoundingMode.HALF_UP).doubleValue();
                double doubleLatitude = bigDecimalLatitude.setScale(11, RoundingMode.HALF_UP).doubleValue();
                double doubleBDLongitude = bigDecimalBDLongitude.setScale(11, RoundingMode.HALF_UP).doubleValue();
                double doubleBDLatitude = bigDecimalBDLatitude.setScale(11, RoundingMode.HALF_UP).doubleValue();

                String timeStr = GoUtils.timeStamp2Date(Long.toString(TimeStamp));
                String wgsLatLng = "[经度:" + doubleLongitude + " 纬度:" + doubleLatitude + "]";
                String bdLatLng = "[经度:" + doubleBDLongitude + " 纬度:" + doubleBDLatitude + "]";

                data.add(new HistoryItem(ID, Location, timeStr,
                        wgsLatLng, bdLatLng, doubleBDLongitude, doubleBDLatitude));
            }
            cursor.close();
        } catch (Exception e) {
            data.clear();
            Log.e("HistoryActivity", "ERROR - fetchAllRecord");
        }

        return data;
    }

    private void recordArchive() {
        double limits;
        try {
            limits = Double.parseDouble(sharedPreferences.getString("setting_pos_history", getResources().getString(R.string.history_expiration)));
        } catch (NumberFormatException e) {  // GOOD: The exception is caught.
            limits = 7;
        }
        final long weekSecond = (long) (limits * 24 * 60 * 60);

        try {
            mHistoryLocationDB.delete(DataBaseHistoryLocation.TABLE_NAME,
                    DataBaseHistoryLocation.DB_COLUMN_TIMESTAMP + " < ?", new String[] {Long.toString(System.currentTimeMillis() / 1000 - weekSecond)});
        } catch (Exception e) {
            Log.e("HistoryActivity", "ERROR - recordArchive");
        }
    }

    private boolean deleteRecord(int ID) {
        boolean deleteRet = true;

        try {
            if (ID <= -1) {
                mHistoryLocationDB.delete(DataBaseHistoryLocation.TABLE_NAME,null, null);
            } else {
                mHistoryLocationDB.delete(DataBaseHistoryLocation.TABLE_NAME,
                        DataBaseHistoryLocation.DB_COLUMN_ID + " = ?", new String[] {Integer.toString(ID)});
            }
        } catch (Exception e) {
            deleteRet = false;
            Log.e("HistoryActivity", "ERROR - deleteRecord");
        }

        return deleteRet;
    }

    private void initSearchView() {
        mSearchView = findViewById(R.id.searchView);
        mSearchView.onActionViewExpanded();// 当展开无输入内容的时候，没有关闭的图标
        mSearchView.setSubmitButtonEnabled(false);//显示提交按钮
        mSearchView.setFocusable(false);
        mSearchView.clearFocus();
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {// 当点击搜索按钮时触发该方法
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {// 当搜索内容改变时触发该方法
                if (mAdapter != null) {
                    mAdapter.filter(newText);
                    if (!TextUtils.isEmpty(newText) && mAdapter.isFilteredEmpty()) {
                        GoUtils.DisplayToast(HistoryActivity.this, getResources().getString(R.string.history_error_search));
                        mAdapter.filter(""); // 无结果时回退到全部
                    }
                }
                return false;
            }
        });
    }

    private void showDeleteDialog(int locId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("警告");
        builder.setMessage("确定要删除该项历史记录吗?");
        builder.setPositiveButton("确定", (dialog, whichButton) -> {
            boolean deleteRet = deleteRecord(locId);
            if (deleteRet) {
                GoUtils.DisplayToast(HistoryActivity.this, getResources().getString(R.string.history_delete_ok));
                boolean removed = mAdapter.removeItemById(locId);
                if (!removed || mAdapter.isFilteredEmpty()) {
                    // 如果当前过滤列表为空，重新加载
                    updateRecordList();
                } else if (mAdapter.getItemCount() == 0) {
                    updateEmptyView();
                }
            }
        });
        builder.setNegativeButton("取消", null);

        builder.show();
    }

    private void showInputDialog(int locId, String name) {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(name);
        input.setSelectAllOnFocus(true);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("名称");
        builder.setView(input);
        builder.setPositiveButton("确认", (dialog, whichButton) -> {
            String userInput = input.getText().toString();
            DataBaseHistoryLocation.updateHistoryLocation(mHistoryLocationDB, String.valueOf(locId), userInput);
            // 精准刷新单条 item，保留搜索状态，不会触发全部 item 刷新
            mAdapter.updateItemTitle(locId, userInput);
        });
        builder.setNegativeButton("取消", null);

        builder.show();
    }

    private String[] randomOffset(String longitude, String latitude) {
        String max_offset_default = getResources().getString(R.string.setting_random_offset_default);
        double lon_max_offset = Double.parseDouble(Objects.requireNonNull(sharedPreferences.getString("setting_lon_max_offset", max_offset_default)));
        double lat_max_offset = Double.parseDouble(Objects.requireNonNull(sharedPreferences.getString("setting_lat_max_offset", max_offset_default)));
        double lon = Double.parseDouble(longitude);
        double lat = Double.parseDouble(latitude);

        double randomLonOffset = (Math.random() * 2 - 1) * lon_max_offset;  // Longitude offset (meters)
        double randomLatOffset = (Math.random() * 2 - 1) * lat_max_offset;  // Latitude offset (meters)

        lon += randomLonOffset / 111320;    // (meters -> longitude)
        lat += randomLatOffset / 110574;    // (meters -> latitude)

        String offsetMessage = String.format(Locale.US, "经度偏移: %.2f米\n纬度偏移: %.2f米", randomLonOffset, randomLatOffset);
        GoUtils.DisplayToast(this, offsetMessage);

        return new String[]{String.valueOf(lon), String.valueOf(lat)};
    }

    private void initRecordListView() {
        noRecordText = findViewById(R.id.record_no_textview);
        mSearchLayout = findViewById(R.id.search_linear);
        mRecordListView = findViewById(R.id.record_list_view);

        // RecyclerView 配置
        mRecordListView.setLayoutManager(new LinearLayoutManager(this));

        mAdapter = new HistoryAdapter();

        // 点击 item：导航到该位置
        mAdapter.setOnItemClickListener(item -> {
            String bdLongitudeStr = String.valueOf(item.getBdLongitude());
            String bdLatitudeStr = String.valueOf(item.getBdLatitude());
            String name = item.getLocation();

            // Random offset
            if (sharedPreferences.getBoolean("setting_random_offset", false)) {
                String[] offsetResult = randomOffset(bdLongitudeStr, bdLatitudeStr);
                bdLongitudeStr = offsetResult[0];
                bdLatitudeStr = offsetResult[1];
            }

            if (!MainActivity.showLocation(item.getId(), name, bdLongitudeStr, bdLatitudeStr)) {
                GoUtils.DisplayToast(this, getResources().getString(R.string.history_error_location));
            }
            this.finish();
        });

        // 长按 item：弹出编辑/删除菜单
        mAdapter.setOnItemLongClickListener((anchorView, item) -> {
            PopupMenu popupMenu = new PopupMenu(HistoryActivity.this, anchorView);
            popupMenu.setGravity(Gravity.END | Gravity.BOTTOM);
            popupMenu.getMenu().add("编辑");
            popupMenu.getMenu().add("删除");

            popupMenu.setOnMenuItemClickListener(menuItem -> {
                switch (menuItem.getTitle().toString()) {
                    case "编辑":
                        showInputDialog(item.getId(), item.getLocation());
                        return true;
                    case "删除":
                        showDeleteDialog(item.getId());
                        return true;
                    default:
                        return false;
                }
            });

            popupMenu.show();
        });

        mRecordListView.setAdapter(mAdapter);

        updateRecordList();
    }

    private void updateEmptyView() {
        if (mAdapter != null && !mAdapter.isFilteredEmpty()) {
            noRecordText.setVisibility(View.GONE);
            mRecordListView.setVisibility(View.VISIBLE);
            mSearchLayout.setVisibility(View.VISIBLE);
        } else {
            mRecordListView.setVisibility(View.GONE);
            mSearchLayout.setVisibility(View.GONE);
            noRecordText.setVisibility(View.VISIBLE);
        }
    }

    private void updateRecordList() {
        mAllItems = fetchAllRecord();

        if (mAllItems.isEmpty()) {
            mAdapter.setData(null);
            updateEmptyView();
        } else {
            mAdapter.setData(mAllItems);
            updateEmptyView();
            // 恢复当前搜索过滤状态
            if (mSearchView != null) {
                String currentQuery = mSearchView.getQuery().toString();
                if (!TextUtils.isEmpty(currentQuery)) {
                    mAdapter.filter(currentQuery);
                }
            }
        }
    }
}