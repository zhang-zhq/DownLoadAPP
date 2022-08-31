package com.example.servicebestpractice;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    public static final String TAG = "MainActivity";
    public static final String BASE_URL = "http://172.30.91.47:9090/v1/version/getAndroidNewVersion";
    private UpdateInfo updateInfo = new UpdateInfo();
    public String url = "";
    private DownloadService downloadService;
    private DownloadService.DownloadBinder downloadBinder;
    //为了保证下载任务处于后台一直执行，将下载的任务放入service中
    private DownloadTask downloadTask = null;
    //活动和后台服务进行绑定的必要操作
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            //活动与服务解除绑定时调用
        }
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            //活动与服务成功绑定时调用
            downloadBinder =  (DownloadService.DownloadBinder)service;
            downloadService = ((DownloadService.DownloadBinder)service).getService();
            downloadService.setDownloadListener(new DownloadListener() {
                @Override
                public void onProgress(int progress) {
                    downloadService.getNotificationManager().notify(1,
                            downloadService.getNotification("Downloading...", progress));
                }

                @Override
                public void onSuccess() {
                    downloadTask = null;
                    downloadService.getNotificationManager().notify(1,
                            downloadService.getNotification("Download Success", -1));
                    Toast.makeText(MainActivity.this, "Download Success",
                            Toast.LENGTH_SHORT).show();
                    if(url != null){
                        Log.d(TAG,"onSuccess" + url);
                        String fileName = url.substring(url.lastIndexOf("/"));
                        File file = new File(Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_DOWNLOADS).getPath(),fileName);

                        downSuccess(file);
                    }

                }

                @Override
                public void onFailed() {
                    downloadTask = null;
                    // 下载失败时将前台服务通知关闭，并创建一个下载失败的通知
                    //stopForeground(true);
                    downloadService.getNotificationManager().notify(1,
                            downloadService.getNotification("Download Failed", -1));
                    Toast.makeText(MainActivity.this, "Download Failed",
                            Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onPaused() {
                    downloadTask = null;
                    Toast.makeText(MainActivity.this, "Paused",
                            Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onCanceled() {
                    downloadService.getNotificationManager().cancel(1);

                    Toast.makeText(MainActivity.this, "Canceled",
                            Toast.LENGTH_SHORT).show();
                }
            });
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button startDownload = (Button) findViewById(R.id.start_download);
        Button pauseDownload = (Button) findViewById(R.id.pause_download);
        Button cancelDownload = (Button) findViewById(R.id.cancel_download);
        startDownload.setOnClickListener(this);
        pauseDownload.setOnClickListener(this);
        cancelDownload.setOnClickListener(this);

        //活动和后台服务进行绑定
        Intent intent = new Intent(this, DownloadService.class);
        startService(intent);
        bindService(intent, connection, BIND_AUTO_CREATE);
        //认证授权
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{ Manifest.permission. WRITE_EXTERNAL_STORAGE },
                    1);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        updatePackage();
    }

    @Override
    public void onClick(View v) {
        if ( downloadBinder == null) {
            return;
        }
        switch (v.getId()) {
            case R.id.start_download:
                //updatePackage();
               if(url != null){
                   Log.d(TAG,"开始按钮");
                   downloadBinder.startDownload(url);
               }
                break;
            case R.id.pause_download:
                downloadBinder.pauseDownload();
                break;
            case R.id.cancel_download:
                Log.d(TAG,"取消按钮");
                downloadBinder.cancelDownload();
                break;
            default:
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "拒绝权限将无法使用程序", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(connection);
    }
    public void updatePackage() {
        HttpUtil.setOkhttpRequest(BASE_URL, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {

                String responseBody = response.body().string();
                updateInfo = HttpUtil.handlePackageResponse(responseBody);
                url = updateInfo.getData().getUrl();
                String now_version = "";
                try {
                    //获得当前版本
                    PackageManager packageManager = getPackageManager();
                    PackageInfo packageInfo = packageManager.getPackageInfo(getPackageName(),0);
                    now_version = packageInfo.versionName;
                    Log.d(TAG, "onResponse: "+ now_version);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
                if(updateInfo.getData().getVersion().equals(now_version)){
                    showError("发布包版本号与当前版本一致");
                }else{
                    showUpdateDialog(updateInfo);
                }

            }
        });

    }
    /**
     * 版本号错误
     * @param msg
     */
    public void showError(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG,"showError:" + msg);
            }
        });
    }
    /**
     * 根据更新策略显示更新对话框
     * @param updateInfo
     */
    public void showUpdateDialog(UpdateInfo updateInfo) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //提示更新
                if(updateInfo.getData().getUpPolicy().equals("0")){
                    if(((int) updateInfo.getData().getHintNum()) == 0){
                        //一直提示
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setIcon(android.R.drawable.ic_dialog_info);
                        builder.setTitle("检查到新版本：" + updateInfo.getData().getVersion());
                        //版本升级提示信息
                        builder.setMessage(updateInfo.getData().getNoticeMsg());
                        builder.setCancelable(false);
                        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //判断手机SD卡
                                if (Environment.getExternalStorageState().equals(
                                        Environment.MEDIA_MOUNTED)) {
                                    String url = updateInfo.getData().getUrl();
                                    downloadBinder.startDownload(url);
                                } else {
                                    Toast.makeText(MainActivity.this,
                                            "SD卡不可用",Toast.LENGTH_LONG).show();
                                }
                            }
                        });
                        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(MainActivity.this,
                                        "你取消了更新",Toast.LENGTH_LONG).show();
                            }
                        });
                        builder.create().show();
                    }else{
                        int count = (int) updateInfo.getData().getHintNum(); //提示次数
                        while(count > 0){
                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                            builder.setIcon(android.R.drawable.ic_dialog_info);
                            builder.setTitle("检测到新版本" + updateInfo.getData().getVersion());
                            //版本升级提示信息
                            builder.setMessage(updateInfo.getData().getNoticeMsg());
                            builder.setCancelable(false);
                            builder.setPositiveButton("升级", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //判断手机SD卡
                                    if (Environment.getExternalStorageState().equals(
                                            Environment.MEDIA_MOUNTED)) {
                                        downloadBinder.startDownload(updateInfo.getData().getUrl());
                                    } else {
                                        Toast.makeText(MainActivity.this,
                                                "SD卡不可用，请插入SD卡",Toast.LENGTH_LONG).show();
                                    }
                                }
                            });
                            builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Toast.makeText(MainActivity.this,
                                            "你取消了更新",Toast.LENGTH_LONG).show();
                                }
                            });
                            builder.create().show();
                            count--;
                        }
                    }
                }else{
                    //强制更新
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setIcon(android.R.drawable.ic_dialog_info);
                    builder.setTitle("检测到新版本" + updateInfo.getData().getVersion());
                    //版本升级提示信息
                    builder.setMessage(updateInfo.getData().getNoticeMsg());
                    builder.setCancelable(false);
                    builder.setPositiveButton("升级", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //判断手机SD卡
                            if (Environment.getExternalStorageState().equals(
                                    Environment.MEDIA_MOUNTED)) {
                                Log.d("强制更新",updateInfo.getData().getUrl());
                                downloadBinder.startDownload(updateInfo.getData().getUrl());
                            } else {
                                Toast.makeText(MainActivity.this,
                                        "SD卡不可用，请插入SD卡",Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                    builder.create().show();
                }
            }
        });
    }

    /**
     * 下载成功，开始安装
     * @param file
     */
    public void downSuccess(File file){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setIcon(android.R.drawable.ic_dialog_info);
        builder.setTitle("安装包下载完成");
        builder.setMessage("是否安装");
        builder.setCancelable(false);
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                String authority = "com.example.servicebestpractice.fileprovider";
                Log.d("TAG","aaaaaaaaaaaaaa");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { //aandroid N的权限问题
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    Uri contentUri = FileProvider.getUriForFile(MainActivity.this, authority, file);
                    intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
                    Log.d("TAG","bbbbbbbbbbbb");
                } else {
                    intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
                    Log.d("TAG","ccccccccccccccccc");
                }
                startActivity(intent);
                Log.d("TAG","dddddddddddddddd");
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(MainActivity.this,
                        "你取消了安装",Toast.LENGTH_LONG).show();
            }
        });
        builder.create().show();
    }



}
