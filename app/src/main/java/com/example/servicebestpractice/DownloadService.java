package com.example.servicebestpractice;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;

import android.content.Intent;
import android.graphics.BitmapFactory;

import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import java.io.File;

public class DownloadService extends Service {
    //为了保证下载任务处于后台一直执行，将下载的任务放入service中
    private DownloadTask downloadTask;
    private String downloadUrl;
    private DownloadListener listener = null;
    public void setDownloadListener(DownloadListener listener){
        this.listener = listener;
    }

    private DownloadBinder mBinder = new DownloadBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    class DownloadBinder extends Binder {

        public DownloadService getService(){

            return DownloadService.this;
        }

        public void startDownload(String url) {
            //主activity和service进行绑定，

            // 当在主线程中点击下载时，开始在后台服务中开启异步子线程执行downloadTask下载任务
                downloadTask = null;
                downloadUrl = url;
                downloadTask = new DownloadTask(listener);
                downloadTask.execute(downloadUrl);
                //同时将改后台服务变成前台服务，以免被回收
                //startForeground(1, getNotification("Downloading...", 0));
                Toast.makeText(DownloadService.this, "Downloading...",
                        Toast.LENGTH_LONG).show();
                Log.d("DownloadService","startDownload, address:" + url);

        }

        public void pauseDownload() {
            if (downloadTask != null) {
                Log.d("DownloadService","pauseDownload");
                downloadTask.pauseDownload();
            }
        }

        public void cancelDownload() {

            if (downloadTask != null) {
                downloadTask.cancelDownload();
                Log.d("取消","取消按钮1");
            } else {
                if (downloadUrl != null) {
                    // 取消下载时需将文件删除，并将通知关闭
                    String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
                    String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
                    File file = new File(directory + fileName);
                    if (file.exists()) {
                        file.delete();
                    }
                    Log.d("取消","取消按钮2");
                    getNotificationManager().cancel(1);
                    stopForeground(true);
                    Toast.makeText(DownloadService.this, "Canceled", Toast.LENGTH_SHORT).show();
                }
            }
        }

    }

    public NotificationManager getNotificationManager() {
        return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    public Notification getNotification(String title, int progress) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel =
                    new NotificationChannel("normal", "Normal",
                            NotificationManager.IMPORTANCE_DEFAULT);
            getNotificationManager().createNotificationChannel(channel);
        }
        //这里使用通知构建出，点击通知时的意图，当点击通知时，跳转到主页面进行暂停或下载操作
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "normal");
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        builder.setContentIntent(pi);
        builder.setContentTitle(title);
        if (progress >= 0) {
            // 当progress大于或等于0时才需显示下载进度
            builder.setContentText(progress + "%");
            builder.setProgress(100, progress, false);
        }
        return builder.build();
    }






}
