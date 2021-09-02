package com.zcshou.utils;

public class MapUtils {
//    public final static String COORDINATE_TYPE_GCJ02 = "gcj02";
//    public final static String COORDINATE_TYPE_BD09LL = "bd09ll";
//    public final static String COORDINATE_TYPE_BD09MC = "bd09";
//    public static float[] EARTH_WEIGHT = {0.1f, 0.2f, 0.4f, 0.6f, 0.8f}; // 推算计算权重_地球
//    public static float[] MOON_WEIGHT = {0.0167f,0.033f,0.067f,0.1f,0.133f};
//    public static float[] MARS_WEIGHT = {0.034f,0.068f,0.152f,0.228f,0.304f};
    
    //坐标转换相关
    public final static double pi = 3.14159265358979324;
    public final static double a = 6378245.0;
    public final static double ee = 0.00669342162296594323;
    public final static double x_pi = 3.14159265358979324 * 3000.0 / 180.0;
    
    public static double[] bd2wgs(double lon, double lat) {
        double[] bd2Gcj = bd09togcj02(lon, lat);
        return gcj02towgs84(bd2Gcj[0], bd2Gcj[1]);
    }

    /**
     * WGS84 转换为 BD-09
     * @param lng   经度
     * @param lat   纬度
     * @return double[] 转换后的经度，纬度 数组
     *
     */
    public static double[] wgs2bd09(double lng, double lat){
        //第一次转换
        double dlat = transformLat(lng - 105.0, lat - 35.0);
        double dlng = transformLon(lng - 105.0, lat - 35.0);
        double radlat = lat / 180.0 * pi;
        double magic = Math.sin(radlat);
        magic = 1 - ee * magic * magic;
        double sqrtmagic = Math.sqrt(magic);
        dlat = (dlat * 180.0) / ((a * (1 - ee)) / (magic * sqrtmagic) * pi);
        dlng = (dlng * 180.0) / (a / sqrtmagic * Math.cos(radlat) * pi);
        double mglat = lat + dlat;
        double mglng = lng + dlng;

        //第二次转换
        double z = Math.sqrt(mglng * mglng + mglat * mglat) + 0.00002 * Math.sin(mglat * x_pi);
        double theta = Math.atan2(mglat, mglng) + 0.000003 * Math.cos(mglng * x_pi);
        double bd_lng = z * Math.cos(theta) + 0.0065;
        double bd_lat = z * Math.sin(theta) + 0.006;
        return new double[] { bd_lng, bd_lat };
    }
    
    public static double[] bd09togcj02(double bd_lon, double bd_lat) {
        double x = bd_lon - 0.0065;
        double y = bd_lat - 0.006;
        double z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * x_pi);
        double theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * x_pi);
        double gg_lng = z * Math.cos(theta);
        double gg_lat = z * Math.sin(theta);
        return new double[] { gg_lng, gg_lat };
    }

    public static double[] gcj02towgs84(double lng, double lat) {
        double dlat = transformLat(lng - 105.0, lat - 35.0);
        double dlng = transformLon(lng - 105.0, lat - 35.0);
        double radlat = lat / 180.0 * pi;
        double magic = Math.sin(radlat);
        magic = 1 - ee * magic * magic;
        double sqrtmagic = Math.sqrt(magic);
        dlat = (dlat * 180.0) / ((a * (1 - ee)) / (magic * sqrtmagic) * pi);
        dlng = (dlng * 180.0) / (a / sqrtmagic * Math.cos(radlat) * pi);
        double mglat = lat + dlat;
        double mglng = lng + dlng;
        return new double[] { lng * 2 - mglng, lat * 2 - mglat };
    }
    
    private static double transformLat(double lat, double lon) {
        double ret = -100.0 + 2.0 * lat + 3.0 * lon + 0.2 * lon * lon + 0.1 * lat * lon + 0.2 * Math.sqrt(Math.abs(lat));
        ret += (20.0 * Math.sin(6.0 * lat * pi) + 20.0 * Math.sin(2.0 * lat * pi)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(lon * pi) + 40.0 * Math.sin(lon / 3.0 * pi)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(lon / 12.0 * pi) + 320 * Math.sin(lon * pi  / 30.0)) * 2.0 / 3.0;
        return ret;
    }
    
    private static double transformLon(double lat, double lon) {
        double ret = 300.0 + lat + 2.0 * lon + 0.1 * lat * lat + 0.1 * lat * lon + 0.1 * Math.sqrt(Math.abs(lat));
        ret += (20.0 * Math.sin(6.0 * lat * pi) + 20.0 * Math.sin(2.0 * lat * pi)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(lat * pi) + 40.0 * Math.sin(lat / 3.0 * pi)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(lat / 12.0 * pi) + 300.0 * Math.sin(lat / 30.0 * pi)) * 2.0 / 3.0;
        return ret;
    }
    
    private static boolean out_of_china(double lng, double lat) {
        return (lng < 72.004 || lng > 137.8347) || ((lat < 0.8293 || lat > 55.8271));
    }
}
