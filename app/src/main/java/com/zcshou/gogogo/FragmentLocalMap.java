package com.zcshou.gogogo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.mapapi.map.offline.MKOLUpdateElement;

import java.util.ArrayList;

import static com.zcshou.gogogo.FragmentDownMap.mOffline;

/*已下载离线地图页面*/

public class FragmentLocalMap extends Fragment {

    public static final String ARG_PAGE = "ARG_PAGE";
    public static TextView noOfflineMap;
    public static ListView localMapListView;
    
    public static ArrayList<MKOLUpdateElement> localMapList = null;
    public static LocalMapAdapter lAdapter = null;
    
    public static FragmentLocalMap newInstance(int page) {
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, page);
        FragmentLocalMap pageFragment = new FragmentLocalMap();
        pageFragment.setArguments(args);
        return pageFragment;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // getArguments().getInt(ARG_PAGE);
        Log.d("FragmentLocalMap", "onCreate");
    }
    
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.map_local_list, container, false);
        
        //offline map
        if (mOffline != null) {
            // 获取已下过的离线地图信息
            localMapList = mOffline.getAllUpdateInfo();
        }

        if (localMapList == null) {
            localMapList = new ArrayList<>();
        }
        
        //init list view
        localMapListView = view.findViewById(R.id.local_map_list);
        noOfflineMap = view.findViewById(R.id.no_offline_map);
        lAdapter = new LocalMapAdapter();
        localMapListView.setAdapter(lAdapter);
        
        //
        if (localMapList.size() != 0) {
            localMapListView.setVisibility(View.VISIBLE);
            noOfflineMap.setVisibility(View.GONE);
        } else {
            localMapListView.setVisibility(View.GONE);
            noOfflineMap.setVisibility(View.VISIBLE);
        }
        
        return view;
    }
    
    public static void updateView() {
        localMapList = mOffline.getAllUpdateInfo();
        
        if (localMapList == null) {
            localMapList = new ArrayList<>();
        }
        
        if (localMapList.size() != 0) {
            localMapListView.setVisibility(View.VISIBLE);
            noOfflineMap.setVisibility(View.GONE);
        } else {
            localMapListView.setVisibility(View.GONE);
            noOfflineMap.setVisibility(View.VISIBLE);
        }
        
        lAdapter.notifyDataSetChanged();
    }
    
    /**
     * 离线地图管理列表适配器
     */
    public class LocalMapAdapter extends BaseAdapter {
    
        @Override
        public int getCount() {
            return localMapList.size();
        }
        
        @Override
        public Object getItem(int index) {
            return localMapList.get(index);
        }
        
        @Override
        public long getItemId(int index) {
            return index;
        }
        
        @SuppressLint("ViewHolder")
        @Override
        public View getView(int index, View view, ViewGroup arg2) {
            MKOLUpdateElement e = (MKOLUpdateElement) getItem(index);
            view = View.inflate(FragmentLocalMap.this.getContext(), R.layout.map_local_item, null);
            initViewItem(view, e);
            return view;
        }
        
        void initViewItem(View view, final MKOLUpdateElement e) {
            Button remove = view.findViewById(R.id.remove);
            Button control = view.findViewById(R.id.control);
            TextView title = view.findViewById(R.id.title);
            TextView update = view.findViewById(R.id.update);
            TextView ratio = view.findViewById(R.id.ratio);
            ratio.setText(e.ratio + "%");
            title.setText(e.cityName);
            
            if (e.update) {
                update.setText("可更新");
                control.setVisibility(View.VISIBLE);
            } else {
                update.setText("最新");
            }

            remove.setEnabled(true);

            //更新按钮
            control.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DisplayToast("开始更新");
                    mOffline.update(e.cityID);
                    //updateView();
                }
            });
            remove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    Context context = FragmentLocalMap.this.getContext();
                    if (context != null) {
                        new AlertDialog.Builder(FragmentLocalMap.this.getContext())
                                .setTitle("警告")//这里是表头的内容
                                .setMessage("确定要删除" + e.cityName + "的离线地图吗?") //这里是中间显示的具体信息
                                .setPositiveButton("确定",
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                if (e.status == MKOLUpdateElement.DOWNLOADING){
                                                    mOffline.pause(e.cityID);
                                                }
                                                mOffline.remove(e.cityID);
                                                updateView();
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
    }

    public void DisplayToast(String str) {
        Toast toast = Toast.makeText(FragmentLocalMap.this.getContext(), str, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP, 0, 220);
        toast.show();
    }
}