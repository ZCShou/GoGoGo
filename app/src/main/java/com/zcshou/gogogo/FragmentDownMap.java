package com.zcshou.gogogo;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.mapapi.map.offline.MKOLSearchRecord;
import com.baidu.mapapi.map.offline.MKOLUpdateElement;
import com.baidu.mapapi.map.offline.MKOfflineMap;
import com.baidu.mapapi.map.offline.MKOfflineMapListener;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.zcshou.gogogo.FragmentLocalMap.localMapList;
import static com.zcshou.gogogo.FragmentLocalMap.updateView;

/*地图下载页面*/

public class FragmentDownMap extends Fragment implements MKOfflineMapListener {

    public static final String ARG_PAGE = "ARG_PAGE";

    private ListView mCityListView;
    private SearchView mCitySearchView;
    private TextView mTips;

    public static MKOfflineMap mOffline = null;
    
    List<Map<String, Object>> mAllCityList;
    List<Map<String, Object>> mHotCityList;
    
    public static FragmentDownMap newInstance(int page) {
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, page);
        FragmentDownMap pageFragment = new FragmentDownMap();
        pageFragment.setArguments(args);

        return pageFragment;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("FragmentDownMap", "onCreate");
    }
    
    @Override
    public void onResume() {
        super.onResume();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d("FragmentDownMap", "onCreateView");
        View view = inflater.inflate(R.layout.map_down_list, container, false);
        //init search view
        mCitySearchView = view.findViewById(R.id.searchView);
        mCitySearchView.onActionViewExpanded();// 当展开无输入内容的时候，没有关闭的图标
        mCitySearchView.setSubmitButtonEnabled(false);//显示提交按钮
        mCitySearchView.setFocusable(false);
        mCitySearchView.requestFocusFromTouch();
        mCitySearchView.clearFocus();

        // 去掉搜索框的下划线
        if (mCitySearchView != null) {
            try {        //--拿到字节码
                Class<?> argClass = mCitySearchView.getClass();
                //--指定某个私有属性,mSearchPlate是搜索框父布局的名字
                Field ownField = argClass.getDeclaredField("mSearchPlate");
                //--暴力反射,只有暴力反射才能拿到私有属性
                ownField.setAccessible(true);
                View mView = (View) ownField.get(mCitySearchView);
                //--设置背景
                if (mView != null) {
                    mView.setBackgroundColor(Color.TRANSPARENT);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        //offline map
        if (mOffline == null) {
            mOffline = new MKOfflineMap();
            mOffline.init(this);
        }
        
        //获取城市列表
        mAllCityList = fetchAllCity();
        mCityListView = view.findViewById(R.id.city_list_view);
        SimpleAdapter simAdapt = new SimpleAdapter(
            view.getContext(),
            mAllCityList,
            R.layout.map_down_item,
            new String[] {"key_cityname", "key_citysize", "key_cityid"}, // 与下面数组元素要一一对应
            new int[] {R.id.CityNameText, R.id.CitySizeText, R.id.CityIDText});
        mCityListView.setAdapter(simAdapt);

        mHotCityList = fetchAllHotCity();

        mTips = view.findViewById(R.id.offline_map_tips);

        //list item click
        setItemClickListener();

        //设置搜索的监听
        setSearchViewListener();

        return view;
    }
    
    //WIFI是否可用
    private boolean isWifiConnected() {
        Context context = getContext();
        if (context != null) {
            ConnectivityManager mConnectivityManager = (ConnectivityManager)getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mWiFiNetworkInfo = mConnectivityManager
                    .getNetworkInfo(ConnectivityManager.TYPE_WIFI);

            if (mWiFiNetworkInfo != null) {
                return mWiFiNetworkInfo.isAvailable();
            }
        }

        return false;
    }
    
    //MOBILE网络是否可用
    private boolean isMobileConnected() {
        Context context = getContext();
        if (context != null) {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mMobileNetworkInfo = mConnectivityManager
                    .getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

            if (mMobileNetworkInfo != null) {
                return mMobileNetworkInfo.isAvailable();
            }
        }

        return false;
    }

    // 断是否有网络连接，但是如果该连接的网络无法上网，也会返回true
    public boolean isNetworkConnected() {
        Context context = getContext();
        if (context != null) {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) getContext()
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();

            if (mNetworkInfo != null) {
                return mNetworkInfo.isAvailable();
            }
        }

        return false;
    }

    //网络是否可用
    private boolean isNetworkAvailable() {
        return ((isWifiConnected() || isMobileConnected()) && isNetworkConnected());
    }

    public String formatDataSize(long size) {
        String ret;
        
        if (size < (1024 * 1024)) {
            ret = String.format(Locale.getDefault(), "%dK", size / 1024);
        } else {
            ret = String.format(Locale.getDefault(), "%.1fM", size / (1024 * 1024.0));
        }
        
        return ret;
    }
    
    private void setSearchViewListener() {
        mCitySearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
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
                    mTips.setText(R.string.offline_map_tips_all);

                    SimpleAdapter simAdapt = new SimpleAdapter(
                        FragmentDownMap.this.getContext(),
                        mAllCityList,
                        R.layout.map_down_item,
                        new String[] {"key_cityname", "key_citysize", "key_cityid"}, // 与下面数组元素要一一对应
                        new int[] {R.id.CityNameText, R.id.CitySizeText, R.id.CityIDText});
                    mCityListView.setAdapter(simAdapt);
                } else {
                    ArrayList<MKOLSearchRecord> records = mOffline.searchCity(newText);
                    List<Map<String, Object>> searchRet = new ArrayList<>();
                    
                    if (records != null) {
                        if (records.size() > 0) {
                            for (MKOLSearchRecord r : records) {
                                Log.d("CITY", "" + r.cityName);
                                Map<String, Object> item = new HashMap<>();
                                item.put("key_cityname", r.cityName);
                                item.put("key_citysize", formatDataSize(r.dataSize));
                                item.put("key_cityid", r.cityID);
                                searchRet.add(item);
                            }

                            SimpleAdapter simAdapt = new SimpleAdapter(
                                FragmentDownMap.this.getContext(),
                                searchRet,
                                R.layout.map_down_item,
                                new String[] {"key_cityname", "key_citysize", "key_cityid"}, // 与下面数组元素要一一对应
                                new int[] {R.id.CityNameText, R.id.CitySizeText, R.id.CityIDText});
                            mCityListView.setAdapter(simAdapt);
                        }
                    } else {
                        DisplayToast("未搜索到该城市或该城市不支持离线地图");

                        mTips.setText(R.string.offline_map_tips_hot);

                        SimpleAdapter simAdapt = new SimpleAdapter(
                            FragmentDownMap.this.getContext(),
                            mHotCityList,
                            R.layout.map_down_item,
                            new String[] {"key_cityname", "key_citysize", "key_cityid"}, // 与下面数组元素要一一对应
                            new int[] {R.id.CityNameText, R.id.CitySizeText, R.id.CityIDText});
                        mCityListView.setAdapter(simAdapt);
                    }
                }
                
                return false;
            }
        });
    }
    
    private void setItemClickListener() {
        mCityListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //获取cityID
                final String cityID = ((TextView)(view.findViewById(R.id.CityIDText))).getText().toString();
                final String cityName = ((TextView)(view.findViewById(R.id.CityNameText))).getText().toString();
                Context context = FragmentDownMap.this.getContext();
                if (context != null) {
                    new AlertDialog.Builder(context)
                            .setTitle("下载")//这里是表头的内容
                            .setMessage("确定要下载" + cityName + "的离线地图吗?") //这里是中间显示的具体信息
                            .setPositiveButton("确定",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            boolean exist = false;
                                            boolean needUpdate = true;

                                            for (MKOLUpdateElement e : localMapList) {
                                                if (e.cityID == Integer.parseInt(cityID)) {
                                                    if (e.ratio == 100) {
                                                        exist = true;
                                                        if (!e.update){
                                                            needUpdate = false;
                                                            DisplayToast("离线地图已存在");
                                                        }
                                                    }
                                                    break;
                                                }
                                            }

                                            if (isNetworkAvailable()) {
                                                if (!exist) {
                                                    mOffline.start(Integer.parseInt(cityID));
                                                    mCitySearchView.onActionViewCollapsed();
                                                    DisplayToast("开始下载离线地图");
                                                } else {
                                                    if (needUpdate) {
                                                        mOffline.update(Integer.parseInt(cityID));
                                                        DisplayToast("开始更新离线地图");
                                                    }
                                                }
                                            } else {
                                                DisplayToast("网络连接不可用,请检查网络设置");
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
                }
            }
        });
    }
    
    private List<Map<String, Object>> fetchAllCity() {
        List<Map<String, Object>> data = new ArrayList<>();
        
        // 获取所有支持离线地图的城市
        try {
            ArrayList<MKOLSearchRecord> records1 = mOffline.getOfflineCityList();
            
            if (records1 != null) {
                for (MKOLSearchRecord r : records1) {
                    //V4.5.0起，保证数据不溢出，使用long型保存数据包大小结果
                    Map<String, Object> item = new HashMap<>();
                    item.put("key_cityname", r.cityName);
                    item.put("key_citysize", this.formatDataSize(r.dataSize));
                    item.put("key_cityid", r.cityID);
                    data.add(item);
                }
            }
        } catch (Exception e) {
            data.clear();
            e.printStackTrace();
        }
        
        return data;
    }
    
    private List<Map<String, Object>> fetchAllHotCity() {
        List<Map<String, Object>> data = new ArrayList<>();
        
        // 获取热闹城市列表
        try {
            ArrayList<MKOLSearchRecord> records1 = mOffline.getHotCityList();
            
            if (records1 != null) {
                for (MKOLSearchRecord r : records1) {
                    //V4.5.0起，保证数据不溢出，使用long型保存数据包大小结果
                    Map<String, Object> item = new HashMap<>();
                    item.put("key_cityname", r.cityName);
                    item.put("key_citysize", this.formatDataSize(r.dataSize));
                    item.put("key_cityid", r.cityID);
                    data.add(item);
                }
            }
        } catch (Exception e) {
            data.clear();
            e.printStackTrace();
        }
        
        return data;
    }
    
    @Override
    public void onGetOfflineMapState(int type, int state) {
        switch (type) {
        case MKOfflineMap.TYPE_DOWNLOAD_UPDATE: {
            MKOLUpdateElement update = mOffline.getUpdateInfo(state);
            
            // 处理下载进度更新提示
            if (update != null) {
                updateView();
            }
        }
        break;
        
        case MKOfflineMap.TYPE_NEW_OFFLINE:
            // 有新离线地图安装
            Log.d("OfflineDemo", String.format("add offline map num:%d", state));
            break;
            
        case MKOfflineMap.TYPE_VER_UPDATE:
            // 版本更新提示
            // MKOLUpdateElement e = mOffline.getUpdateInfo(state);
            break;
            
        default:
            break;
        }
    }

    //public MKOfflineMapListener getMKOfflineMapListener() {
    //    return FragmentDownMap.this;
    //}

    public void DisplayToast(String str) {
        Toast toast = Toast.makeText(FragmentDownMap.this.getContext(), str, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP, 0, 250);
        toast.show();
    }
}