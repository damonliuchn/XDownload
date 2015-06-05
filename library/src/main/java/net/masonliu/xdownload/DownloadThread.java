package net.masonliu.xdownload;



import android.util.Log;

import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadThread extends Thread {

	private RandomAccessFile saveFile;
	private URL downUrl;
	private int block;
	private int threadId = -1;
	private int startPos;
	private int downLength;
	private boolean finish = false;
	private Downloader downloader;

	public DownloadThread(Downloader downloader, URL downUrl,
			RandomAccessFile saveFile, int block, int startPos, int threadId) {
		this.downUrl = downUrl;
		this.saveFile = saveFile;
		this.block = block;
		this.startPos = startPos;
		this.downloader = downloader;
		this.threadId = threadId;
		downLength = startPos - (block * (threadId - 1));
	}

	@Override
	public void run() {
		if (downLength < block) {
			try {
				HttpURLConnection http = (HttpURLConnection) downUrl
						.openConnection();
				http.setRequestMethod("GET");
				http.setRequestProperty(
						"Accept",
						"image/gif, image/jpeg, image/pjpeg, image/pjpeg, application/x-shockwave-flash, application/xaml+xml, application/vnd.ms-xpsdocument, application/x-ms-xbap, application/x-ms-application, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, */*");
				http.setRequestProperty("Accept-Language", "zh-CN");
				http.setRequestProperty("Referer", downUrl.toString());
				http.setRequestProperty("Charset", "UTF-8");
				http.setRequestProperty("Range", "bytes=" + startPos + "-");
				http.setRequestProperty(
						"User-Agent",
						"Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)");
				http.setRequestProperty("Connection", "Keep-Alive");
				InputStream inStream = http.getInputStream();
				int max = block > 1024 ? 1024 : (block > 10 ? 10 : 1);
				byte[] buffer = new byte[max];
				int offset = 0;
				print("线程开始" + threadId + "起始位置" + startPos );
				while (downLength < block && (offset = inStream.read(buffer, 0, max)) != -1) {
					saveFile.write(buffer, 0, offset);
					downLength += offset;
					downloader.append(offset);
					int spare = block - downLength;
					if (spare < max)
						max = (int) spare;
					if(isInterrupted()){
						break;
					}
				}
				downloader.update(threadId, block * (threadId - 1) + downLength,1);
				saveFile.close();
				inStream.close();
				print("线程结束：" + threadId);
				finish = true;
			} catch (Exception e) {
				downloader.update(threadId, block * (threadId - 1) + downLength,0);
				downLength = -1;
				print("线程" + threadId + ":" + e);
			}
		}
	}

	private static void print(String msg) {
		Log.e("xdownlod-thread",msg);
	}

	public boolean isFinish() {
		return finish;
	}

	public long getDownLength() {
		return downLength;
	}
}
