package net.masonliu.xdownload;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBOpenHelper extends SQLiteOpenHelper {

    private static final String DBNAME = "XDOWNLOAD.db";

    private static final int VERSION = 1;

    public DBOpenHelper(Context context) {
        super(context, DBNAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE IF NOT EXISTS filedown (id varchar(100) primary key ,downpath varchar(100) , threadid INTEGER, position INTEGER)";
        try {
            db.execSQL(sql);
        } catch (SQLException e) {
            System.out.println("create table failed");
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onCreate(db);
    }

}
