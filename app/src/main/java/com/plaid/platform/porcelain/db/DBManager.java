package com.plaid.platform.porcelain.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.google.gson.internal.$Gson$Types;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;

public class DBManager {

    private DatabaseHelper dbHelper;

    private Context context;

    private SQLiteDatabase database;
    Context mContext;

    public DBManager(Context c) {
        context = c;
    }

    public DBManager open() throws SQLException {
        dbHelper = new DatabaseHelper(context);
        database = dbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        dbHelper.close();
    }
//


    public void defaultDonateInsert(double donate, String charity, double pending_donate, double total_donate, double transaction) {

        ContentValues contentValue = new ContentValues();
        contentValue.put(DatabaseHelper.DONATE, donate);
        contentValue.put(DatabaseHelper.CHARITY, charity);
        contentValue.put(DatabaseHelper.PENDING_DONATE, pending_donate);
        contentValue.put(DatabaseHelper.TOTAL_DONATE, total_donate);
        contentValue.put(DatabaseHelper.TOTAL_TRANSACTIONS, transaction);
        database.insert(DatabaseHelper.DONATION_TABLE, null, contentValue);
    }

    public int donateUpdate(double donate) {
        ContentValues contentValue = new ContentValues();
        contentValue.put(DatabaseHelper.DONATE, donate);
        int i = database.update(DatabaseHelper.DONATION_TABLE, contentValue, DatabaseHelper._ID + " = " + 1, null);
        return i;
    }

    public int charityUpdate(String charity) {
        ContentValues contentValue = new ContentValues();
        contentValue.put(DatabaseHelper.CHARITY, charity);
        int i = database.update(DatabaseHelper.DONATION_TABLE, contentValue, DatabaseHelper._ID + " = " + 1, null);
        return i;
    }

    public int pendingDonateUpdate(double pending_donate) {
        ContentValues contentValue = new ContentValues();
        contentValue.put(DatabaseHelper.PENDING_DONATE, pending_donate);
        int i = database.update(DatabaseHelper.DONATION_TABLE, contentValue, DatabaseHelper._ID + " = " + 1, null);
        return i;
    }

    public int totalDonateUpdate(double total_donate) {
        ContentValues contentValue = new ContentValues();
        contentValue.put(DatabaseHelper.TOTAL_DONATE, total_donate);
        int i = database.update(DatabaseHelper.DONATION_TABLE, contentValue, DatabaseHelper._ID + " = " + 1, null);
        return i;
    }

    public int transactionUpdate(double transaction) {
        ContentValues contentValue = new ContentValues();
        contentValue.put(DatabaseHelper.TOTAL_TRANSACTIONS, transaction);
        int i = database.update(DatabaseHelper.DONATION_TABLE, contentValue, DatabaseHelper._ID + " = " + 1, null);
        return i;
    }

    public void resetDonateDB() {

        ContentValues contentValue = new ContentValues();
        contentValue.put(DatabaseHelper.DONATE, 0);
        contentValue.put(DatabaseHelper.CHARITY, "United Way Worldwide");
        contentValue.put(DatabaseHelper.PENDING_DONATE, 0);
        contentValue.put(DatabaseHelper.TOTAL_DONATE, 0);
        contentValue.put(DatabaseHelper.TOTAL_TRANSACTIONS, 0);
        database.update(DatabaseHelper.DONATION_TABLE, contentValue, DatabaseHelper._ID + " = " + 1, null);
    }

    public int itemUpdate(long _id, String image, String name, String type) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHelper.ITEM_IMAGE, image);
        contentValues.put(DatabaseHelper.ITEM_NAME, name);
        contentValues.put(DatabaseHelper.ITEM_TYPE, type);
        int i = database.update(DatabaseHelper.ITEM_TABLE, contentValues, DatabaseHelper.ITEM_ID + " = " + _id, null);
        return i;
    }

    public void itemDelete(long _id) {
        database.delete(DatabaseHelper.ITEM_TABLE, DatabaseHelper.ITEM_ID + "=" + _id, null);
    }
    public Cursor getData(String query) {
        return database.rawQuery(query,null);
    }

}
