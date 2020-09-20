/*
 * Copyright (C) 2016 Baidu, Inc. All Rights Reserved.
 */
package mapapi.overlayutil;

import java.util.ArrayList;
import java.util.List;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.Polyline;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.route.IndoorRouteLine;

import android.graphics.Color;
import android.os.Bundle;

public class IndoorRouteOverlay extends OverlayManager {

    private IndoorRouteLine mRouteLine;
    int[] colorInfo;

    /**
     * 构造函数
     *
     * @param baiduMap
     *            该TransitRouteOverlay引用的 BaiduMap 对象
     */
    public IndoorRouteOverlay(BaiduMap baiduMap) {
        super(baiduMap);
        colorInfo = new int[]{ Color.argb(178, 0, 78, 255), Color.argb(178, 88, 208, 0), Color.argb(178, 88, 78,
                255)};
    }


    /**
     * 设置路线数据
     *
     * @param routeOverlay
     *            路线数据
     */
    public void setData(IndoorRouteLine routeOverlay) {
        this.mRouteLine = routeOverlay;
    }

    /**
     * 覆写此方法以改变默认起点图标
     *
     * @return 起点图标
     */
    public BitmapDescriptor getStartMarker() {
        return null;
    }

    /**
     * 覆写此方法以改变默认终点图标
     *
     * @return 终点图标
     */
    public BitmapDescriptor getTerminalMarker() {
        return null;
    }

    public int getLineColor() {
        return 0;
    }
    @Override
    public List<OverlayOptions> getOverlayOptions() {
        if (mRouteLine == null) {
            return null;
        }

        List<OverlayOptions> overlayList = new ArrayList<OverlayOptions>();


        // 添加step的节点
        if (mRouteLine.getAllStep() != null && mRouteLine.getAllStep().size() > 0) {
            for (IndoorRouteLine.IndoorRouteStep step : mRouteLine.getAllStep()) {
                Bundle b = new Bundle();
                b.putInt("index", mRouteLine.getAllStep().indexOf(step));
                if (step.getEntrace() != null) {
                    overlayList.add((new MarkerOptions()).position(step.getEntrace().getLocation())
                            .zIndex(10).anchor(0.5f, 0.5f).extraInfo(b)
                            .icon(BitmapDescriptorFactory.fromAssetWithDpi("Icon_walk_route.png")));
                }

                // 最后路段绘制出口点
                if (mRouteLine.getAllStep().indexOf(step) == (mRouteLine.getAllStep().size() - 1)
                        && step.getExit() != null) {
                    overlayList.add((new MarkerOptions()).position(step.getExit().getLocation()).anchor(0.5f, 0.5f)
                            .zIndex(10).icon(BitmapDescriptorFactory.fromAssetWithDpi("Icon_walk_route.png")));

                }
            }
        }
        // 添加起点starting
        if (mRouteLine.getStarting() != null) {
            overlayList.add((new MarkerOptions()).position(mRouteLine.getStarting().getLocation())
                    .icon(getStartMarker() != null ? getStartMarker() :
                            BitmapDescriptorFactory.fromAssetWithDpi("Icon_start.png"))
                    .zIndex(10));
        }
        // 添加终点terminal
        if (mRouteLine.getTerminal() != null) {
            overlayList.add((new MarkerOptions()).position(mRouteLine.getTerminal().getLocation())
                    .icon(getTerminalMarker() != null ? getTerminalMarker() :
                            BitmapDescriptorFactory.fromAssetWithDpi("Icon_end.png"))
                    .zIndex(10));
        }

        // 添加线poly line list
        if (mRouteLine.getAllStep() != null && mRouteLine.getAllStep().size() > 0) {
            LatLng lastStepLastPoint = null;
            int idex = 0;
            for (IndoorRouteLine.IndoorRouteStep step : mRouteLine.getAllStep()) {
                List<LatLng> watPoints = step.getWayPoints();
                if (watPoints != null) {
                    List<LatLng> points = new ArrayList<LatLng>();
                    if (lastStepLastPoint != null) {
                        points.add(lastStepLastPoint);
                    }
                    points.addAll(watPoints);
                    overlayList.add(new PolylineOptions().points(points).width(10)
                            .color(getLineColor() != 0 ? getLineColor() : colorInfo[idex++ % 3]).zIndex(0));
                    lastStepLastPoint = watPoints.get(watPoints.size() - 1);
                }
            }

        }

        return overlayList;

    }

//    private BitmapDescriptor getIconForStep(IndoorRouteLine.TransitStep step) {
//        switch (step.getVehileType()) {
//            case ESTEP_WALK:
//                return BitmapDescriptorFactory.fromAssetWithDpi("Icon_walk_route.png");
//            case ESTEP_TRAIN:
//                return BitmapDescriptorFactory.fromAssetWithDpi("Icon_subway_station.png");
//            case ESTEP_DRIVING:
//            case ESTEP_COACH:
//            case ESTEP_PLANE:
//            case ESTEP_BUS:
//                return BitmapDescriptorFactory.fromAssetWithDpi("Icon_bus_station.png");
//            default:
//                return null;
//        }
//    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }

    @Override
    public boolean onPolylineClick(Polyline polyline) {
        return false;
    }
}

