package net.masonliu.xdownload;


import android.content.Context;
import android.os.Handler;
import android.os.Message;

import java.io.File;
import java.io.IOException;

public class DownloaderManager implements Runnable {

    private Context context;
    private String url;
    private File saveFile;
    private int threadNum;
    private int id;
    private Handler handler;

    private Downloader downer;
    private Thread downthread;
    private long currentSize;

    public enum Mode {
        connectFailed, ing,downError
    }

    public DownloaderManager(Context context, String url, File saveFile, int threadNum, Handler handler) {
        this.context = context;
        this.url = url;
        this.saveFile = saveFile;
        this.threadNum = threadNum;
        this.handler = handler;
    }

    public void run() {
        try {
            downer = new Downloader(context, url, saveFile, threadNum, id);
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            Message msg = new Message();
            msg.obj = Mode.connectFailed;
            msg.getData().putString("error", e1.toString());
            handler.sendMessage(msg);
            e1.printStackTrace();
            return;
        }
        try {
            downer.download(new DownloadProgressListener() {
                public void onDownloadSize(int size,int totalSize) {
                    currentSize = size;
                    Message msg = new Message();
                    msg.obj = Mode.ing;
                    msg.getData().putInt("size", size);
                    msg.getData().putInt("totalSize", totalSize);
                    handler.sendMessage(msg);
                }
            });

        } catch (Exception e) {
            Message msg = new Message();
            msg.obj = Mode.downError;
            msg.getData().putString("error", e.toString());
            handler.sendMessage(msg);
        }
    }

    public void start() {
        downthread = new Thread(this);
        downthread.start();
    }

    public void pause() {
        if (downer != null) {
            downer.pause();
            downer = null;
        }
        if (downthread != null) {
            downthread.interrupt();
            downthread = null;
        }
    }

    public float getDownloadSize() {
        return (float) currentSize;
    }
}
