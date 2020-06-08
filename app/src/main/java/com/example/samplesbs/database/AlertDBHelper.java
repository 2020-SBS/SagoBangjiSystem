package com.example.samplesbs.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import javax.annotation.Nullable;

public class AlertDBHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "alert.db";
    public static final String TABLE_NAME = "alert_table";
    public static final String COL_1 = "ID";
    public static final String COL_2 = "SOUND";
    public static final String COL_3 = "VIBRATION";

    public AlertDBHelper(@Nullable Context context){
        super(context,DATABASE_NAME,null,1);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table if not exists "+TABLE_NAME + " (" +
                        COL_1 + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COL_2 + " INTEGER DEFAULT 0, " +
                        COL_3 +  " INTEGER DEFAULT 0);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS "+ TABLE_NAME);
        onCreate(db);
    }

    public void updateSound(int sound){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();


        Cursor cursor = db.rawQuery("select * from "+ TABLE_NAME,null);
        if(cursor.getCount()==0){
            cv.put(COL_2, sound);
            cv.put(COL_3, 0);
            db.insert(TABLE_NAME,null,cv);
        }else{
            cursor.moveToLast();
            long id = cursor.getLong(cursor.getColumnIndex(COL_1));
            cv.put(COL_2, sound);
            cv.put(COL_3, getVibration());
            db.update(TABLE_NAME,cv, COL_1 + "=?",new String[] {String.valueOf(id)});
        }
    }
    public void updateVibration(int vibration){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();


        Cursor cursor = db.rawQuery("select * from "+ TABLE_NAME,null);
        if(cursor.getCount()==0){
            cv.put(COL_2, 0);
            cv.put(COL_3, vibration);
            db.insert(TABLE_NAME,null,cv);
        }else{
            cursor.moveToLast();
            long id = cursor.getLong(cursor.getColumnIndex(COL_1));
            cv.put(COL_2, getSound());
            cv.put(COL_3, vibration);
            db.update(TABLE_NAME,cv, COL_1 + "=?",new String[] {String.valueOf(id)});
        }
    }

    public int getSound(){
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("select * from "+ TABLE_NAME,null);
        if(cursor.getCount()==0){
            return 0;
        }else{
            cursor.moveToFirst();
            int sound = cursor.getInt(cursor.getColumnIndex(COL_2));
            return sound;
        }
    }
    public int getVibration(){
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("select * from "+ TABLE_NAME,null);
        if(cursor.getCount()==0){
            return 0;
        }else{
            cursor.moveToFirst();
            int vibration = cursor.getInt(cursor.getColumnIndex(COL_3));
            return vibration;
        }
    }
}
