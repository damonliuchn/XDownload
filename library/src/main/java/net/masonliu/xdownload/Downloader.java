package net.masonliu.xdownload;


import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Downloader {

    private DAO dao;
    private int id;
    private int currentSize = 0;
    private int totalSize = 0;
    private DownloadThread[] threads;
    private URL downloadUrl;
    private String downloadUrlStr;
    private File file;
    /* 缓存各线程最后下载的位置 */
    private Map<Integer, Integer> data = new ConcurrentHashMap<Integer, Integer>();
    /* 每条线程下载的大小 */
    private int block;
    private DownloadProgressListener listener;

    public Downloader(Context context, String downloadUrlStr,
                      File fileSaveDir, int threadNum, int id) throws IOException {
        this.id = id;
        this.downloadUrlStr = downloadUrlStr;
        downloadUrl = new URL(downloadUrlStr);
        dao = new DAO(context, threadNum);
        threads = new DownloadThread[threadNum];
        initFileStatus(fileSaveDir);
        initDataAndBlock();
    }

    private void initFileStatus(File fileSaveDir) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) downloadUrl.openConnection();
        conn.setConnectTimeout(6 * 1000);
        conn.setRequestMethod("GET");
        conn.setRequestProperty(
                "Accept",
                "image/gif, image/jpeg, image/pjpeg, image/pjpeg, application/x-shockwave-flash, application/xaml+xml, application/vnd.ms-xpsdocument, application/x-ms-xbap, application/x-ms-application, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, */*");
        conn.setRequestProperty("Accept-Language", "zh-CN");
        conn.setRequestProperty("Referer", downloadUrlStr);
        conn.setRequestProperty("Charset", "UTF-8");
        conn.setRequestProperty(
                "User-Agent",
                "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)");
        conn.setRequestProperty("Connection", "Keep-Alive");
        conn.connect();
        if (conn.getResponseCode() == 200) {
            totalSize = conn.getContentLength();
            if (totalSize <= 0) {
                throw new RuntimeException("无法获知文件大小 ");
            }
            String filename = getFileName(conn);
            if (!fileSaveDir.exists()) {
                fileSaveDir.mkdirs();
            }
            file = new File(fileSaveDir, filename + ".tmp");/* 保存文件 */
            conn.disconnect();
        } else {
            conn.disconnect();
            throw new IOException("服务器响应错误 ");
        }
    }

    private String getFileName(HttpURLConnection conn) {
        String filename = downloadUrl.toString().substring(downloadUrl.toString().lastIndexOf('/') + 1);
        if (filename == null || "".equals(filename.trim())) {// 如果获取不到文件名称
            String raw = conn.getHeaderField("Content-Disposition");
            // raw = "attachment; filename=abc.jpg"
            if (raw != null && raw.indexOf("=") != -1) {
                filename = raw.split("=")[1]; //getting value after '='
            } else {
                filename = UUID.randomUUID() + ".tmp";// 默认取一个文件名
            }
        }
        return filename;
    }

    private void initDataAndBlock() {
        block = totalSize / threads.length + 1;

        Map<Integer, Integer> logdata = dao.getData(downloadUrlStr);

        if (logdata.size() == threads.length) {
            data.putAll(logdata);
            for (int i = 0; i < threads.length; i++) {
                currentSize += data.get(i + 1) - (block * i);
            }
        }else{
            //如果缓存与新下载的线程数已经不相等了，需要重新下载
            dao.delete(downloadUrlStr);
            for (int i = 0; i < threads.length; i++) {
                data.put(i + 1, block * i);
            }
            dao.insert(downloadUrlStr, data);
        }
    }

    public int getTotalSize() {
        return totalSize;
    }

    public String getFilePath() {
        if (file != null)
            return file.getPath();
        return null;
    }

    public int download(DownloadProgressListener listener) throws Exception {
        try {
            this.listener = listener;
            //开始下载
            for (int i = 0; i < threads.length; i++) {
                int downLength = data.get(i + 1) - (block * i);
                // 该线程未完成下载时,继续下载
                if (downLength < block && data.get(i + 1) < totalSize) {
                    RandomAccessFile randOut = new RandomAccessFile(file, "rw");
                    if (totalSize > 0) {
                        randOut.setLength(totalSize);
                    }
                    randOut.seek(data.get(i + 1));
                    threads[i] = new DownloadThread(this, downloadUrl, randOut, block, data.get(i + 1), i + 1);
                    threads[i].setPriority(7);
                    threads[i].start();
                } else {
                    threads[i] = null;
                }
            }
            //轮询查看下载进度
            boolean notFinish = true;
            while (notFinish) {
                Thread.sleep(1000);
                notFinish = false;
                for (int i = 0; i < threads.length; i++) {
                    if (threads[i] != null && !threads[i].isFinish()) {
                        notFinish = true;
                        if (threads[i].getDownLength() == -1) {
                            RandomAccessFile randOut = new RandomAccessFile(file, "rw");
                            randOut.seek(data.get(i + 1));
                            threads[i] = new DownloadThread(this,
                                    downloadUrl, randOut, block,
                                    data.get(i + 1), i + 1);
                            threads[i].setPriority(7);
                            threads[i].start();
                        }
                    }
                }
                if (listener != null) {
                    listener.onDownloadSize(currentSize,totalSize);
                }
            }
            if(currentSize == totalSize){
                dao.delete(downloadUrlStr);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("下载失败");
        }
        return currentSize;
    }

    public void pause() {
        if (threads == null)
            return;
        for (int i = 0; i < threads.length; i++) {
            if (threads[i] == null)
                continue;
            threads[i].interrupt();
        }
    }

    /**
     * 供线程调用
     */

    protected synchronized void append(int size) {
        currentSize += size;
    }

    /** 子线程结束后都会执行*/
    protected synchronized void update(int threadId, int pos, int state) {
        data.put(threadId, pos);
        dao.update(downloadUrlStr, data);

        if(currentSize == totalSize){
            dao.delete(downloadUrlStr);
        }
        if (listener != null) {
            listener.onDownloadSize(currentSize,totalSize);
        }
    }

}
