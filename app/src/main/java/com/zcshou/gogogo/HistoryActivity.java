package com.zcshou.gogogo;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;

import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zcshou.database.HistoryLocationDataBaseHelper;

import static com.zcshou.gogogo.MainActivity.showHistoryLocation;


public class HistoryActivity extends BaseActivity {
    private ListView listView;
    private TextView noRecordText;
    private SearchView mSearchView;
    private LinearLayout mSearchLayout;

    private SQLiteDatabase sqLiteDatabase;
    private List<Map<String, Object>> allHistoryRecord;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.history_list);

        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        try {
            HistoryLocationDataBaseHelper hisLocDBHelper = new HistoryLocationDataBaseHelper(getApplicationContext());
            sqLiteDatabase = hisLocDBHelper.getWritableDatabase();
        } catch (Exception e) {
            Log.e("HistoryActivity", "SQLiteDatabase init error");
            e.printStackTrace();
        }
        
        listView = findViewById(R.id.record_list_view);
        noRecordText = findViewById(R.id.record_no_textview);
        mSearchLayout = findViewById(R.id.search_linear);
        mSearchView = findViewById(R.id.searchView);
        mSearchView.onActionViewExpanded();// 当展开无输入内容的时候，没有关闭的图标
        mSearchView.setSubmitButtonEnabled(false);//显示提交按钮
        mSearchView.setFocusable(false);
        mSearchView.requestFocusFromTouch();
        mSearchView.clearFocus();

        if (recordArchive(sqLiteDatabase)) {
            Log.d("HistoryActivity", "archive success");
        }
        
        initListView();

        setSearchResultClickListener();

        setSearchViewListener();
    }

    @Override
    protected void onDestroy() {
        sqLiteDatabase.close();
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
                    .setTitle("Warning")//这里是表头的内容
                    .setMessage("确定要删除全部历史记录吗?")//这里是中间显示的具体信息
                    .setPositiveButton("确定",
                            (dialog, which) -> {
                                boolean deleteRet = deleteRecord(sqLiteDatabase, -1);

                                if (deleteRet) {
                                    DisplayToast("删除成功!");
                                    initListView();
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

    private void initListView() {
        allHistoryRecord = fetchAllRecord(sqLiteDatabase);

        if (allHistoryRecord.size() == 0) {
            listView.setVisibility(View.GONE);
            mSearchLayout.setVisibility(View.GONE);
            noRecordText.setVisibility(View.VISIBLE);
        } else {
            noRecordText.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
            mSearchLayout.setVisibility(View.VISIBLE);

            try {
                // 与下面数组元素要一一对应
                SimpleAdapter simAdapt = new SimpleAdapter(
                        this,
                        allHistoryRecord,
                        R.layout.history_item,
                        new String[]{"key_id", "key_location", "key_time", "key_wgslatlng", "kdy_bdlatlng"}, // 与下面数组元素要一一对应
                        new int[]{R.id.LocationID, R.id.LoctionText, R.id.TimeText, R.id.WGSLatLngText, R.id.BDLatLngText});
                listView.setAdapter(simAdapt);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void setSearchResultClickListener() {
        listView.setOnItemClickListener((adapterView, view, i, l) -> {
            String bd09Longitude;
            String bd09Latitude;
            String wgs84Longitude;
            String wgs84Latitude;
            //bd09坐标
            String bd09LatLng = (String) ((TextView) view.findViewById(R.id.BDLatLngText)).getText();
            bd09LatLng = bd09LatLng.substring(bd09LatLng.indexOf("[") + 1, bd09LatLng.indexOf("]"));
            String[] latLngStr = bd09LatLng.split(" ");
            bd09Longitude = latLngStr[0].substring(latLngStr[0].indexOf(":") + 1);
            bd09Latitude = latLngStr[1].substring(latLngStr[1].indexOf(":") + 1);
            //wgs84坐标
            String wgs84LatLng = (String) ((TextView) view.findViewById(R.id.WGSLatLngText)).getText();
            wgs84LatLng = wgs84LatLng.substring(wgs84LatLng.indexOf("[") + 1, wgs84LatLng.indexOf("]"));
            String[] latLngStr2 = wgs84LatLng.split(" ");
            wgs84Longitude = latLngStr2[0].substring(latLngStr2[0].indexOf(":") + 1);
            wgs84Latitude = latLngStr2[1].substring(latLngStr2[1].indexOf(":") + 1);

            if (!showHistoryLocation(bd09Longitude, bd09Latitude, wgs84Longitude, wgs84Latitude)) {
                DisplayToast("定位失败,请手动选取定位点");
            }

            returnLastActivity();
        });

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            new AlertDialog.Builder(HistoryActivity.this)
                    .setTitle("Warning")//这里是表头的内容
                    .setMessage("确定要删除该项历史记录吗?")//这里是中间显示的具体信息
                    .setPositiveButton("确定",
                            (dialog, which) -> {
                                String locID = (String) ((TextView) view.findViewById(R.id.LocationID)).getText();
                                boolean deleteRet = deleteRecord(sqLiteDatabase, Integer.parseInt(locID));

                                if (deleteRet) {
                                    DisplayToast("删除成功!");
                                    initListView();
                                }
                            })
                    .setNegativeButton("取消",
                            (dialog, which) -> {
                            })
                    .show();
            return true;
        });
    }

    private void setSearchViewListener() {
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            // 当点击搜索按钮时触发该方法
            @Override
            public boolean onQueryTextSubmit(String query) {
                // DisplayToast("click submit");
                return false;
            }

            // 当搜索内容改变时触发该方法
            @Override
            public boolean onQueryTextChange(String newText) {
                if (TextUtils.isEmpty(newText)) {
                    SimpleAdapter simAdapt = new SimpleAdapter(
                            HistoryActivity.this.getBaseContext(),
                            allHistoryRecord,
                            R.layout.history_item,
                            new String[]{"key_id", "key_location", "key_time", "key_wgslatlng", "kdy_bdlatlng"}, // 与下面数组元素要一一对应
                            new int[]{R.id.LocationID, R.id.LoctionText, R.id.TimeText, R.id.WGSLatLngText, R.id.BDLatLngText});
                    listView.setAdapter(simAdapt);
                } else {
                    List<Map<String, Object>> searchRet = new ArrayList<>();
                    for (int i = 0; i < allHistoryRecord.size(); i++){
                        if (allHistoryRecord.get(i).toString().indexOf(newText) > 0){
                            searchRet.add(allHistoryRecord.get(i));
                        }
                    }
                    if (searchRet.size() > 0) {
                        SimpleAdapter simAdapt = new SimpleAdapter(
                                HistoryActivity.this.getBaseContext(),
                                searchRet,
                                R.layout.history_item,
                                new String[]{"key_id", "key_location", "key_time", "key_wgslatlng", "kdy_bdlatlng"}, // 与下面数组元素要一一对应
                                new int[]{R.id.LocationID, R.id.LoctionText, R.id.TimeText, R.id.WGSLatLngText, R.id.BDLatLngText});
                        listView.setAdapter(simAdapt);
                    } else {
                        DisplayToast("未搜索到指定内容");
                        SimpleAdapter simAdapt = new SimpleAdapter(
                                HistoryActivity.this.getBaseContext(),
                                allHistoryRecord,
                                R.layout.history_item,
                                new String[]{"key_id", "key_location", "key_time", "key_wgslatlng", "kdy_bdlatlng"}, // 与下面数组元素要一一对应
                                new int[]{R.id.LocationID, R.id.LoctionText, R.id.TimeText, R.id.WGSLatLngText, R.id.BDLatLngText});
                        listView.setAdapter(simAdapt);
                    }
                }

                return false;
            }
        });
    }

    public void returnLastActivity() {
        this.finish();
    }
    
    //sqlite 操作 查询所有记录
    private List<Map<String, Object>> fetchAllRecord(SQLiteDatabase sqLiteDatabase) {
        List<Map<String, Object>> data = new ArrayList<>();
        
        try {
            Cursor cursor = sqLiteDatabase.query(HistoryLocationDataBaseHelper.TABLE_NAME, null,
                                                 "ID > ?", new String[] {"0"},
                                                 null, null, "TimeStamp DESC", null);
                                                 
            while (cursor.moveToNext()) {
                Map<String, Object> item = new HashMap<>();
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
                double doubleLongitude = bigDecimalLongitude.setScale(11, BigDecimal.ROUND_HALF_UP).doubleValue();
                double doubleLatitude = bigDecimalLatitude.setScale(11, BigDecimal.ROUND_HALF_UP).doubleValue();
                double doubleBDLongitude = bigDecimalBDLongitude.setScale(11, BigDecimal.ROUND_HALF_UP).doubleValue();
                double doubleBDLatitude = bigDecimalBDLatitude.setScale(11, BigDecimal.ROUND_HALF_UP).doubleValue();
                item.put("key_id", "" + ID);
                item.put("key_location", Location);
                item.put("key_time", timeStamp2Date(Long.toString(TimeStamp), null));
                item.put("key_wgslatlng", "[经度:" + doubleLongitude + " 纬度:" + doubleLatitude + "]");
                item.put("kdy_bdlatlng", "[经度:" + doubleBDLongitude + " 纬度:" + doubleBDLatitude + "]");
                data.add(item);
            }
            
            // 关闭光标
            cursor.close();
        } catch (Exception e) {
            Log.e("SQLITE", "query error");
            data.clear();
            e.printStackTrace();
        }
        
        return data;
    }
    
    public static String timeStamp2Date(String seconds, String format) {
        if (seconds == null || seconds.isEmpty() || seconds.equals("null")) {
            return "";
        }
        
        if (format == null || format.isEmpty()){
            format = "yyyy-MM-dd HH:mm:ss";
        }

        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat sdf = new SimpleDateFormat(format);

        return sdf.format(new Date(Long.parseLong(seconds + "000")));
    }
    
    //sqlite 操作 保留七天的数据
    private boolean recordArchive(SQLiteDatabase sqLiteDatabase) {
        boolean archiveRet = true;
        final long weekSecond = 7 * 24 * 60 * 60;
        
        try {
            sqLiteDatabase.delete(HistoryLocationDataBaseHelper.TABLE_NAME,
                                  "TimeStamp < ?", new String[] {Long.toString(System.currentTimeMillis() / 1000 - weekSecond)});
        } catch (Exception e) {
            Log.e("SQLITE", "archive error");
            archiveRet = false;
            e.printStackTrace();
        }
        
        Log.d("SQLITE", "archive success");

        return archiveRet;
    }
    
    //sqlite 操作 删除记录
    private boolean deleteRecord(SQLiteDatabase sqLiteDatabase, int ID) {
        boolean deleteRet = true;
        
        try {
            if (ID <= -1) {
                sqLiteDatabase.delete(HistoryLocationDataBaseHelper.TABLE_NAME,null, null);
            } else {
                sqLiteDatabase.delete(HistoryLocationDataBaseHelper.TABLE_NAME,
                        "ID = ?", new String[] {Integer.toString(ID)});
            }
            Log.d("DDDDDD", "delete success");
        } catch (Exception e) {
            Log.e("SQLITE", "delete error");
            deleteRet = false;
            e.printStackTrace();
        }
        
        return deleteRet;
    }

    public void DisplayToast(String str) {
        Toast toast = Toast.makeText(this, str, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP, 0, 220);
        toast.show();
    }
}