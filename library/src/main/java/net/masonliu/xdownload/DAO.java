package net.masonliu.xdownload;


import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.HashMap;
import java.util.Map;

public class DAO {

    private DBOpenHelper openHelper;
    private int threadNum;

    public DAO(Context context, int threadNum) {
        this.threadNum = threadNum;
        openHelper = new DBOpenHelper(context);
    }

    public Map<Integer, Integer> getData(String path) {
        SQLiteDatabase db = openHelper.getWritableDatabase();
        Map<Integer, Integer> data = new HashMap<Integer, Integer>();
        Cursor cursor = db.rawQuery(
                "select threadid, position from filedown where downpath=?",
                new String[]{path});
        while (cursor.moveToNext()) {
            data.put(cursor.getInt(cursor.getColumnIndex("threadid")),
                    cursor.getInt(cursor.getColumnIndex("position")));
        }
        cursor.close();
        db.close();
        return data;
    }

    public void insert(String path, Map<Integer, Integer> map) {
        SQLiteDatabase db = openHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
                db.execSQL(
                        "insert into filedown(id,downpath, threadid, position) values(?,?,?,?)",
                        new Object[]{path+entry.getKey(),path, entry.getKey(), entry.getValue()});
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
        db.close();
    }

    public void update(String path, Map<Integer, Integer> map) {
        SQLiteDatabase db = openHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
                db.execSQL(
                        "update filedown set position=? where downpath=? and threadid=?",
                        new Object[]{entry.getValue(), path, entry.getKey()});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        db.close();
    }

    public void delete(String path) {
        SQLiteDatabase db = openHelper.getWritableDatabase();
        db.execSQL("delete from filedown where downpath=?",
                new Object[]{path});
        db.close();
    }

}
