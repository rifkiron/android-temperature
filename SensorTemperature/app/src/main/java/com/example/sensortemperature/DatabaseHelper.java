package com.example.sensortemperature;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "TemperatureData.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_TEMPERATURE = "temperature";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TEMPERATURE = "temperature_value";
    public static final String COLUMN_TIMESTAMP = "timestamp";

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + TABLE_TEMPERATURE + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_TEMPERATURE + " REAL," +
                    COLUMN_TIMESTAMP + " INTEGER)";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Implement migration logic here if needed
    }
}
