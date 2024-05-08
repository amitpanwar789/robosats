package com.robosats.modules;

import android.util.Log;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.robosats.tor.TorKmpManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class TorModule extends ReactContextBaseJavaModule {
    private TorKmpManager torKmpManager;
    private ReactApplicationContext context;
    public TorModule(ReactApplicationContext reactContext) {
        context = reactContext;
    }

    @Override
    public String getName() {
        return "TorModule";
    }

    @ReactMethod
    public void sendRequest(String action, String url, String headers, String body, final Promise promise) throws JSONException {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS) // Set connection timeout
                .readTimeout(30, TimeUnit.SECONDS) // Set read timeout
                .proxy(torKmpManager.getProxy()).build();

        Request.Builder requestBuilder = new Request.Builder().url(url);

        JSONObject headersObject = new JSONObject(headers);
        headersObject.keys().forEachRemaining(key -> {
            String value = headersObject.optString(key);
            requestBuilder.addHeader(key, value);
        });

        if (Objects.equals(action, "DELETE")) {
            requestBuilder.delete();
        } else if (Objects.equals(action, "POST")) {
            RequestBody requestBody = RequestBody.create(body, MediaType.get("application/json; charset=utf-8"));
            requestBuilder.post(requestBody);
        } else {
            requestBuilder.get();
        }

        Request request = requestBuilder.build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.d("RobosatsError", e.toString());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "{}";
                JSONObject headersJson = new JSONObject();
                response.headers().names().forEach(name -> {
                    try {
                        headersJson.put(name, response.header(name));
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                });
                promise.resolve("{\"json\":" + body + ", \"headers\": " + headersJson +"}");
            }
        });
    }

    @ReactMethod
    public void getTorStatus() {
        String torState = torKmpManager.getTorState().getState().name();
        WritableMap payload = Arguments.createMap();
        payload.putString("torStatus", torState);
        context
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit("TorStatus", payload);
    }

    @ReactMethod
    public void isConnected() {
        String isConnected = String.valueOf(torKmpManager.isConnected());
        WritableMap payload = Arguments.createMap();
        payload.putString("isConnected", isConnected);
        context
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit("TorIsConnected", payload);
    }

    @ReactMethod
    public void isStarting() {
        String isStarting = String.valueOf(torKmpManager.isStarting());
        WritableMap payload = Arguments.createMap();
        payload.putString("isStarting", isStarting);
        context
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit("TorIsStarting", payload);
    }

    @ReactMethod
    public void stop() {
        torKmpManager.getTorOperationManager().stopQuietly();
        WritableMap payload = Arguments.createMap();
        context
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit("TorStop", payload);
    }

    @ReactMethod
    public void start() {
        torKmpManager = new TorKmpManager(context.getCurrentActivity().getApplication());
        torKmpManager.getTorOperationManager().startQuietly();
        WritableMap payload = Arguments.createMap();
        context
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit("TorStart", payload);
    }

    @ReactMethod
    public void restart() {
        torKmpManager = new TorKmpManager(context.getCurrentActivity().getApplication());
        torKmpManager.getTorOperationManager().restartQuietly();
        WritableMap payload = Arguments.createMap();
        context
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit("TorRestart", payload);
    }

    @ReactMethod
    public void newIdentity() {
        torKmpManager.newIdentity(context.getCurrentActivity().getApplication());
        WritableMap payload = Arguments.createMap();
        context
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit("TorNewIdentity", payload);
    }
}
