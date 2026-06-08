package com.zcshou.gogogo;

public class HistoryItem {
    private final int id;
    private String location;
    private final String time;
    private final String wgsLatLngText;
    private final String bdLatLngText;
    private final double bdLongitude;
    private final double bdLatitude;

    public HistoryItem(int id, String location, String time,
                       String wgsLatLngText, String bdLatLngText,
                       double bdLongitude, double bdLatitude) {
        this.id = id;
        this.location = location;
        this.time = time;
        this.wgsLatLngText = wgsLatLngText;
        this.bdLatLngText = bdLatLngText;
        this.bdLongitude = bdLongitude;
        this.bdLatitude = bdLatitude;
    }

    public int getId() { return id; }

    public String getLocation() { return location; }

    public void setLocation(String location) { this.location = location; }

    public String getTime() { return time; }

    public String getWgsLatLngText() { return wgsLatLngText; }

    public String getBdLatLngText() { return bdLatLngText; }

    public double getBdLongitude() { return bdLongitude; }

    public double getBdLatitude() { return bdLatitude; }

    /**
     * 用于搜索匹配，检查所有可搜索字段
     */
    public boolean matches(String query) {
        if (query == null || query.isEmpty()) return true;
        String lowerQuery = query.toLowerCase();
        return location.toLowerCase().contains(lowerQuery)
                || time.toLowerCase().contains(lowerQuery)
                || wgsLatLngText.toLowerCase().contains(lowerQuery);
    }
}
