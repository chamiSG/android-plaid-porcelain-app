package com.plaid.platform.porcelain.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.sql.Blob;

public class DatabaseHelper extends SQLiteOpenHelper {

    // Table Name
    public static final String DONATION_TABLE = "DONATION";
    public static final String ITEM_TABLE = "ITEMS";

    // Table columns
    //DONATION_TABLE
    public static final String _ID = "_id";
    public static final String DONATE = "donate";
    public static final String CHARITY = "charity";
    public static final String PENDING_DONATE = "pending_donate";
    public static final String TOTAL_DONATE = "total_donate";
    public static final String TOTAL_TRANSACTIONS = "total_transaction";
    // ITEMS
    public static final String ITEM_ID = "_id";
    public static final String ITEM_IMAGE = "item_image";
    public static final String ITEM_NAME = "item_name";
    public static final String ITEM_TYPE = "item_type";

    // Database Information
    static final String DB_NAME = "PORCELAIN.DB";

    // database version
    static final int DB_VERSION = 1;

    // Creating table query
    private static final String CREATE_DONATION_TABLE = "create table " + DONATION_TABLE + "(" + _ID
            + " INTEGER PRIMARY KEY AUTOINCREMENT, " + DONATE + " DOUBLE DEFAULT '0.50', " + CHARITY + " TEXT DEFAULT 'United Way Worldwide', " + PENDING_DONATE + " DOUBLE DEFAULT 0, " + TOTAL_DONATE + " DOUBLE DEFAULT 0, " + TOTAL_TRANSACTIONS + " DOUBLE DEFAULT 0);";
    private static final String CREATE_ITEM_TABLE = "create table " + ITEM_TABLE + "(" + ITEM_ID
            + " INTEGER PRIMARY KEY AUTOINCREMENT, " + ITEM_IMAGE + " blob , " + ITEM_NAME + " TEXT , "+ ITEM_TYPE + " TEXT );";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(CREATE_DONATION_TABLE);
        db.execSQL(CREATE_ITEM_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + DONATION_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + ITEM_TABLE);
        onCreate(db);
    }
}
