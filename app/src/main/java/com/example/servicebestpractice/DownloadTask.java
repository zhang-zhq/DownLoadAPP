package com.example.servicebestpractice;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadTask extends AsyncTask<String, Integer, Integer> {

    public static final int TYPE_SUCCESS = 0;
    public static final int TYPE_FAILED = 1;
    public static final int TYPE_PAUSED = 2;
    public static final int TYPE_CANCELED = 3;

   //定义一个回调的接口，实现对下载的各种状态进行监听
    private DownloadListener listener;

    private boolean isCanceled = false;

    private boolean isPaused = false;

    private int lastProgress;


    public DownloadTask(DownloadListener listener) {
        this.listener = listener;
    }

    @Override
    protected Integer doInBackground(String... params) {
        InputStream is = null;
        RandomAccessFile savedFile = null;
        File file = null;
        try {
            long downloadedLength = 0; // 记录已下载的文件长度
            String downloadUrl = params[0];
            //从下载的路径中截取文件名
            String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
            //设置新的文件存放地址，这里是手机默认的下载位置
            String directory = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS).getPath();
            //新建下载文件，把要下载到的文件地址和文件名传入
            System.out.println(directory);
            file = new File(directory, fileName);
            //当前地址是否已经存在该文件，如果存在，就获取该文件的长度。方便与断点续传
            if (file.exists()) {
                downloadedLength = file.length();
            }
            //获得当前下载内容的长度，去请求服务器对应的路径开始下载。
            long contentLength = getContentLength(downloadUrl);
            if (contentLength == 0) {
                return TYPE_FAILED;
            } else if (contentLength == downloadedLength) {
                //这里，一开始下载文件， downloadedLength=0，,文件
                // 已下载字节和文件总字节相等，说明已经下载完成了
                return TYPE_SUCCESS;
            }
            //请求服务器开始下载
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    // 断点下载，指定从哪个字节开始下载
                    .addHeader("RANGE", "bytes=" + downloadedLength + "-")
                    .url(downloadUrl)
                    .build();
            Response response = client.newCall(request).execute();
            Log.d("下载中",downloadUrl);
            if (response != null) {

                is = response.body().byteStream();
                //使用随机流来连接要下载的文件
                savedFile = new RandomAccessFile(file, "rw");
                //使用对应的seek方法跳过已下载的字节
                savedFile.seek(downloadedLength); // 跳过已下载的字节
                byte[] b = new byte[1024];
                int total = 0;
                int len;
                while ((len = is.read(b)) != -1) {
                    if (isCanceled) {
                        return TYPE_CANCELED;
                    } else if(isPaused) {
                        return TYPE_PAUSED;
                    } else {
                        //没有取消或者暂停,就记录下载的总数
                        total += len;
                        //把下载到缓存数组的字节写入对应的文件
                        savedFile.write(b, 0, len);
                        // 计算已下载的百分比
                        int progress = (int) ((total + downloadedLength) * 100 / contentLength);
                        //将下载的进度传入UI执行的方法
                        publishProgress(progress);
                    }
                }
                Log.d("下载开始了","下载完成");
                //关闭数据流，返回下载成功
                response.body().close();
                return TYPE_SUCCESS;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (savedFile != null) {
                    savedFile.close();
                }
                if (isCanceled && file != null) {
                    file.delete();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return TYPE_FAILED;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        //跟新下载的进度，这里是和UI界面打交道的地方
        int progress = values[0];
        if (progress > lastProgress) {
            listener.onProgress(progress);
            lastProgress = progress;
        }
    }
    @Override
    protected void onPostExecute(Integer status) {
        switch (status) {
            case TYPE_SUCCESS:
                listener.onSuccess();
                break;
            case TYPE_FAILED:
                listener.onFailed();
                break;
            case TYPE_PAUSED:
                listener.onPaused();
                break;
            case TYPE_CANCELED:
                listener.onCanceled();
            default:
                break;
        }
    }

    public void pauseDownload() {
        isPaused = true;
        Log.d("暂停","暂停按钮true");
    }

    public void cancelDownload() {
        isCanceled = true;
        Log.d("取消","取消按钮true");
    }
     //获取已下载文件的长度
    private long getContentLength(String downloadUrl) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(downloadUrl)
                .build();
        //同步操作,
        Response response = client.newCall(request).execute();
        //获取下载内容的长度
        if (response != null && response.isSuccessful()) {
            long contentLength = response.body().contentLength();
            response.close();
            return contentLength;
        }
        //如果没下载下来，或者不成功，直接返回零长度
        return 0;
    }

}