package mapapi.overlayutil;

import android.os.Bundle;
import android.util.Log;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.Polyline;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.sug.SuggestionResult;
import com.zcshou.gogogo.R;

import java.util.ArrayList;
import java.util.List;

/**
 * 用于显示poi的overly
 */
public class PoiOverlay extends OverlayManager {

    private static final int MAX_POI_SIZE = 10;
    
    private PoiResult mPoiResult = null;
    private SuggestionResult mSuggestionResult = null;
    private BaiduMap mBaiduMap;
    
    /**
     * 构造函数
     *
     * @param baiduMap 该 PoiOverlay 引用的 BaiduMap 对象
     */
    public PoiOverlay(BaiduMap baiduMap) {
        super(baiduMap);
        mBaiduMap = baiduMap;
    }
    
    /**
     * 设置POI数据
     *
     * @param poiResult 设置POI数据
     */
    public void setData(PoiResult poiResult) {
        this.mPoiResult = poiResult;
    }
    
    public void setSugData(SuggestionResult sugResult) {
        this.mSuggestionResult = sugResult;
    }
    
    
    @Override
    public final List<OverlayOptions> getOverlayOptions() {
        if ((mPoiResult == null || mPoiResult.getAllPoi() == null) && (mSuggestionResult == null || mSuggestionResult.getAllSuggestions() == null)) {
            return null;
        }
        
        List<OverlayOptions> markerList = new ArrayList<OverlayOptions>();
        int curInd = 0;
        
        if (!(mPoiResult == null || mPoiResult.getAllPoi() == null)) {
            for (int i = 0; i < mPoiResult.getAllPoi().size(); i++) {
                if (mPoiResult.getAllPoi().get(i).location == null) {
                    continue;
                }
                
                Bundle bundle = new Bundle();
                bundle.putInt("index", curInd++);
                markerList.add(new MarkerOptions()
                               .icon(BitmapDescriptorFactory.fromAssetWithDpi("ic_location_on_black_36dp.png")).extraInfo(bundle)
                               .position(mPoiResult.getAllPoi().get(i).location));
            }
        }
        
        if (!(mSuggestionResult == null || mSuggestionResult.getAllSuggestions() == null)) {
            for (int j = 0; j < mSuggestionResult.getAllSuggestions().size(); j++) {
                if (mSuggestionResult.getAllSuggestions().get(j).pt == null) {
                    continue;
                }
                
                Bundle bundle = new Bundle();
                bundle.putInt("index", curInd++);
                markerList.add(new MarkerOptions()
                               .icon(BitmapDescriptorFactory.fromAssetWithDpi("ic_location_on_black_36dp.png")).extraInfo(bundle)
                               .position(mSuggestionResult.getAllSuggestions().get(j).pt));
            }
        }
        
        return markerList;
    }
    
    /**
     * 获取该 PoiOverlay 的 poi数据
     *
     * @return
     */
    public PoiResult getPoiResult() {
        return mPoiResult;
    }
    
    public SuggestionResult getSugResult() {
        return mSuggestionResult;
    }
    
    /**
     * 覆写此方法以改变默认点击行为
     *
     * @param i 被点击的poi在
     *          {@link com.baidu.mapapi.search.poi.PoiResult#getAllPoi()} 中的索引
     * @return
     */
    public boolean onPoiClick(int i) {
        //        if (mPoiResult.getAllPoi() != null
        //                && mPoiResult.getAllPoi().get(i) != null) {
        //            Toast.makeText(BMapManager.getInstance().getContext(),
        //                    mPoiResult.getAllPoi().get(i).name, Toast.LENGTH_LONG)
        //                    .show();
        //        }
        return false;
    }
    
    @Override
    public final boolean onMarkerClick(Marker marker) {
        if (!mOverlayList.contains(marker)) {
            return false;
        }
        
        if (marker.getExtraInfo() != null) {
            //            marker.setAnimation(getScaleAnimation());
            //            marker.startAnimation();
            mBaiduMap.clear();
            addToMap();
            mBaiduMap.addOverlay(new MarkerOptions().position(marker.getPosition()).icon(BitmapDescriptorFactory
                                 .fromResource(R.drawable.icon_gcoding)));
            //            marker.setIcon(BitmapDescriptorFactory.fromAssetWithDpi("ic_location_on_black_48dp.png"));
            return onPoiClick(marker.getExtraInfo().getInt("index"));
        }
        
        return false;
    }
    
    @Override
    public boolean onPolylineClick(Polyline polyline) {
        // TODO Auto-generated method stub
        return false;
    }
    
}
