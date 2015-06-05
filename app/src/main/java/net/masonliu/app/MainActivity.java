package net.masonliu.app;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import net.masonliu.xdownload.DownloaderManager;

import java.io.File;


public class MainActivity extends ActionBarActivity {
    private Handler handler;
    private DownloaderManager downloaderManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button start = (Button)findViewById(R.id.button1);
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloaderManager.start();
            }
        });

        Button pause = (Button)findViewById(R.id.button2);
        pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloaderManager.pause();
            }
        });

        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch ((DownloaderManager.Mode) msg.obj) {
                    case connectFailed:
                        Log.e("xdownload","connectFailed");
                        break;
                    case ing:
                        int size = msg.getData().getInt("size");
                        int totalSize = msg.getData().getInt("totalSize");
                        Log.e("xdownload","size-totalSize:"+size+"-"+totalSize);
                        break;
                    case downError:
                        Log.e("xdownload","downError");
                        break;
                }
                return false;
            }
        });

        downloaderManager = new DownloaderManager(this,"http://mirror.bit.edu.cn/apache/tomcat/tomcat-7/v7.0.62/bin/apache-tomcat-7.0.62.tar.gz",new File(Environment.getExternalStorageDirectory().getPath()),3,handler);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
