package com.zcshou.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DataBaseHistoryLocation extends SQLiteOpenHelper {
    public static final String TABLE_NAME = "HistoryLocation";

    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "HistoryLocation.db";
    private static final String CREATE_TABLE = "create table if not exists " + TABLE_NAME +
            " (ID INTEGER PRIMARY KEY AUTOINCREMENT, Location TEXT, WGS84Longitude TEXT NOT NULL, WGS84Latitude TEXT NOT NULL, TimeStamp BIGINT NOT NULL, BD09Longitude TEXT NOT NULL, BD09Latitude TEXT NOT NULL)";
            
    public DataBaseHistoryLocation(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }
    
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(CREATE_TABLE);
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        String sql = "DROP TABLE IF EXISTS " + TABLE_NAME;
        sqLiteDatabase.execSQL(sql);
        onCreate(sqLiteDatabase);
    }
}