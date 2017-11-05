package com.skyfz.vipplayer.SQHelper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHandler extends SQLiteOpenHelper {

    // All Static variables
    // Database Version
    private static final int DATABASE_VERSION = 1;

    // Database Name
    private static final String DATABASE_NAME = "SkyOlinManager";

    // APIList table name
    private static final String TABLE_API = "APIList";

    // APIList Table Columns names
    private static final String KEY_ID = "id";
    private static final String KEY_NAME = "name";
    private static final String KEY_URL = "url";

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_CONTACTS_TABLE = "CREATE TABLE " + TABLE_API + "("
                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_NAME + " TEXT,"
                + KEY_URL + " TEXT" + ")";
        db.execSQL(CREATE_CONTACTS_TABLE);
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_API);

        // Create tables again
        onCreate(db);
    }

    /**
     * All CRUD(Create, Read, Update, Delete) Operations
     */

    // Adding new api
    public long addAPI(ApiField APIList) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_NAME, APIList.getName());
        values.put(KEY_URL, APIList.getURL()); 

        // Inserting Row
        long id = db.insert(TABLE_API, null, values);
        db.close(); // Closing database connection
        return id;
    }

    // Getting single api
    ApiField getURL(long id) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_API, new String[] { KEY_ID,
                        KEY_NAME, KEY_URL }, KEY_ID + "=?",
                new String[] { String.valueOf(id) }, null, null, null, null);
        if (cursor != null)
            cursor.moveToFirst();

        ApiField api = new ApiField(Integer.parseInt(cursor.getString(0)),
                cursor.getString(1), cursor.getString(2));
        
        return api;
    }

    // Getting All ApiFields
    public List<ApiField> getAllAPI() {
        List<ApiField> APIList = new ArrayList<ApiField>();
        // Select All Query
        String selectQuery = "SELECT  * FROM " + TABLE_API;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                ApiField api = new ApiField();
                api.setID(Integer.parseInt(cursor.getString(0)));
                api.setName(cursor.getString(1));
                api.setURL(cursor.getString(2));
                // Adding api to list
                APIList.add(api);
            } while (cursor.moveToNext());
        }

        // return api list
        return APIList;
    }

    // Updating single api
    public int updateAPI(ApiField api) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_NAME, api.getName());
        values.put(KEY_URL, api.getURL());

        // updating row
        return db.update(TABLE_API, values, KEY_ID + " = ?",
                new String[] { String.valueOf(api.getID()) });
    }

    // Deleting single api
    public void deleteAPI(ApiField api) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_API, KEY_ID + " = ?",
                new String[]{String.valueOf(api.getID())});
        db.close();
    }

    // Deleting single api by id
    public void deleteAPIById(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_API, KEY_ID + " = ?",
                new String[] { String.valueOf(id) });
        db.close();
    }


    // Getting apis Count
    public int getAPICount() {
        String countQuery = "SELECT  * FROM " + TABLE_API;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        int c = cursor.getCount();
        cursor.close();

        // return count
        return c;
    }

}
