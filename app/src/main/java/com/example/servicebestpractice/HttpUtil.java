package com.example.servicebestpractice;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.nfc.Tag;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class HttpUtil {
    //用于发送http请求，并传入对应的地址，根据回调来处理服务器的响应，
    /**
     * enqueue(callback);回调，因为服务器是耗时操作，尽量不要放在主线程中，只能开启子线程，但是子线程不能return
     * 拿不到返回的数据，因此只能定义方法，在子线程内部回调，拿到数据
     *  okhttp3.Callback callback是自带的回调接口
     *  enqueue(callback)在内部实现了子线程开启
     * */
    public static void setOkhttpRequest(String address, okhttp3.Callback callback){
        String productId = "047c8cc085e3efcd386e365f36af80e3";
        String version = "1.0.0.1";
        String serialNo = "1bc32244-12f5-4356-b39a-cb5c4b7d8";
        String userId = "123";
        JSONObject json = new JSONObject();
        try{
            json.put("productId", productId);
            json.put("version", version);
            json.put("serialNo",serialNo);
            json.put("userId", userId);
        }catch (Exception e){
            e.printStackTrace();
        }
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"), String.valueOf(json));

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(address)
                .post(body)
                .build();
        client.newCall(request).enqueue(callback);
        Log.d("HttpUtil","网络执行了");
    }
    //解析服务器返回的Json数据
    public static UpdateInfo handlePackageResponse(String response){
        if(!TextUtils.isEmpty(response)){
            Gson gson = new Gson();
            UpdateInfo updateInfo = gson.fromJson(response, UpdateInfo.class);
            Log.d("HttpUtil",updateInfo.getCode() + updateInfo.getMsg());
            return updateInfo;
        }
        return null;
    }
}
